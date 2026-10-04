package com.app.market.domain.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
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

    @Test
    fun defaultSwatchesCoverAllRolesAndAreOpaqueDistinctColors() {
        val swatches = MonetColorDefaults.defaultSeedSwatches
        assertEquals(MonetColorDefaults.DefaultRole.entries.size, swatches.size)
        assertEquals(MonetColorDefaults.DefaultRole.entries, swatches.map { it.role })

        val argbs = swatches.map { it.argb }
        assertEquals(argbs.size, argbs.toSet().size, "默认方案种子色不得重复")
        argbs.forEach { argb ->
            assertTrue(MonetColorDefaults.isOpaque(argb), "默认方案种子色必须不透明: ${argb.toHex()}")
        }
        // 首项即引擎默认种子本身
        assertEquals(
            MonetColorDefaults.DEFAULT_SEED_COLOR_ARGB,
            swatches.first { it.role == MonetColorDefaults.DefaultRole.PRIMARY }.argb,
        )
    }

    // --- 持久化编解码与异常容错 ---

    @Test
    fun parseBlankOrNullStoredSeedYieldsNull() {
        assertNull(MonetColorDefaults.parseStoredSeed(null))
        assertNull(MonetColorDefaults.parseStoredSeed(""))
        assertNull(MonetColorDefaults.parseStoredSeed("   "))
    }

    @Test
    fun parseLegacySignedDecimalSeed() {
        // 历史格式：ARGB Int 的有符号十进制
        val seed = 0xFF6750A4.toInt()
        assertEquals(seed, MonetColorDefaults.parseStoredSeed(seed.toString()))
        assertEquals(-1, MonetColorDefaults.parseStoredSeed("4294967295"))
        assertEquals(0, MonetColorDefaults.parseStoredSeed("0"))
    }

    @Test
    fun parseUnsignedDecimalSeed() {
        // 兼容无符号 0x00000000..0xFFFFFFFF 十进制
        assertEquals(0xFF6750A4.toInt(), MonetColorDefaults.parseStoredSeed("4284960932"))
    }

    @Test
    fun parseCorruptStoredSeedSafelyDegradesToNull() {
        listOf(
            "abc",
            "#6750A4",
            "0xFF6750A4",
            "4294967296", // 超过 32 位无符号范围
            "-2147483649", // 超过 32 位有符号范围
            "12 34",
            "",
        ).forEach { raw ->
            assertNull(MonetColorDefaults.parseStoredSeed(raw), "损坏值必须安全降级为 null: \"$raw\"")
        }
    }

    @Test
    fun formatAndParseRoundTrips() {
        listOf(null, 0xFF6750A4.toInt(), 0xFF625B71.toInt(), 0xFF000000.toInt(), 0xFFFFFFFF.toInt())
            .forEach { seed ->
                val restored = MonetColorDefaults.parseStoredSeed(MonetColorDefaults.formatStoredSeed(seed))
                assertEquals(seed, restored)
            }
    }

    // --- 设备差异归一化 ---

    @Test
    fun normalizeSeedKeepsWallpaperFollowSemanticsForNull() {
        assertNull(MonetColorDefaults.normalizeSeed(null))
    }

    @Test
    fun normalizeSeedForcesOpaqueAlpha() {
        val translucent = 0x806750A4.toInt()
        assertEquals(0xFF6750A4.toInt(), MonetColorDefaults.normalizeSeed(translucent))
        // 已不透明的颜色原样返回
        val opaque = 0xFF625B71.toInt()
        assertEquals(opaque, MonetColorDefaults.normalizeSeed(opaque))
    }

    @Test
    fun normalizeParsedCorruptSeedChainNeverThrows() {
        val chain = MonetColorDefaults.normalizeSeed(MonetColorDefaults.parseStoredSeed("garbage"))
        assertNull(chain)
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
        assertFalse(MonetColorDefaults.isOpaque(0x806750A4.toInt()))
    }

    private fun Int.toHex(): String = "#" + (this and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
}
