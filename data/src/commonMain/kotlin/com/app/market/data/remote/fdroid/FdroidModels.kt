package com.app.market.data.remote.fdroid

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * F-Droid 官方索引（entry.json / index-v2.json）的最小 DTO 集。
 *
 * 仅映射搜索、详情、更新与下载所需字段；未使用的字段（antiFeatures、donate 等）
 * 依赖 Json { ignoreUnknownKeys = true } 直接忽略。
 * 字段定义见 https://f-droid.org/docs/Index_V2/ 。
 *
 * 兼容两代 schema：
 * - 旧版：媒体字段是仓库相对路径字符串，版本信息在 version 顶层；
 * - 新版（当前官方仓库）：媒体字段是 {name,sha256,size} 文件对象且路径以 `/` 开头，
 *   版本信息内聚到 version.manifest，截图重组为 screenshots.<orientation>.<locale>。
 */

/** entry.jar 解出的 entry.json：指向当前生效的索引文件。 */
@Serializable
internal data class FdroidEntryJson(
    val timestamp: Long = 0L,
    val version: Long = 0L,
    val index: FdroidEntryFile? = null,
)

@Serializable
internal data class FdroidEntryFile(
    val name: String = "",
    val sha256: String = "",
    val size: Long = 0L,
    val timestamp: Long = 0L,
)

/** index-v2 解出的索引 JSON。 */
@Serializable
internal data class FdroidIndexV2Json(
    val repo: FdroidRepoJson = FdroidRepoJson(),
    val packages: Map<String, FdroidPackageJson> = emptyMap(),
)

@Serializable
internal data class FdroidRepoJson(
    val name: Map<String, String>? = null,
    val address: String = "",
    val timestamp: Long = 0L,
    val version: Long = 0L,
    val maxage: Long = 0L,
)

@Serializable
internal data class FdroidPackageJson(
    val metadata: FdroidMetadataJson = FdroidMetadataJson(),
    val versions: Map<String, FdroidVersionJson> = emptyMap(),
)

@Serializable
internal data class FdroidMetadataJson(
    val name: Map<String, String>? = null,
    val summary: Map<String, String>? = null,
    val description: Map<String, String>? = null,
    /** 新版：locale -> 文件对象；旧版：locale -> 路径字符串（由 [FdroidFileRefSerializer] 兼容）。 */
    val icon: Map<String, FdroidFileRef>? = null,
    val authorName: String? = null,
    val authorEmail: String? = null,
    val webSite: String? = null,
    val sourceCode: String? = null,
    val issueTracker: String? = null,
    val added: Long = 0L,
    val lastUpdated: Long = 0L,
    val whatsNew: Map<String, String>? = null,
    val categories: List<String> = emptyList(),
    /** 旧版截图：locale -> 仓库相对路径列表。 */
    val phoneScreenshots: Map<String, List<String>>? = null,
    /** 新版截图：orientation(phone/tablet) -> locale -> 文件对象列表。 */
    val screenshots: Map<String, Map<String, List<FdroidFileRef>>>? = null,
)

@Serializable
internal data class FdroidVersionJson(
    val file: FdroidFileJson = FdroidFileJson(),
    // 旧版字段（新版移入 manifest，二者取一）
    val versionName: String = "",
    val versionCode: Long = 0L,
    val sdkVersion: Int = 0,
    val targetSdkVersion: Int = 0,
    val added: Long = 0L,
    val whatsNew: Map<String, String>? = null,
    val signer: FdroidSignerJson? = null,
    // 新版字段
    val manifest: FdroidManifestJson? = null,
)

@Serializable
internal data class FdroidManifestJson(
    val versionName: String = "",
    val versionCode: Long = 0L,
    val usesSdk: FdroidUsesSdkJson? = null,
    val signer: FdroidSignerJson? = null,
)

@Serializable
internal data class FdroidUsesSdkJson(
    val minSdkVersion: Int = 0,
    val targetSdkVersion: Int = 0,
)

@Serializable
internal data class FdroidFileJson(
    val name: String = "",
    val sha256: String = "",
    val size: Long = 0L,
)

@Serializable
internal data class FdroidSignerJson(
    /** APK 签名证书 SHA-256（小写 HEX，F-Droid 保证与仓库签名证书一致）。 */
    val sha256: List<String> = emptyList(),
)

/**
 * 仓库内文件引用：新 schema 为对象 `{name,sha256,size}`，旧 schema 为纯路径字符串。
 */
@Serializable(with = FdroidFileRefSerializer::class)
internal data class FdroidFileRef(
    val name: String,
    val sha256: String = "",
    val size: Long = 0L,
)

internal object FdroidFileRefSerializer : KSerializer<FdroidFileRef> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.app.market.data.remote.fdroid.FdroidFileRef", PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): FdroidFileRef {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw IllegalArgumentException("FdroidFileRef 只支持 JSON 解码")
        return when (val element = jsonDecoder.decodeJsonElement()) {
            is JsonPrimitive -> FdroidFileRef(name = element.content)
            is JsonObject -> {
                val file = jsonDecoder.json.decodeFromJsonElement(FdroidFileJson.serializer(), element)
                FdroidFileRef(name = file.name, sha256 = file.sha256, size = file.size)
            }
            else -> throw IllegalArgumentException("无法识别的 F-Droid 文件引用：$element")
        }
    }

    override fun serialize(encoder: Encoder, value: FdroidFileRef) {
        // 本应用只读索引，不会回写；保留字符串形式即可满足序列化器契约
        encoder.encodeString(value.name)
    }
}
