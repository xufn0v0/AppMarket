package com.app.market.data.remote.fdroid

import com.app.market.domain.exception.MarketException

/** JAR 验签通过后解出的载荷及签名证书指纹。 */
internal data class FdroidVerifiedJar(
    val payload: ByteArray,
    val signerFingerprint: String,
)

/**
 * F-Droid 签名 JAR（JAR signing v1）校验器。
 *
 * 实现必须完成官方客户端等价的三项校验，任一不满足即抛 [MarketException]：
 * 1. JAR 内每个非 META-INF 条目都带有有效签名（MANIFEST 摘要、.SF、PKCS#7 签名链完整）；
 * 2. 签名证书的 SHA-256 指纹等于内置锁定的官方指纹 [pinnedFingerprint]；
 * 3. 指定名称的载荷条目存在且内容完整读出。
 */
internal interface FdroidSignatureVerifier {
    fun verify(jarBytes: ByteArray, payloadEntryName: String, pinnedFingerprint: String): FdroidVerifiedJar
}
