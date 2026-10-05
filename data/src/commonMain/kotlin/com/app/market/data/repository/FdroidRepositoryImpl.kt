package com.app.market.data.repository

import com.app.market.data.platform.sha256
import com.app.market.data.remote.fdroid.FdroidCatalog
import com.app.market.data.remote.fdroid.FdroidCatalogApp
import com.app.market.data.remote.fdroid.FdroidConfig
import com.app.market.data.remote.fdroid.FdroidEntryJson
import com.app.market.data.remote.fdroid.FdroidIndexHttp
import com.app.market.data.remote.fdroid.FdroidIndexV2Json
import com.app.market.data.remote.fdroid.FdroidSignatureVerifier
import com.app.market.data.remote.fdroid.fdroidSafeRepoRelativePath
import com.app.market.data.remote.fdroid.fdroidStableAppId
import com.app.market.data.remote.fdroid.toCatalog
import com.app.market.domain.exception.MarketException
import com.app.market.domain.model.download.DownloadMeta
import com.app.market.domain.model.download.DownloadPart
import com.app.market.domain.model.installed.InstalledPackage
import com.app.market.domain.model.market.AppDetail
import com.app.market.domain.model.market.AppScreenshot
import com.app.market.domain.model.market.AppSource
import com.app.market.domain.model.market.FdroidRepoStatus
import com.app.market.domain.model.market.MarketAppInfo
import com.app.market.domain.model.market.ScreenshotOrientation
import com.app.market.domain.model.market.SearchPage
import com.app.market.domain.model.update.ManualUpdateRequest
import com.app.market.domain.model.update.ManualUpdateResult
import com.app.market.domain.model.update.ManualUpdateStatus
import com.app.market.domain.repository.FdroidRepository
import com.app.market.domain.repository.InstalledPackagesRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * F-Droid 官方仓库数据源实现。
 *
 * 安全链路（每次刷新）：
 * 1. HTTPS 拉取 entry.jar，JAR v1 验签 + 签名证书指纹锁定官方指纹；
 * 2. 解析 entry.json，按其中声明的文件名与 sha256 拉取索引：
 *    新版为明文 /index-v2.json（信任来自签名 entry 的哈希钉扎），
 *    旧版为 index-v2.jar（先比对整包 SHA-256，再做 JAR v1 验签 + 同枚指纹锁定）；
 * 3. 解析 index-v2.json 并缓存；entry 时间戳未变化时跳过大索引下载；
 * 4. 下载 APK 时把索引声明的 SHA-256 透传到下载管线（[DownloadPart.hash]）。
 */
internal class FdroidRepositoryImpl(
    private val api: FdroidIndexHttp,
    private val verifier: FdroidSignatureVerifier,
    private val json: Json,
    private val installedPackages: InstalledPackagesRepository,
) : FdroidRepository {

    private val refreshMutex = Mutex()

    @Volatile
    private var cachedCatalog: FdroidCatalog? = null

    override suspend fun search(keyword: String, page: Int): SearchPage {
        val query = keyword.trim()
        if (query.isBlank()) return SearchPage(emptyList(), false)
        val catalog = catalog()
        val matched = catalog.apps
            .mapNotNull { app -> app.matchScoreOrNull(query)?.let { score -> score to app } }
            .sortedWith(compareBy({ it.first }, { it.second.displayName.lowercase() }))
            .map { it.second.toApp() }
        val pageIndex = page.coerceAtLeast(0)
        val from = pageIndex * FdroidConfig.SEARCH_PAGE_SIZE
        if (from > matched.lastIndex) return SearchPage(emptyList(), false)
        val to = minOf(from + FdroidConfig.SEARCH_PAGE_SIZE, matched.size)
        return SearchPage(
            items = matched.subList(from, to),
            hasMore = to < matched.size,
        )
    }

    override suspend fun appDetail(packageName: String): AppDetail {
        val app = catalog().appByPackage(packageName)
        return AppDetail(
            app = app.toApp(),
            brief = app.summary,
            introduction = app.description,
            changeLog = app.latest.changeLog,
            category = app.category,
            ageClassification = "",
            downloadCount = 0L,
            registrationNum = "",
            updateTime = app.lastUpdatedMillis,
            privacyUrl = app.webSite,
            screenshots = app.screenshots.map {
                AppScreenshot(url = it, expandedUrl = it, orientation = ScreenshotOrientation.PORTRAIT)
            },
            comments = emptyList(),
            sameDeveloperApps = emptyList(),
        )
    }

    override suspend fun downloadMeta(app: MarketAppInfo): DownloadMeta {
        val catalogApp = catalog().appByPackage(app.packageName)
        return catalogApp.toDownloadMeta(app)
    }

    override suspend fun downloadUpdateMeta(app: MarketAppInfo): DownloadMeta {
        val catalogApp = catalog().appByPackage(app.packageName)
        if (app.installedVersionCode > 0L && catalogApp.latest.versionCode <= app.installedVersionCode) {
            throw MarketException("F-Droid 未提供比当前版本更新的安装包")
        }
        return catalogApp.toDownloadMeta(app)
    }

    override suspend fun checkUpdates(): List<MarketAppInfo> {
        val catalog = catalog()
        val byPackage = catalog.apps.associateBy { it.packageName.lowercase() }
        return installedPackages.installed()
            .filter { it.packageName.isNotBlank() && it.versionCode > 0L }
            .mapNotNull { installed ->
                val catalogApp = byPackage[installed.packageName.lowercase()] ?: return@mapNotNull null
                if (catalogApp.latest.versionCode <= installed.versionCode) return@mapNotNull null
                catalogApp.toApp().withInstalled(installed)
            }
            .sortedBy { it.displayName.lowercase() }
    }

    override suspend fun checkManualUpdate(request: ManualUpdateRequest): ManualUpdateResult {
        val catalogApp = catalog().apps.firstOrNull {
            it.packageName.equals(request.packageName, ignoreCase = true)
        } ?: return ManualUpdateResult(ManualUpdateStatus.NOT_FOUND)
        val installed = InstalledPackage(
            packageName = request.packageName,
            versionCode = request.versionCode,
            versionName = request.versionName,
            isSystemApp = request.isSystemApp,
            installedBy = request.installedBy,
            splits = request.splits,
            oldApkHash = request.oldApkHash,
            apkSource = request.apkSource,
        )
        val app = catalogApp.toApp().withInstalled(installed)
        return if (catalogApp.latest.versionCode > request.versionCode) {
            ManualUpdateResult(ManualUpdateStatus.UPDATE_AVAILABLE, app)
        } else {
            ManualUpdateResult(ManualUpdateStatus.RECOGNIZED_NO_UPDATE, app)
        }
    }

    override suspend fun verifyConnection(): FdroidRepoStatus {
        val catalog = refreshCatalog()
        return FdroidRepoStatus(
            repoName = catalog.repoName,
            appCount = catalog.apps.size,
            indexTimestamp = catalog.repoTimestamp,
            certificateFingerprint = FdroidConfig.OFFICIAL_CERTIFICATE_FINGERPRINT_SHA256,
        )
    }

    // ------------------------------------------------------------------
    // 签名索引拉取与缓存
    // ------------------------------------------------------------------

    private suspend fun catalog(): FdroidCatalog = refreshMutex.withLock {
        val current = cachedCatalog
        if (current != null) maybeRefreshLocked(current) else loadCatalogLocked()
    }

    /** 缓存命中时仍用极小的 entry.jar 探测更新；探测失败则继续使用缓存（离线可用）。调用方持锁。 */
    private suspend fun maybeRefreshLocked(current: FdroidCatalog): FdroidCatalog {
        val entry = runCatching { fetchVerifiedEntry() }.getOrNull() ?: return current
        val latestTimestamp = entry.index?.timestamp ?: 0L
        if (entry.index == null || latestTimestamp <= current.repoTimestamp) return current
        return runCatching { loadCatalogLocked() }.getOrDefault(current)
    }

    /** 完整拉取 entry/index 签名链并重建目录。调用方持锁。 */
    private suspend fun loadCatalogLocked(): FdroidCatalog {
        val entry = fetchVerifiedEntry()
        val indexFile = entry.index
            ?: throw MarketException("F-Droid entry.json 未声明 index 文件")
        if (indexFile.name.isBlank()) {
            throw MarketException("F-Droid entry.json 声明的 index 文件名为空")
        }

        val cached = cachedCatalog
        if (cached != null && indexFile.timestamp in 1..cached.repoTimestamp) {
            return cached
        }

        // 在调用 HTTP 端口前先在仓库层做路径安全校验（测试替身可能不经过真实 FdroidApi）
        fdroidSafeRepoRelativePath(indexFile.name)
        val indexJar = api.fetchIndexJar(indexFile.name)
        if (indexFile.sha256.isNotBlank() &&
            !sha256Hex(sha256(indexJar)).equals(indexFile.sha256, ignoreCase = true)
        ) {
            throw MarketException("F-Droid 索引的 SHA-256 与 entry 声明不一致，拒绝加载")
        }
        // 官方有两种索引形态：
        //  - 新版 entry(v30000+) 直接钉扎明文 /index-v2.json：信任来自已验签 entry 中的 SHA-256；
        //  - 旧版钉扎 index-v2.jar：除哈希外还需 JAR v1 验签 + 同一枚官方证书指纹锁定。
        val indexPayload = when {
            indexFile.name.endsWith(".json", ignoreCase = true) -> indexJar
            indexFile.name.endsWith(".jar", ignoreCase = true) -> verifier.verify(
                jarBytes = indexJar,
                payloadEntryName = FdroidConfig.INDEX_PAYLOAD_NAME,
                pinnedFingerprint = FdroidConfig.OFFICIAL_CERTIFICATE_FINGERPRINT_SHA256,
            ).payload
            else -> throw MarketException("F-Droid entry 声明了不支持的索引格式：${indexFile.name}")
        }
        val indexJson = try {
            json.decodeFromString<FdroidIndexV2Json>(indexPayload.decodeToString())
        } catch (e: Exception) {
            throw MarketException("F-Droid index-v2.json 解析失败：${e.message}")
        }
        return indexJson.toCatalog(FdroidConfig.OFFICIAL_REPO_URL).also { catalog ->
            if (catalog.apps.isEmpty()) throw MarketException("F-Droid 索引为空，疑似异常响应")
            cachedCatalog = catalog
        }
    }

    private suspend fun fetchVerifiedEntry(): FdroidEntryJson {
        val entryJar = api.fetchEntryJar()
        val verified = verifier.verify(
            jarBytes = entryJar,
            payloadEntryName = FdroidConfig.ENTRY_PAYLOAD_NAME,
            pinnedFingerprint = FdroidConfig.OFFICIAL_CERTIFICATE_FINGERPRINT_SHA256,
        )
        return try {
            json.decodeFromString<FdroidEntryJson>(verified.payload.decodeToString())
        } catch (e: Exception) {
            throw MarketException("F-Droid entry.json 解析失败：${e.message}")
        }
    }

    private suspend fun refreshCatalog(): FdroidCatalog =
        refreshMutex.withLock { loadCatalogLocked() }

    // ------------------------------------------------------------------
    // 映射
    // ------------------------------------------------------------------

    private fun FdroidCatalog.appByPackage(packageName: String): FdroidCatalogApp =
        apps.firstOrNull { it.packageName.equals(packageName, ignoreCase = true) }
            ?: throw MarketException("F-Droid 官方仓库未收录该应用：$packageName")

    /** 0=精确/前缀命中（优先），1=包含命中；不匹配返回 null。 */
    private fun FdroidCatalogApp.matchScoreOrNull(query: String): Int? {
        val name = displayName.lowercase()
        val pkg = packageName.lowercase()
        val summary = summary.lowercase()
        val q = query.lowercase()
        return when {
            name == q || pkg == q -> 0
            name.startsWith(q) || pkg.startsWith(q) -> 0
            name.contains(q) || pkg.contains(q) -> 1
            summary.contains(q) -> 1
            else -> null
        }
    }

    private fun FdroidCatalogApp.toApp(): MarketAppInfo = MarketAppInfo(
        appId = fdroidStableAppId(packageName),
        packageName = packageName,
        displayName = displayName,
        publisherName = author,
        versionName = latest.versionName,
        versionCode = latest.versionCode,
        icon = iconUrl,
        apkSize = latest.size,
        deltaSize = 0L,
        ratingScore = 0.0,
        changeLog = latest.changeLog,
        source = AppSource.FDROID,
        category = category,
    )

    private fun FdroidCatalogApp.toDownloadMeta(app: MarketAppInfo): DownloadMeta {
        val artifact = latest
        if (artifact.fileName.isBlank()) throw MarketException("F-Droid 未返回完整安装包地址")
        val url = "${FdroidConfig.OFFICIAL_REPO_URL}/${artifact.fileName}"
        return DownloadMeta(
            appId = app.appId,
            packageName = packageName,
            displayName = displayName.ifBlank { app.displayName },
            versionName = artifact.versionName.ifBlank { app.versionName },
            versionCode = artifact.versionCode,
            url = url,
            size = artifact.size.takeIf { it > 0L } ?: app.apkSize,
            // F-Droid 无增量协议，显式只保留整包部分并透传索引签名声明的 APK SHA-256
            parts = listOf(
                DownloadPart(name = "", type = "base", url = url, size = artifact.size, hash = artifact.sha256),
            ),
            icon = iconUrl,
            changeLog = artifact.changeLog,
            source = AppSource.FDROID,
        )
    }

    private fun MarketAppInfo.withInstalled(installed: InstalledPackage): MarketAppInfo = copy(
        installedVersionName = installed.versionName,
        installedVersionCode = installed.versionCode,
        deltaSize = 0L,
        installedOldApkHash = installed.oldApkHash,
        installedBaseApkPath = installed.baseApkPath,
        installedSplits = installed.splits,
        isSystemApp = installed.isSystemApp,
    )

    private fun sha256Hex(data: ByteArray): String =
        data.joinToString("") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
}
