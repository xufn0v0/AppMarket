package com.app.market.data.remote.fdroid

import com.app.market.domain.exception.MarketException
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.jar.JarFile

/**
 * 基于 JDK [JarFile] 内建 JAR signing v1 校验的 F-Droid 验签实现（Android/桌面共用）。
 *
 * `JarFile(file, true)` 在条目流被完整读取时执行：MANIFEST 逐条目摘要校验 →
 * 签名文件（.SF）摘要校验 → PKCS#7 签名块密码学校验；任一失败在读流时抛
 * SecurityException，且未签名/验签失败的条目 `certificates` 为空。
 *
 * 在 JDK 校验之上再强制「签名证书指纹 == 官方锁定指纹」，防止用其它合法证书
 * （自签证书任何人都能生成）重签一个内容合法的 JAR。
 */
internal class JvmFdroidSignatureVerifier : FdroidSignatureVerifier {

    override fun verify(
        jarBytes: ByteArray,
        payloadEntryName: String,
        pinnedFingerprint: String,
    ): FdroidVerifiedJar {
        val expected = normalizeFingerprint(pinnedFingerprint)
        val tempFile = File.createTempFile("fdroid-index", ".jar")
        try {
            tempFile.writeBytes(jarBytes)
            JarFile(tempFile, true).use { jar ->
                var payload: ByteArray? = null
                var payloadFingerprint = ""
                var signedEntryCount = 0

                val entries = jar.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (entry.isDirectory) continue
                    val name = entry.name
                    val bytes = jar.getInputStream(entry).use { it.readAllBytesCompat() }
                    if (name.startsWith("META-INF/")) continue

                    // 能走到这里且未抛 SecurityException，说明该条目摘要/签名链有效；
                    // certificates 只有在条目流完整读取后才可用。
                    val certs = entry.certificates
                        ?.filterIsInstance<X509Certificate>()
                        .orEmpty()
                    if (certs.isEmpty()) {
                        throw MarketException("F-Droid 索引存在未签名条目：$name")
                    }
                    val matching = certs.firstOrNull {
                        normalizeFingerprint(sha256Hex(it.encoded)) == expected
                    } ?: throw MarketException("F-Droid 索引签名证书指纹与官方锁定指纹不匹配：$name")
                    signedEntryCount++

                    if (name == payloadEntryName) {
                        payload = bytes
                        payloadFingerprint = normalizeFingerprint(sha256Hex(matching.encoded))
                    }
                }

                if (signedEntryCount == 0) {
                    throw MarketException("F-Droid 索引 JAR 不含任何已签名内容")
                }
                val payloadBytes = payload
                    ?: throw MarketException("F-Droid 索引缺少载荷条目：$payloadEntryName")
                return FdroidVerifiedJar(payloadBytes, payloadFingerprint)
            }
        } catch (e: MarketException) {
            throw e
        } catch (e: SecurityException) {
            throw MarketException("F-Droid 索引 JAR 签名校验失败：${e.message}").apply { initCause(e) }
        } catch (e: Exception) {
            throw MarketException("F-Droid 索引 JAR 读取失败：${e.message}").apply { initCause(e) }
        } finally {
            tempFile.delete()
        }
    }

    private fun normalizeFingerprint(raw: String): String =
        raw.uppercase().filter { it in '0'..'9' || it in 'A'..'F' }

    private fun sha256Hex(data: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(data)
        return buildString(digest.size * 2) {
            digest.forEach { byte ->
                val value = byte.toInt() and 0xFF
                append(HEX_DIGITS[value ushr 4])
                append(HEX_DIGITS[value and 0x0F])
            }
        }
    }

    private fun java.io.InputStream.readAllBytesCompat(): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val read = read(buffer)
            if (read == -1) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private companion object {
        val HEX_DIGITS = "0123456789ABCDEF".toCharArray()
    }
}
