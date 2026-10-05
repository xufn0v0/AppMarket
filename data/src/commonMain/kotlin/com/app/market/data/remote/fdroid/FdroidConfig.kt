package com.app.market.data.remote.fdroid

/**
 * F-Droid 官方仓库的固定连接参数。
 *
 * 仅支持官方主仓库，地址与签名指纹均内置锁定（不允许用户改写）：
 * - [OFFICIAL_REPO_URL]：F-Droid 官方客户端内置的仓库地址，末尾不带 `/`；
 * - [OFFICIAL_CERTIFICATE_FINGERPRINT_SHA256]：仓库索引签名证书的 SHA-256 指纹，
 *   即官方文档 https://f-droid.org/docs/Release_Channels_and_Signing_Keys/ 公布的
 *   `43:23:8D:51:...:CC:AB`（F-Droid 客户端 default_repos 内置同一枚指纹）；
 * - 安全模型：始终先验证 entry.jar 的 v1 JAR 签名并锁定签名证书指纹；
 *   旧版 entry 再指向 index-v2.jar（二级 JAR 签名），新版 entry(v30000+) 直接用
 *   签名 entry 内声明的 SHA-256 钉扎明文 /index-v2.json。
 *   见 https://f-droid.org/docs/Index_V2/ 。
 */
internal object FdroidConfig {
    const val OFFICIAL_REPO_URL: String = "https://f-droid.org/repo"

    const val OFFICIAL_CERTIFICATE_FINGERPRINT_SHA256: String =
        "43238D512C1E5EB2D6569F4A3AFBF5523418B82E0A3ED1552770ABB9A9C9CCAB"

    /** 签名入口 JAR，载荷为 entry.json。 */
    const val ENTRY_JAR_NAME: String = "entry.jar"
    const val ENTRY_PAYLOAD_NAME: String = "entry.json"

    /** entry.json 指向的索引 JAR 默认载荷名。 */
    const val INDEX_PAYLOAD_NAME: String = "index-v2.json"

    /** 完整索引约 60MB，为索引请求单独放宽超时（10 分钟），覆盖弱网首次刷新。 */
    const val INDEX_REQUEST_TIMEOUT_MS: Long = 10 * 60 * 1000L

    const val SEARCH_PAGE_SIZE: Int = 20
}
