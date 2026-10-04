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
 * 该对象同时承载 WCAG 2.1 对比度与 HCT tone 计算工具，
 * 供 data / app 两层共享，保证配色算法在不同设备、屏幕尺寸与显示条件下的一致性。
 */
object MonetColorDefaults {

    /** 引擎默认种子色（ARGB），TonalSpot/Spec2021 基线 Material You 紫。 */
    const val DEFAULT_SEED_COLOR_ARGB: Int = 0xFF6750A4.toInt()

    /** WCAG 2.1 AA 级正文文本（<18pt 常规 / <14pt 粗体）最低对比度。 */
    const val WCAG_AA_CONTRAST_NORMAL_TEXT: Float = 4.5f

    /** WCAG 2.1 AA 级大号文本最低对比度。 */
    const val WCAG_AA_CONTRAST_LARGE_TEXT: Float = 3f

    /** 判断 ARGB 是否为完全不透明颜色（Monet 种子要求不透明，语义才确定）。 */
    fun isOpaque(argb: Int): Boolean = (argb ushr 24) == 0xFF

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
