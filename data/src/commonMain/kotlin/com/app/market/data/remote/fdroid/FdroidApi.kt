package com.app.market.data.remote.fdroid

import com.app.market.domain.exception.MarketException
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.plugins.timeout
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException

/** F-Droid 仓库 JAR 字节拉取端口（便于测试注入假实现）。 */
internal interface FdroidIndexHttp {
    suspend fun fetchEntryJar(): ByteArray
    suspend fun fetchIndexJar(fileName: String): ByteArray
}

/**
 * F-Droid 官方仓库只读 HTTP 客户端：只负责取原始字节，验签与解析在仓库层完成。
 * 所有请求强制 HTTPS（[FdroidConfig.OFFICIAL_REPO_URL] 已锁定）。
 */
internal class FdroidApi(private val client: HttpClient) : FdroidIndexHttp {

    override suspend fun fetchEntryJar(): ByteArray =
        downloadJar("${FdroidConfig.OFFICIAL_REPO_URL}/${FdroidConfig.ENTRY_JAR_NAME}")

    override suspend fun fetchIndexJar(fileName: String): ByteArray =
        // 完整索引约 60MB，在全局 60s 请求超时之上为这一个请求放宽到 10 分钟
        downloadJar(
            url = "${FdroidConfig.OFFICIAL_REPO_URL}/${fdroidSafeRepoRelativePath(fileName)}",
            requestTimeoutMillis = FdroidConfig.INDEX_REQUEST_TIMEOUT_MS,
        )

    private suspend fun downloadJar(url: String, requestTimeoutMillis: Long? = null): ByteArray {
        val response = try {
            client.get(url) {
                if (requestTimeoutMillis != null) {
                    timeout { this.requestTimeoutMillis = requestTimeoutMillis }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw MarketException("无法连接 F-Droid 官方仓库：${e.message}")
        }
        if (!response.status.isSuccess()) {
            throw MarketException("F-Droid 仓库返回异常状态：${response.status}")
        }
        return try {
            response.body<ByteArray>()
        } catch (e: Exception) {
            throw MarketException("F-Droid 索引下载不完整：${e.message}")
        }
    }
}

/**
 * 把签名 entry 声明的索引名规范化为仓库内相对路径。
 *
 * 官方索引名形如 "/index-v2.json"（以单个斜杠开头的仓库根相对路径）。
 * 出于纵深防御，拒绝目录穿越、协议头、查询串、反斜杠等任何仓库外路径形态。
 */
internal fun fdroidSafeRepoRelativePath(fileName: String): String {
    val relative = fileName.trim().trimStart('/')
    val illegal = relative.isBlank() ||
        relative.startsWith('/') ||
        relative.contains("..") ||
        relative.any { it == '\\' || it == '?' || it == '#' || it == ':' }
    if (illegal) {
        throw MarketException("F-Droid entry 声明了非法的索引文件名：$fileName")
    }
    return relative
}
