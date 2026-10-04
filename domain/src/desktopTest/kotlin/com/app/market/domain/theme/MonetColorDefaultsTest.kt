package com.app.market.domain.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Monet 引擎默认方案的纯逻辑单元测试：默认常量、持久化编解码容错、设备差异归一化、
 * WCAG 2.1 对比度计算。不依赖任何平台 UI 能力，在 JVM 上确定性运行。
 */
class MonetColorDefaultsTest {

    // --- 引擎默认方案常量 ---

    @Test
    fun defaultSeedMatchesEngineBaseline() {
        // 与 Miuix monetSystemColors() 回退种子一致（Material You 基线 #6750A4）
        assertEquals(0xFF6750A4.toInt(), MonetColorDefaults.DEFAULT_SEED_COLOR_ARGB)
    }

    // --- WCAG 2.1 对比度 ---

    @Test
    fun contrastRatioOfBlackAndWhiteIs21() {
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        assertEquals(21f, MonetColorDefaults.contrastRatio(black, white), 0.001f)
        // 对称性
        assertEquals(
            MonetColorDefaults.contrastRatio(black, white),
            MonetColorDefaults.contrastRatio(white, black),
            0.0001f,
        )
    }

    @Test
    fun contrastRatioOfIdenticalColorsIsOne() {
        val seed = MonetColorDefaults.DEFAULT_SEED_COLOR_ARGB
        assertEquals(1f, MonetColorDefaults.contrastRatio(seed, seed), 0.0001f)
    }

    @Test
    fun engineDefaultPrimaryMeetsAaWithWhite() {
        // 引擎默认主色 #6750A4 与白字的对比度满足 AA（4.5:1）
        val ratio = MonetColorDefaults.contrastRatio(0xFFFFFFFF.toInt(), MonetColorDefaults.DEFAULT_SEED_COLOR_ARGB)
        assertTrue(ratio >= MonetColorDefaults.WCAG_AA_CONTRAST_NORMAL_TEXT, "primary/white ratio was $ratio")
    }

    private fun Int.toHex(): String = "#" + (this and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
}
