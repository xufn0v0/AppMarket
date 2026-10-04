package com.app.market.domain.theme

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Monet 引擎默认配色方案的唯一事实来源。
 *
 * 这里的「默认」严格对齐 Miuix（top.yukonga.miuix.kmp 0.9.x）Monet 引擎自身的默认参数：
 * - 默认种子 [DEFAULT_SEED_COLOR_ARGB] = `#6750A4`，与引擎在无壁纸提取能力的平台上
 *   使用的 `monetSystemColors()` 回退种子完全一致（即 AOSP Material You 基线种子）；
 * - 调色板风格 TonalSpot、颜色规范 Spec2021（在 UI 层装配 ThemeController 时显式固定，
 *   不依赖库的构造器默认值，避免后续升级库时配色漂移）。
 *
 * 该对象同时承载种子色的持久化编解码、设备差异容错归一化，以及 WCAG 2.1 对比度计算，
 * 供 data / app 两层共享，保证配色算法在不同设备、屏幕尺寸与显示条件下的一致性。
 */
object MonetColorDefaults {

    /** 引擎默认种子色（ARGB），TonalSpot/Spec2021 基线 Material You 紫。 */
    const val DEFAULT_SEED_COLOR_ARGB: Int = 0xFF6750A4.toInt()

    /** WCAG 2.1 AA 级正文文本（<18pt 常规 / <14pt 粗体）最低对比度。 */
    const val WCAG_AA_CONTRAST_NORMAL_TEXT: Float = 4.5f

    /** WCAG 2.1 AA 级大号文本最低对比度。 */
    const val WCAG_AA_CONTRAST_LARGE_TEXT: Float = 3f

    /** Monet 引擎默认方案中可作为种子身份的语义角色（见 [defaultSeedSwatches]）。 */
    enum class DefaultRole {
        PRIMARY,
        TERTIARY,
        ERROR,
    }

    /** 一颗可选的默认方案种子：[role] 为它在引擎默认方案中对应的语义角色。 */
    data class SeedSwatch(val role: DefaultRole, val argb: Int)

    /**
     * Monet 引擎默认方案（`#6750A4` / TonalSpot / Spec2021）中、经实测重新作为种子后能生成
     * **不同**和谐调色板的角色色：
     * - [DefaultRole.PRIMARY] `#6750A4`：引擎默认主色种子本身，即「引擎默认配色」；
     * - [DefaultRole.TERTIARY] `#7D5260`：默认方案第三色，粉玫瑰强调色相；
     * - [DefaultRole.ERROR] `#B3261E`：默认方案错误色，暖红警示色相。
     *
     * 默认方案中的 secondary / neutral 系角色（如 `#625B71`、`#79747E`）经 TonalSpot
     * 重新取色后会和谐化回主色相，不构成独立的可选身份，故不作为预设——它们仍然是引擎
     * 输出配色的一部分（见 [com.app.market.ui.theme] 对 Colors 全角色的消费与测试）。
     */
    val defaultSeedSwatches: List<SeedSwatch> = listOf(
        SeedSwatch(DefaultRole.PRIMARY, 0xFF6750A4.toInt()),
        SeedSwatch(DefaultRole.TERTIARY, 0xFF7D5260.toInt()),
        SeedSwatch(DefaultRole.ERROR, 0xFFB3261E.toInt()),
    )

    /** 判断 ARGB 是否为完全不透明颜色（Monet 种子要求不透明，语义才确定）。 */
    fun isOpaque(argb: Int): Boolean = (argb ushr 24) == 0xFF

    /**
     * 设备差异容错归一化：
     * - null 保持「跟随壁纸」语义（Android 12+ 取壁纸；其余平台引擎自动回退默认种子）；
     * - 非 null 一律强制为不透明，避免个别设备写入 / 同步出带透明通道的异常种子，
     *   导致 HCT 取色结果不确定。
     */
    fun normalizeSeed(argb: Int?): Int? = argb?.let { it or 0xFF000000.toInt() }

    /** 持久化格式：null 存空字符串，非 null 存十进制有符号 Int（兼容历史数据）。 */
    fun formatStoredSeed(argb: Int?): String = argb?.toString().orEmpty()

    /**
     * 容错解析持久化种子色：
     * - null / 空白 → null（按未设置 / 跟随壁纸处理）；
     * - 兼容有符号 Int 十进制（历史格式）与无符号 0x00000000..0xFFFFFFFF 十进制；
     * - 任何非法字符、越界数值一律安全降级为 null，绝不抛出异常。
     */
    fun parseStoredSeed(raw: String?): Int? {
        if (raw.isNullOrBlank()) return null
        val value = raw.trim().toLongOrNull() ?: return null
        if (value < Int.MIN_VALUE || value > 0xFFFFFFFFL) return null
        return value.toInt()
    }

    /** sRGB 相对亮度（WCAG 2.1 定义），入参为 ARGB Int。 */
    fun relativeLuminance(argb: Int): Double {
        val r = srgbChannel((argb shr 16) and 0xFF)
        val g = srgbChannel((argb shr 8) and 0xFF)
        val b = srgbChannel(argb and 0xFF)
        return 0.2126 * r + 0.7152 * g + 0.0722 * b
    }

    /**
     * HCT 色彩空间的色调位次（tone），即 CIELAB L*（0=纯黑，100=纯白）。
     * MD3 规范用 tone 精确定义动态方案中每个角色的明度位次（如浅色 primary = tone 40）。
     */
    fun hctTone(argb: Int): Double {
        val y = relativeLuminance(argb)
        return if (y <= 216.0 / 24389.0) {
            y * (24389.0 / 27.0)
        } else {
            116.0 * y.pow(1.0 / 3.0) - 16.0
        }
    }

    /** 两个不透明颜色间的 WCAG 对比度，取值 1.0（相同）~ 21.0（黑 / 白）。 */
    fun contrastRatio(argb1: Int, argb2: Int): Float {
        val lighter = max(relativeLuminance(argb1), relativeLuminance(argb2))
        val darker = min(relativeLuminance(argb1), relativeLuminance(argb2))
        return ((lighter + 0.05) / (darker + 0.05)).toFloat()
    }

    private fun srgbChannel(channel8Bit: Int): Double {
        val c = channel8Bit / 255.0
        return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
    }
}
