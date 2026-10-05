package com.app.market.domain.repository

import com.app.market.domain.model.download.DownloadMeta
import com.app.market.domain.model.market.AppDetail
import com.app.market.domain.model.market.FdroidRepoStatus
import com.app.market.domain.model.market.MarketAppInfo
import com.app.market.domain.model.market.SearchPage
import com.app.market.domain.model.update.ManualUpdateRequest
import com.app.market.domain.model.update.ManualUpdateResult

/**
 * F-Droid 官方仓库（https://f-droid.org/repo）数据源。
 *
 * 与其它商店的在线分页 API 不同，F-Droid 采用「签名全量索引 + 本地检索」模型：
 * 客户端先拉取签名 entry.jar，再按其中声明的 sha256 拉取签名 index-v2.jar，
 * 验签通过后在本地完成搜索（名称/包名/简介匹配）与更新判断（versionCode 对比）。
 */
interface FdroidRepository {
    /** 本地全量索引分页搜索；F-Droid 无服务端搜索接口。 */
    suspend fun search(keyword: String, page: Int = 0): SearchPage

    /** 按 Android 包名读取应用详情。 */
    suspend fun appDetail(packageName: String): AppDetail

    suspend fun downloadMeta(app: MarketAppInfo): DownloadMeta
    suspend fun downloadUpdateMeta(app: MarketAppInfo): DownloadMeta

    /** 与本机已安装应用按包名匹配，返回 versionCode 更高的仓库版本。 */
    suspend fun checkUpdates(): List<MarketAppInfo>

    suspend fun checkManualUpdate(request: ManualUpdateRequest): ManualUpdateResult

    /**
     * 验证源连接有效性：拉取并校验 entry/index 签名链，返回仓库元信息。
     * 任何网络、签名或解析失败都会抛出异常，由调用方提示。
     */
    suspend fun verifyConnection(): FdroidRepoStatus
}
