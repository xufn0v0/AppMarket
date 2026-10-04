package com.app.market.domain.theme

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * MD3/MD3E 官方命名体系、基线精确色值表、色调位次表的静态验证。
 *
 * 本测试不依赖 Compose / 引擎，仅校验纯 Kotlin 定义与 Material Design 规范的一致性。
 */
class MaterialDesignColorsTest {

    // ------------------------------------------------------------------
    // 枚举完整性（官方命名逐字匹配）
    // ------------------------------------------------------------------

    @Test
    fun md3ColorRoleSpecNamesMatchOfficialTerminology() {
        val official = listOf(
            "primary", "onPrimary", "primaryContainer", "onPrimaryContainer",
            "secondary", "onSecondary", "secondaryContainer", "onSecondaryContainer",
            "tertiary", "onTertiary", "tertiaryContainer", "onTertiaryContainer",
            "error", "onError", "errorContainer", "onErrorContainer",
            "background", "onBackground",
            "surface", "onSurface", "surfaceVariant", "onSurfaceVariant",
            "outline", "outlineVariant",
            "inverseSurface", "inverseOnSurface", "inversePrimary",
            "surfaceTint", "scrim", "shadow",
        )
        assertEquals(
            official,
            MaterialDesignColors.Md3ColorRole.entries.map { it.specName },
            "MD3 核心角色枚举名称与官方规范术语不一致",
        )
    }

    @Test
    fun md3ExtendedColorRoleSpecNamesMatchOfficialTerminology() {
        val official = listOf(
            "primaryFixed", "primaryFixedDim", "onPrimaryFixed", "onPrimaryFixedVariant",
            "secondaryFixed", "secondaryFixedDim", "onSecondaryFixed", "onSecondaryFixedVariant",
            "tertiaryFixed", "tertiaryFixedDim", "onTertiaryFixed", "onTertiaryFixedVariant",
            "surfaceDim", "surfaceBright",
            "surfaceContainerLowest", "surfaceContainerLow", "surfaceContainer",
            "surfaceContainerHigh", "surfaceContainerHighest",
        )
        assertEquals(
            official,
            MaterialDesignColors.Md3ExtendedColorRole.entries.map { it.specName },
            "MD3E 扩展角色枚举名称与官方规范术语不一致",
        )
    }

    @Test
    fun paletteStyleSpecNamesMatchOfficialTerminology() {
        val official = listOf(
            "TonalSpot", "Neutral", "Vibrant", "Expressive",
            "Rainbow", "FruitSalad", "Monochrome", "Fidelity", "Content",
        )
        assertEquals(
            official,
            MaterialDesignColors.MaterialTonalPaletteStyle.entries.map { it.specName },
            "调色板风格枚举名称与官方规范不一致",
        )
    }

    // ------------------------------------------------------------------
    // 基线精确色值：浅/深双模式齐全
    // ------------------------------------------------------------------

    @Test
    fun coreLightBaselineContainsAll30Roles() {
        assertEquals(30, MaterialDesignColors.Md3ColorRole.entries.size)
        MaterialDesignColors.Md3ColorRole.entries.forEach { role ->
            val argb = MaterialDesignColors.coreBaseline(role, dark = false)
            assertTrue(MonetColorDefaults.isOpaque(argb), "$role 浅色基线色必须为不透明")
        }
    }

    @Test
    fun coreDarkBaselineContainsAll30Roles() {
        MaterialDesignColors.Md3ColorRole.entries.forEach { role ->
            val argb = MaterialDesignColors.coreBaseline(role, dark = true)
            assertTrue(MonetColorDefaults.isOpaque(argb), "$role 深色基线色必须为不透明")
        }
    }

    @Test
    fun baselinePrimaryIsM3PurpleInBothAppearances() {
        assertEquals(
            0xFF6750A4.toInt(),
            MaterialDesignColors.coreBaseline(MaterialDesignColors.Md3ColorRole.PRIMARY, dark = false),
            "浅色 primary 基线应为 #6750A4",
        )
        assertEquals(
            0xFFD0BCFF.toInt(),
            MaterialDesignColors.coreBaseline(MaterialDesignColors.Md3ColorRole.PRIMARY, dark = true),
            "深色 primary 基线应为 #D0BCFF",
        )
    }

    // ------------------------------------------------------------------
    // 基线方案 WCAG 2.1 AA
    // ------------------------------------------------------------------

    @Test
    fun coreBaselineLightMeetsAaForCriticalTextPairs() {
        val bg = MaterialDesignColors.coreBaseline(MaterialDesignColors.Md3ColorRole.BACKGROUND, dark = false)
        val onBg = MaterialDesignColors.coreBaseline(MaterialDesignColors.Md3ColorRole.ON_BACKGROUND, dark = false)
        val surface = MaterialDesignColors.coreBaseline(MaterialDesignColors.Md3ColorRole.SURFACE, dark = false)
        val onSurface = MaterialDesignColors.coreBaseline(MaterialDesignColors.Md3ColorRole.ON_SURFACE, dark = false)

        assertTrue(
            MonetColorDefaults.contrastRatio(onBg, bg) >= MonetColorDefaults.WCAG_AA_CONTRAST_NORMAL_TEXT,
            "onBackground/background light 未达 AA",
        )
        assertTrue(
            MonetColorDefaults.contrastRatio(onSurface, surface) >= MonetColorDefaults.WCAG_AA_CONTRAST_NORMAL_TEXT,
            "onSurface/surface light 未达 AA",
        )
    }

    @Test
    fun coreBaselineDarkMeetsAaForCriticalTextPairs() {
        val bg = MaterialDesignColors.coreBaseline(MaterialDesignColors.Md3ColorRole.BACKGROUND, dark = true)
        val onBg = MaterialDesignColors.coreBaseline(MaterialDesignColors.Md3ColorRole.ON_BACKGROUND, dark = true)
        val surface = MaterialDesignColors.coreBaseline(MaterialDesignColors.Md3ColorRole.SURFACE, dark = true)
        val onSurface = MaterialDesignColors.coreBaseline(MaterialDesignColors.Md3ColorRole.ON_SURFACE, dark = true)

        assertTrue(
            MonetColorDefaults.contrastRatio(onBg, bg) >= MonetColorDefaults.WCAG_AA_CONTRAST_NORMAL_TEXT,
            "onBackground/background dark 未达 AA",
        )
        assertTrue(
            MonetColorDefaults.contrastRatio(onSurface, surface) >= MonetColorDefaults.WCAG_AA_CONTRAST_NORMAL_TEXT,
            "onSurface/surface dark 未达 AA",
        )
    }

    // ------------------------------------------------------------------
    // MD3E 固定色：两模式一致
    // ------------------------------------------------------------------

    @Test
    fun fixedColorRolesAreIdenticalAcrossAppearances() {
        val fixedRoles = MaterialDesignColors.Md3ExtendedColorRole.entries.filter {
            it.specName.contains("Fixed")
        }
        assertTrue(fixedRoles.isNotEmpty(), "fixed 固定色角色不得为空")
        fixedRoles.forEach { role ->
            val light = MaterialDesignColors.extendedBaseline(role, dark = false)
            val dark = MaterialDesignColors.extendedBaseline(role, dark = true)
            assertEquals(light, dark, "$role 固定色在浅深模式下必须一致")
            assertTrue(MonetColorDefaults.isOpaque(light), "$role 固定色必须为不透明")
        }
    }

    // ------------------------------------------------------------------
    // 色调位次表：关键角色 tone 值符合规范
    // ------------------------------------------------------------------

    @Test
    fun tonalSpotLightPrimaryIsTone40() {
        val tone = MaterialDesignColors.SPEC2021_TONAL_SPOT_TONES_LIGHT
            .getValue(MaterialDesignColors.Md3ColorRole.PRIMARY)
        assertEquals(40, tone, "浅色 primary 官方 tone 位次应为 40")
    }

    @Test
    fun tonalSpotDarkPrimaryIsTone80() {
        val tone = MaterialDesignColors.SPEC2021_TONAL_SPOT_TONES_DARK
            .getValue(MaterialDesignColors.Md3ColorRole.PRIMARY)
        assertEquals(80, tone, "深色 primary 官方 tone 位次应为 80")
    }

    @Test
    fun tonalSpotLightSurfaceIsTone98() {
        val tone = MaterialDesignColors.SPEC2021_TONAL_SPOT_TONES_LIGHT
            .getValue(MaterialDesignColors.Md3ColorRole.SURFACE)
        assertEquals(98, tone, "浅色 surface 官方 tone 位次应为 98")
    }

    @Test
    fun tonalSpotDarkSurfaceIsTone6() {
        val tone = MaterialDesignColors.SPEC2021_TONAL_SPOT_TONES_DARK
            .getValue(MaterialDesignColors.Md3ColorRole.SURFACE)
        assertEquals(6, tone, "深色 surface 官方 tone 位次应为 6")
    }

    @Test
    fun tonalSpotErrorIsTone40InLightAnd80InDark() {
        val tLight = MaterialDesignColors.SPEC2021_TONAL_SPOT_TONES_LIGHT
            .getValue(MaterialDesignColors.Md3ColorRole.ERROR)
        val tDark = MaterialDesignColors.SPEC2021_TONAL_SPOT_TONES_DARK
            .getValue(MaterialDesignColors.Md3ColorRole.ERROR)
        assertEquals(40, tLight, "浅色 error 官方 tone 位次应为 40")
        assertEquals(80, tDark, "深色 error 官方 tone 位次应为 80")
    }

    // ------------------------------------------------------------------
    // 默认配置锁定
    // ------------------------------------------------------------------

    @Test
    fun defaultConfigIsSpec2021TonalSpot() {
        assertEquals(
            MaterialDesignColors.MaterialColorSpec.SPEC_2021,
            MaterialDesignColors.DEFAULT_SPEC,
            "默认规范版本必须为 Spec2021",
        )
        assertEquals(
            MaterialDesignColors.MaterialTonalPaletteStyle.TONAL_SPOT,
            MaterialDesignColors.DEFAULT_PALETTE_STYLE,
            "默认调色板风格必须为 TonalSpot",
        )
    }
}
