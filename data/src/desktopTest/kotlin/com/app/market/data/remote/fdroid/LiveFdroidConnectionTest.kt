package com.app.market.data.remote.fdroid

import com.app.market.data.platform.createHttpClient
import com.app.market.data.repository.FdroidRepositoryImpl
import com.app.market.domain.model.market.AppSource
import com.app.market.domain.repository.FdroidRepository
import com.app.market.domain.repository.InstalledPackagesRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * F-Droid 官方仓库真实链路联调（默认跳过）：
 * `APPMARKET_LIVE_FDROID=1 ./gradlew :data:desktopTest --tests "*LiveFdroidConnectionTest"`
 *
 * 覆盖 HTTPS 拉取 → entry.jar 验签（官方指纹锁定）→ index-v2.jar SHA-256 + 验签 →
 * 本地搜索 → 下载元数据 URL/哈希生成的完整路径。
 */
class LiveFdroidConnectionTest {

    @Test
    fun officialRepoChainVerifiesAndServesApps() = runBlocking {
        if (System.getenv(ENABLE_ENV) != "1") return@runBlocking

        val repository: FdroidRepository = FdroidRepositoryImpl(
            api = FdroidApi(createHttpClient()),
            verifier = JvmFdroidSignatureVerifier(),
            json = Json { ignoreUnknownKeys = true; isLenient = true },
            installedPackages = InstalledPackagesRepository { emptyList() },
        )

        val status = repository.verifyConnection()
        assertEquals("F-Droid", status.repoName)
        assertTrue(status.appCount > 1000, "official repo should list thousands of apps: $status")
        assertEquals(
            FdroidConfig.OFFICIAL_CERTIFICATE_FINGERPRINT_SHA256,
            status.certificateFingerprint,
        )

        val results = repository.search("F-Droid", 0).items
        val officialClient = results.firstOrNull { it.packageName == "org.fdroid.fdroid" }
        assertTrue(officialClient != null, "official F-Droid client missing from results")
        assertTrue(officialClient!!.versionCode > 0L)
        assertEquals(AppSource.FDROID, officialClient.source)

        val detail = repository.appDetail("org.fdroid.fdroid")
        assertEquals("org.fdroid.fdroid", detail.app.packageName)
        assertTrue(detail.introduction.isNotBlank())

        val meta = repository.downloadMeta(officialClient)
        assertTrue(meta.url.startsWith("https://f-droid.org/repo/org.fdroid.fdroid_"))
        assertTrue(meta.parts.single().hash.matches(Regex("[0-9a-f]{64}")))
    }

    private companion object {
        const val ENABLE_ENV = "APPMARKET_LIVE_FDROID"
    }
}
