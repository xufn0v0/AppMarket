package com.app.market.data.remote.fdroid

import com.app.market.domain.exception.MarketException
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.KeyStore
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * 端到端验签测试：用 JDK 自带 keytool/jarsigner 生成真实的 JAR signing v1 签名夹具，
 * 由 JDK 校验链承认其合法后，再验证我们叠加的「签名证书指纹锁定」策略。
 *
 * 之所以不手工构造 PKCS#7，是因为真实世界的 F-Droid 索引同样由标准签名工具产出，
 * 使用 jarsigner 生成夹具与生产链路完全一致，且能覆盖 JDK 对签名块的全部严格解析。
 */
class JvmFdroidSignatureVerifierTest {

    private val verifier = JvmFdroidSignatureVerifier()

    @Test
    fun acceptsJarSignedByPinnedCertificate() {
        val payload = """{"timestamp":1700000000000,"index":{"name":"index-v2.jar"}}""".toByteArray()
        val jar = SigningTool.signWithJarsigner(listOf("entry.json" to payload))

        val result = verifier.verify(jar, "entry.json", SigningTool.certificateFingerprint)

        assertContentEquals(payload, result.payload)
        assertEqualsNormalized(SigningTool.certificateFingerprint, result.signerFingerprint)
    }

    @Test
    fun verifiesEverySignedEntryInMultiEntryJar() {
        val payloads = listOf(
            "index-v2.json" to "{}".toByteArray(),
            "icons/icon.png" to ByteArray(64) { it.toByte() },
        )
        val jar = SigningTool.signWithJarsigner(payloads)

        val result = verifier.verify(jar, "index-v2.json", SigningTool.certificateFingerprint)
        assertContentEquals("{}".toByteArray(), result.payload)
    }

    @Test
    fun rejectsWhenSignerFingerprintDoesNotMatchPinnedValue() {
        val jar = SigningTool.signWithJarsigner(listOf("entry.json" to "{}".toByteArray()))

        val error = assertFailsWith<MarketException> {
            verifier.verify(jar, "entry.json", "00".repeat(32))
        }
        assertTrue(error.message!!.contains("指纹"), error.message)
    }

    @Test
    fun rejectsTamperedPayloadEvenThoughSignatureBlockPresent() {
        val signedJar = SigningTool.signWithJarsigner(listOf("entry.json" to "original".toByteArray()))

        // 复用合法签名块，但替换载荷内容（模拟 CDN/中间人篡改）
        val tamperedJar = replaceZipEntry(signedJar, "entry.json", "tampered-payload".toByteArray())

        assertFailsWith<MarketException> {
            verifier.verify(tamperedJar, "entry.json", SigningTool.certificateFingerprint)
        }
    }

    @Test
    fun rejectsCompletelyUnsignedJar() {
        val unsignedJar = SigningTool.buildZip(
            listOf(
                "META-INF/MANIFEST.MF" to "Manifest-Version: 1.0\r\n\r\n".toByteArray(),
                "entry.json" to "{}".toByteArray(),
            ),
        )

        val error = assertFailsWith<MarketException> {
            verifier.verify(unsignedJar, "entry.json", "00".repeat(32))
        }
        assertTrue(error.message!!.contains("未签名"), error.message)
    }

    @Test
    fun rejectsWhenPayloadEntryMissing() {
        val jar = SigningTool.signWithJarsigner(listOf("other.json" to "{}".toByteArray()))

        assertFailsWith<MarketException> {
            verifier.verify(jar, "entry.json", SigningTool.certificateFingerprint)
        }
    }

    // ------------------------------------------------------------------
    // 夹具工具
    // ------------------------------------------------------------------

    /** 复制 ZIP 中除 [entryName] 外的全部条目（逐字节保留），并以 [newContent] 替换该条目。 */
    private fun replaceZipEntry(zipBytes: ByteArray, entryName: String, newContent: ByteArray): ByteArray {
        val copied = mutableListOf<Pair<String, ByteArray>>()
        ZipInputStream(zipBytes.inputStream()).use { input ->
            var entry = input.nextEntry
            while (entry != null) {
                val name = entry.name
                val bytes = if (name == entryName) newContent else input.readAllBytes()
                copied.add(name to bytes)
                entry = input.nextEntry
            }
        }
        return SigningTool.buildZip(copied)
    }

    private fun assertEqualsNormalized(expected: String, actual: String) {
        kotlin.test.assertEquals(
            expected.uppercase().filter { it.isLetterOrDigit() },
            actual.uppercase().filter { it.isLetterOrDigit() },
        )
    }

    /**
     * 进程内共享一套由 keytool 生成、jarsigner 签名的测试身份（keystore 仅落临时目录）。
     */
    private object SigningTool {
        private const val ALIAS = "ciarang"
        private const val PASSWORD = "changeit"

        val certificateFingerprint: String
        private val keyStoreFile: File

        init {
            keyStoreFile = File.createTempFile("fdroid-test", ".p12").apply { deleteOnExit() }
            keyStoreFile.delete()
            runTool(
                "keytool",
                "-genkeypair",
                "-keystore", keyStoreFile.absolutePath,
                "-storetype", "PKCS12",
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-alias", ALIAS,
                "-keyalg", "RSA",
                "-keysize", "2048",
                "-validity", "3650",
                "-sigalg", "SHA256withRSA",
                "-dname", "CN=F-Droid Test, O=AppMarket Tests",
            )
            certificateFingerprint = KeyStore.getInstance("PKCS12").run {
                keyStoreFile.inputStream().use { load(it, PASSWORD.toCharArray()) }
                val certificate = getCertificate(ALIAS)
                    ?: error("测试 keystore 中缺少别名 $ALIAS 的证书")
                MessageDigest.getInstance("SHA-256").digest(certificate.encoded)
                    .joinToString("") { "%02X".format(it.toInt() and 0xFF) }
            }
        }

        fun signWithJarsigner(entries: List<Pair<String, ByteArray>>): ByteArray {
            val jar = File.createTempFile("fdroid-fixture", ".jar").apply { deleteOnExit() }
            jar.writeBytes(buildZip(entries))
            runTool(
                "jarsigner",
                "-keystore", keyStoreFile.absolutePath,
                "-storepass", PASSWORD,
                "-keypass", PASSWORD,
                "-sigalg", "SHA256withRSA",
                "-digestalg", "SHA-256",
                jar.absolutePath,
                ALIAS,
            )
            return jar.readBytes()
        }

        fun buildZip(entries: List<Pair<String, ByteArray>>): ByteArray =
            ByteArrayOutputStream().use { out ->
                ZipOutputStream(out).use { zip ->
                    entries.forEach { (name, bytes) ->
                        zip.putNextEntry(ZipEntry(name))
                        zip.write(bytes)
                        zip.closeEntry()
                    }
                }
                out.toByteArray()
            }

        private fun runTool(vararg command: String) {
            val executable = toolPath(command.first())
            val process = ProcessBuilder(listOf(executable) + command.drop(1))
                .redirectErrorStream(true)
                .start()
            val output = process.inputStream.readAllBytes().decodeToString()
            val exitCode = process.waitFor()
            check(exitCode == 0) {
                "${command.first()} 执行失败(exit=$exitCode):\n$output"
            }
        }

        private fun toolPath(name: String): String {
            val suffix = if (System.getProperty("os.name").lowercase().contains("win")) ".exe" else ""
            val candidate = Path(System.getProperty("java.home"), "bin", "$name$suffix").toFile()
            check(candidate.canExecute()) { "找不到 JDK 工具：$candidate" }
            return candidate.absolutePath
        }
    }
}
