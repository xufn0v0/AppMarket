package com.app.market.domain.model.market

/**
 * F-Droid 仓库连接校验结果。
 *
 * 源连接有效性验证通过后返回：仓库名称、收录应用数、索引时间戳（毫秒）与
 * 实际验签通过的仓库签名证书 SHA-256 指纹，供 UI 展示与审计。
 */
data class FdroidRepoStatus(
    val repoName: String,
    val appCount: Int,
    val indexTimestamp: Long,
    val certificateFingerprint: String,
)
