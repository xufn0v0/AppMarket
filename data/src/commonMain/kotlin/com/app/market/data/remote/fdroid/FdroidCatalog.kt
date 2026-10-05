package com.app.market.data.remote.fdroid

import kotlin.math.absoluteValue

/**
 * index-v2.json 经验签后映射出的内存目录，供本地搜索/更新直接使用，
 * 不再触碰网络与序列化结构。
 */
internal data class FdroidCatalog(
    val repoName: String,
    val repoTimestamp: Long,
    val apps: List<FdroidCatalogApp>,
)

internal data class FdroidCatalogApp(
    val packageName: String,
    val displayName: String,
    val summary: String,
    val description: String,
    val iconUrl: String,
    val author: String,
    val webSite: String,
    val sourceCodeUrl: String,
    val issueTrackerUrl: String,
    val category: String,
    val addedMillis: Long,
    val lastUpdatedMillis: Long,
    val screenshots: List<String>,
    val latest: FdroidCatalogArtifact,
)

internal data class FdroidCatalogArtifact(
    val fileName: String,
    val sha256: String,
    val size: Long,
    val versionName: String,
    val versionCode: Long,
    val sdkVersion: Int,
    val targetSdkVersion: Int,
    val changeLog: String,
)

/** 把 index-v2 DTO 映射为本地目录；无可用版本的包直接丢弃。 */
internal fun FdroidIndexV2Json.toCatalog(repoUrl: String): FdroidCatalog {
    val apps = packages.entries.mapNotNull { (packageName, pkg) ->
        val latest = pkg.versions.values
            .filter { it.effectiveVersionCode() > 0L && it.file.name.isNotBlank() }
            .maxWithOrNull(
                compareBy<FdroidVersionJson> { it.effectiveVersionCode() }.thenBy { it.added },
            )
            ?: return@mapNotNull null
        val metadata = pkg.metadata
        FdroidCatalogApp(
            packageName = packageName,
            displayName = metadata.name.pickLocalized().ifBlank { packageName },
            summary = metadata.summary.pickLocalized(),
            description = metadata.description.pickLocalized().sanitizeFdroidDescription(),
            iconUrl = metadata.icon.pickLocalizedRef()?.name.toRepoAbsoluteUrl(repoUrl),
            author = metadata.authorName.orEmpty(),
            webSite = metadata.webSite.orEmpty(),
            sourceCodeUrl = metadata.sourceCode.orEmpty(),
            issueTrackerUrl = metadata.issueTracker.orEmpty(),
            category = metadata.categories.firstOrNull().orEmpty(),
            addedMillis = metadata.added,
            lastUpdatedMillis = metadata.lastUpdated,
            screenshots = metadata.pickScreenshotPaths().map { it.toRepoAbsoluteUrl(repoUrl) },
            latest = FdroidCatalogArtifact(
                fileName = latest.file.name.trimStart('/'),
                sha256 = latest.file.sha256,
                size = latest.file.size,
                versionName = latest.effectiveVersionName(),
                versionCode = latest.effectiveVersionCode(),
                sdkVersion = latest.effectiveMinSdk(),
                targetSdkVersion = latest.effectiveTargetSdk(),
                changeLog = (latest.whatsNew ?: metadata.whatsNew).pickLocalized(),
            ),
        )
    }.sortedBy { it.displayName.lowercase() }
    return FdroidCatalog(
        repoName = repo.name.pickLocalized().ifBlank { repoUrl },
        repoTimestamp = repo.timestamp,
        apps = apps,
    )
}

/** 新版版本信息内聚在 manifest；旧版在顶层，manifest 缺字段时回退顶层。 */
private fun FdroidVersionJson.effectiveVersionCode(): Long =
    manifest?.versionCode?.takeIf { it > 0L } ?: versionCode

private fun FdroidVersionJson.effectiveVersionName(): String =
    manifest?.versionName?.takeIf { it.isNotBlank() } ?: versionName

private fun FdroidVersionJson.effectiveMinSdk(): Int =
    manifest?.usesSdk?.minSdkVersion?.takeIf { it > 0 } ?: sdkVersion

private fun FdroidVersionJson.effectiveTargetSdk(): Int =
    manifest?.usesSdk?.targetSdkVersion?.takeIf { it > 0 } ?: targetSdkVersion

/** 优先新版 screenshots(phone)，其次 tablet，最后旧版 phoneScreenshots。 */
private fun FdroidMetadataJson.pickScreenshotPaths(): List<String> {
    screenshots?.get("phone").pickLocalizedRefs()?.let { if (it.isNotEmpty()) return it }
    screenshots?.get("tablet").pickLocalizedRefs()?.let { if (it.isNotEmpty()) return it }
    return phoneScreenshots.pickLocalizedList()
}

/** 英文优先，其次任意 locale 的首个非空值。 */
private fun Map<String, String>?.pickLocalized(): String {
    if (isNullOrEmpty()) return ""
    return this["en-US"]?.takeIf { it.isNotBlank() }
        ?: this["en"]?.takeIf { it.isNotBlank() }
        ?: values.firstOrNull { it.isNotBlank() }
        ?: ""
}

private fun Map<String, FdroidFileRef>?.pickLocalizedRef(): FdroidFileRef? {
    if (isNullOrEmpty()) return null
    return this["en-US"]?.takeIf { it.name.isNotBlank() }
        ?: this["en"]?.takeIf { it.name.isNotBlank() }
        ?: values.firstOrNull { it.name.isNotBlank() }
}

private fun Map<String, List<FdroidFileRef>>?.pickLocalizedRefs(): List<String>? {
    if (isNullOrEmpty()) return null
    val preferred = this["en-US"] ?: this["en"]
    return (preferred ?: values.firstOrNull())
        ?.mapNotNull { it.name.takeIf(String::isNotBlank) }
}

private fun Map<String, List<String>>?.pickLocalizedList(): List<String> {
    if (isNullOrEmpty()) return emptyList()
    val preferred = this["en-US"] ?: this["en"]
    return (preferred ?: values.firstOrNull()).orEmpty().filter { it.isNotBlank() }
}

private fun String?.toRepoAbsoluteUrl(repoUrl: String): String =
    if (isNullOrBlank()) "" else "${repoUrl.trimEnd('/')}/${trimStart('/')}"

/**
 * F-Droid 描述使用极简 Markdown 方言，UI 按纯文本渲染，这里做最小净化：
 * 保留链接文字、去掉强调标记与残留 HTML 标签、还原常见实体。
 */
private fun String.sanitizeFdroidDescription(): String = this
    .replace(MarkdownLink) { it.groupValues[1] }
    .replace("**", "")
    .replace("`", "")
    .replace(HtmlTag, "")
    .replace("&amp;", "&")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&quot;", "\"")
    .trim()

private val MarkdownLink = Regex("""\[([^\]]+)]\([^)]*\)""")
private val HtmlTag = Regex("""<[^>]+>""")

/** 包名生成稳定 Long id（与 Samsung 源同策略，避免跨进程 id 漂移）。 */
internal fun fdroidStableAppId(packageName: String): Long {
    val hash = packageName.fold(1125899906842597L) { acc, char -> acc * 31 + char.code }
    return hash.absoluteValue.coerceAtLeast(1L)
}
