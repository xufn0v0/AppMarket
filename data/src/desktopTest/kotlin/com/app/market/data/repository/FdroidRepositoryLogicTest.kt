package com.app.market.data.repository

import com.app.market.data.platform.sha256
import com.app.market.data.remote.fdroid.FdroidConfig
import com.app.market.data.remote.fdroid.FdroidIndexHttp
import com.app.market.data.remote.fdroid.FdroidSignatureVerifier
import com.app.market.data.remote.fdroid.FdroidVerifiedJar
import com.app.market.domain.exception.MarketException
import com.app.market.domain.model.installed.InstalledPackage
import com.app.market.domain.model.market.AppSource
import com.app.market.domain.model.update.ManualUpdateRequest
import com.app.market.domain.model.update.ManualUpdateStatus
import com.app.market.domain.repository.FdroidRepository
import com.app.market.domain.repository.InstalledPackagesRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class FdroidRepositoryLogicTest {

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Test
    fun searchesLocalIndexWithPagingAndRanking() = runBlocking {
        val repo = createRepo(installed = emptyList())

        val page0 = repo.search("test", 0)
        assertEquals(20, page0.items.size)
        assertTrue(page0.hasMore)
        assertTrue(page0.items.all { it.source == AppSource.FDROID })

        val page1 = repo.search("test", 1)
        assertEquals(5, page1.items.size)
        assertTrue(!page1.hasMore)

        // 超出范围返回空页而非异常
        val page9 = repo.search("test", 9)
        assertEquals(0, page9.items.size)

        // 包名命中
        val fdroid = repo.search("fdroid", 0).items
        assertEquals(1, fdroid.size)
        assertEquals("org.fdroid.fdroid", fdroid.single().packageName)
        assertEquals(200L, fdroid.single().versionCode)

        // 空白关键字不触发全量索引扫描
        assertEquals(0, repo.search("  ", 0).items.size)
    }

    @Test
    fun checkUpdatesOnlyReturnsHigherVersionCodesAndKeepsInstalledInfo() = runBlocking {
        val repo = createRepo(
            installed = listOf(
                installed("com.test.app1", versionCode = 5, versionName = "0.5"),
                installed("com.test.app2", versionCode = 100),
                installed("com.not.in.catalog", versionCode = 1),
            ),
        )

        val updates = repo.checkUpdates()
        assertEquals(1, updates.size)
        val update = updates.single()
        assertEquals("com.test.app1", update.packageName)
        assertEquals(100L, update.versionCode)
        assertEquals(5L, update.installedVersionCode)
        assertEquals("0.5", update.installedVersionName)
        assertEquals(0L, update.deltaSize)
    }

    @Test
    fun downloadMetaPointsToOfficialRepoUrlWithIndexHash() = runBlocking {
        val repo = createRepo(installed = emptyList())
        val app = repo.search("fdroid", 0).items.single()

        val meta = repo.downloadMeta(app)
        assertEquals("https://f-droid.org/repo/org.fdroid.fdroid_200.apk", meta.url)
        assertEquals(2048L, meta.size)
        assertEquals(200L, meta.versionCode)
        assertEquals(AppSource.FDROID, meta.source)
        assertEquals(1, meta.parts.size)
        assertEquals("bbbb", meta.parts.single().hash)
        assertEquals("base", meta.parts.single().type)
    }

    @Test
    fun manualUpdateCoversAllThreeStatuses() = runBlocking {
        val repo = createRepo(installed = emptyList())

        val available = repo.checkManualUpdate(ManualUpdateRequest("com.test.app1", versionCode = 10))
        assertEquals(ManualUpdateStatus.UPDATE_AVAILABLE, available.status)
        assertEquals(100L, available.app!!.versionCode)

        val current = repo.checkManualUpdate(ManualUpdateRequest("com.test.app1", versionCode = 100))
        assertEquals(ManualUpdateStatus.RECOGNIZED_NO_UPDATE, current.status)

        val missing = repo.checkManualUpdate(ManualUpdateRequest("com.unknown", versionCode = 1))
        assertEquals(ManualUpdateStatus.NOT_FOUND, missing.status)
    }

    @Test
    fun verifyConnectionReturnsRepoMetadataAndPinnedFingerprint() = runBlocking {
        val repo = createRepo(installed = emptyList())

        val status = repo.verifyConnection()
        assertEquals("F-Droid", status.repoName)
        assertEquals(26, status.appCount)
        assertEquals(INDEX_TIMESTAMP, status.indexTimestamp)
        assertEquals(FdroidConfig.OFFICIAL_CERTIFICATE_FINGERPRINT_SHA256, status.certificateFingerprint)
    }

    @Test
    fun rejectsIndexJarWhenEntryDeclaredSha256Mismatches() = runBlocking<Unit> {
        val badEntry = entryJson(declaredSha256 = "00".repeat(32))
        val repo = createRepo(installed = emptyList(), entryJsonOverride = badEntry)

        assertFailsWith<MarketException> { repo.search("anything", 0) }
    }

    @Test
    fun acceptsV30000PlainJsonIndexPinnedBySignedEntry() = runBlocking {
        // 新版官方 entry 直接钉扎明文 /index-v2.json：不再做 JAR 验签，
        // 信任链 = entry.jar 验签 + entry 内声明的 SHA-256 哈希钉扎。
        val verifier = RecordingVerifier(failOnIndexVerification = true)
        val repo = createRepo(
            installed = emptyList(),
            entryJsonOverride = entryJson(indexName = "/index-v2.json", declaredSha256 = sha256(INDEX_JSON.encodeToByteArray()).toHex()),
            verifier = verifier,
        )

        val results = repo.search("fdroid", 0).items
        assertEquals("org.fdroid.fdroid", results.single().packageName)
        assertEquals(1, verifier.entryVerifications)
        assertEquals(0, verifier.indexVerifications)
    }

    @Test
    fun rejectsPathTraversalIndexNameFromSignedEntry() = runBlocking<Unit> {
        val malicious = entryJson(indexName = "/../../etc/passwd", declaredSha256 = "00".repeat(32))
        val repo = createRepo(installed = emptyList(), entryJsonOverride = malicious)

        assertFailsWith<MarketException> { repo.search("anything", 0) }
    }

    // ------------------------------------------------------------------
    // 测试夹具
    // ------------------------------------------------------------------

    private class RecordingVerifier(
        private val failOnIndexVerification: Boolean = false,
    ) : FdroidSignatureVerifier {
        var entryVerifications = 0
        var indexVerifications = 0

        override fun verify(
            jarBytes: ByteArray,
            payloadEntryName: String,
            pinnedFingerprint: String,
        ): FdroidVerifiedJar {
            assertEquals(FdroidConfig.OFFICIAL_CERTIFICATE_FINGERPRINT_SHA256, pinnedFingerprint)
            when (payloadEntryName) {
                FdroidConfig.ENTRY_PAYLOAD_NAME -> entryVerifications++
                FdroidConfig.INDEX_PAYLOAD_NAME -> {
                    if (failOnIndexVerification) {
                        error("v30000 明文索引不应触发 JAR 验签")
                    }
                    indexVerifications++
                }
            }
            return FdroidVerifiedJar(jarBytes, pinnedFingerprint)
        }
    }

    private fun createRepo(
        installed: List<InstalledPackage>,
        entryJsonOverride: String? = null,
        verifier: FdroidSignatureVerifier = RecordingVerifier(),
    ): FdroidRepository {
        val indexBytes = INDEX_JSON.encodeToByteArray()
        val indexSha = sha256(indexBytes).toHex()
        val entryBytes = (entryJsonOverride ?: entryJson(declaredSha256 = indexSha)).encodeToByteArray()

        val http = object : FdroidIndexHttp {
            override suspend fun fetchEntryJar() = entryBytes
            override suspend fun fetchIndexJar(fileName: String): ByteArray {
                // 文件名由签名 entry 决定，仅记录；内容统一返回测试索引
                return indexBytes
            }
        }
        return FdroidRepositoryImpl(
            api = http,
            verifier = verifier,
            json = json,
            installedPackages = InstalledPackagesRepository { installed },
        )
    }

    private fun installed(packageName: String, versionCode: Long, versionName: String = "") =
        InstalledPackage(
            packageName = packageName,
            versionCode = versionCode,
            versionName = versionName,
            isSystemApp = false,
        )

    private fun ByteArray.toHex(): String =
        joinToString("") { (it.toInt() and 0xFF).toString(16).padStart(2, '0') }

    private fun entryJson(
        indexName: String = "index-v2.jar",
        declaredSha256: String,
    ): String =
        """{"timestamp":$INDEX_TIMESTAMP,"version":20001,""" +
            """"index":{"name":"$indexName","sha256":"$declaredSha256","size":42,""" +
            """"timestamp":$INDEX_TIMESTAMP}}"""

    private companion object {
        const val INDEX_TIMESTAMP = 1_700_000_000_000L

        val INDEX_JSON: String = run {
            val testApps = (0 until 25).joinToString(separator = ",") { index ->
                """
                "com.test.app$index": {
                  "metadata": {"name": {"en": "Catalog Test App $index"}, "summary": {"en": "test fixture"}},
                  "versions": {
                    "v1": {
                      "file": {"name": "com.test.app${index}_100.apk", "sha256": "hash$index", "size": 1024},
                      "versionName": "1.0",
                      "versionCode": 100
                    }
                  }
                }
                """.trimIndent()
            }
            """
{
  "repo": {"name": {"en": "F-Droid"}, "address": "https://f-droid.org/repo", "timestamp": $INDEX_TIMESTAMP},
  "packages": {
    $testApps,
    "org.fdroid.fdroid": {
      "metadata": {"name": {"en-US": "F-Droid"}, "summary": {"en": "The official app store"}},
      "versions": {
        "v-old": {"file": {"name": "org.fdroid.fdroid_100.apk", "sha256": "aaaa", "size": 1024}, "versionName": "1.0", "versionCode": 100},
        "v-new": {"file": {"name": "org.fdroid.fdroid_200.apk", "sha256": "bbbb", "size": 2048}, "versionName": "2.0", "versionCode": 200}
      }
    }
  }
}
""".trimIndent()
        }
    }
}
