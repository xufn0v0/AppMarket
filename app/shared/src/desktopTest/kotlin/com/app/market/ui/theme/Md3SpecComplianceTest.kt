package com.app.market.ui.theme

import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.v2.createComposeRule
import com.app.market.domain.theme.MaterialDesignColors
import com.app.market.domain.theme.MonetColorDefaults
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.Colors
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.ThemePaletteStyle
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * MD3/MD3E 规范符合性测试：验证引擎输出与官方色调位次精确一致、
 * 角色映射分类正确、以及 Spec2025 同样满足 WCAG AA。
 */
class Md3SpecComplianceTest {

    @get:Rule
    val rule = createComposeRule()

    private fun renderPalette(
        seedArgb: Int? = MonetColorDefaults.DEFAULT_SEED_COLOR_ARGB,
        dark: Boolean,
        spec: ThemeColorSpec = ThemeColorSpec.Spec2021,
    ): Colors {
        var captured: Colors? = null
        rule.setContent {
            val controller = remember(seedArgb, dark, spec) {
                ThemeController(
                    colorSchemeMode = if (dark) ColorSchemeMode.MonetDark else ColorSchemeMode.MonetLight,
                    keyColor = seedArgb?.let { Color(it) },
                    colorSpec = spec,
                    paletteStyle = ThemePaletteStyle.TonalSpot,
                )
            }
            MiuixTheme(colors = controller.currentColors()) {
                val scheme = MiuixTheme.colorScheme
                SideEffect { captured = scheme }
            }
        }
        rule.waitForIdle()
        return captured ?: error("配色快照未生成：seed=$seedArgb dark=$dark spec=$spec")
    }

    // ------------------------------------------------------------------
    // 映射完整性：全部 30 个 MD3 核心角色 + 19 个 MD3E 扩展角色
    // ------------------------------------------------------------------

    @Test
    fun all30CoreRolesHaveDefinedAvailability() {
        MaterialDesignColors.Md3ColorRole.entries.forEach { role ->
            val availability = MaterialColorSchemeMapping.availabilityOf(role)
            val light = renderPalette(dark = false)
            val color = with(MaterialColorSchemeMapping) { light.md3ColorOrNull(role) }
            when (availability) {
                MaterialColorSchemeMapping.Md3RoleAvailability.DIRECT ->
                    assertNotNull(color, "$role 为 DIRECT，必须能取到颜色")
                MaterialColorSchemeMapping.Md3RoleAvailability.MIUI_ADAPTED ->
                    assertNotNull(color, "$role 为 MIUI_ADAPTED，必须能取到颜色")
                MaterialColorSchemeMapping.Md3RoleAvailability.ENGINE_INTERNAL ->
                    assertNull(color, "$role 为 ENGINE_INTERNAL，不应直接暴露")
            }
        }
    }

    @Test
    fun all19ExtendedRolesHaveDefinedAvailability() {
        MaterialDesignColors.Md3ExtendedColorRole.entries.forEach { role ->
            val availability = MaterialColorSchemeMapping.availabilityOf(role)
            val light = renderPalette(dark = false)
            val color = with(MaterialColorSchemeMapping) { light.md3ExtendedColorOrNull(role) }
            when (availability) {
                MaterialColorSchemeMapping.Md3RoleAvailability.DIRECT ->
                    assertNotNull(color, "$role 为 DIRECT，必须能取到颜色")
                MaterialColorSchemeMapping.Md3RoleAvailability.MIUI_ADAPTED ->
                    assertNotNull(color, "$role 为 MIUI_ADAPTED，必须能取到颜色")
                MaterialColorSchemeMapping.Md3RoleAvailability.ENGINE_INTERNAL ->
                    assertNull(color, "$role 为 ENGINE_INTERNAL，不应直接暴露")
            }
        }
    }

    // ------------------------------------------------------------------
    // 精确数值：引擎输出 tone 位次与 Spec2021 TonalSpot 官方表一致
    // ------------------------------------------------------------------

    @Test
    fun engineOutputMatchesSpec2021TonalSpotTonesLight() {
        val colors = renderPalette(dark = false)
        val tones = MaterialDesignColors.SPEC2021_TONAL_SPOT_TONES_LIGHT
        tones.forEach { (role, expectedTone) ->
            val actualColor = with(MaterialColorSchemeMapping) { colors.md3ColorOrNull(role) }
            if (actualColor != null) {
                val actualTone = MonetColorDefaults.hctTone(actualColor.toArgb())
                assertTrue(
                    abs(actualTone - expectedTone) <= 1.5,
                    "[light] ${role.specName} 官方 tone=$expectedTone，实测=${"%.1f".format(actualTone)}",
                )
            }
        }
    }

    @Test
    fun engineOutputMatchesSpec2021TonalSpotTonesDark() {
        val colors = renderPalette(dark = true)
        val tones = MaterialDesignColors.SPEC2021_TONAL_SPOT_TONES_DARK
        tones.forEach { (role, expectedTone) ->
            val actualColor = with(MaterialColorSchemeMapping) { colors.md3ColorOrNull(role) }
            if (actualColor != null) {
                val actualTone = MonetColorDefaults.hctTone(actualColor.toArgb())
                assertTrue(
                    abs(actualTone - expectedTone) <= 1.5,
                    "[dark] ${role.specName} 官方 tone=$expectedTone，实测=${"%.1f".format(actualTone)}",
                )
            }
        }
    }

    // ------------------------------------------------------------------
    // MD3E (Spec2025) 同样满足 WCAG AA
    // ------------------------------------------------------------------

    @Test
    fun spec2025MeetsWcagAaInBothAppearances() {
        listOf(false, true).forEach { dark ->
            val colors = renderPalette(dark = dark, spec = ThemeColorSpec.Spec2025)
            assertAllCriticalPairsMeetAa(colors, "Spec2025 / dark=$dark")
        }
    }

    // ------------------------------------------------------------------
    // 默认种子与基线 primary 一致
    // ------------------------------------------------------------------

    @Test
    fun defaultSeedYieldsBaselinePrimary() {
        val colors = renderPalette(dark = false)
        val baseline = MaterialDesignColors.coreBaseline(
            MaterialDesignColors.Md3ColorRole.PRIMARY, dark = false,
        )
        // 基线色值与引擎生成值应在同一 tone 邻域（±1.5）
        val baselineTone = MonetColorDefaults.hctTone(baseline)
        val actualTone = MonetColorDefaults.hctTone(colors.primary.toArgb())
        assertTrue(
            abs(baselineTone - actualTone) <= 1.5,
            "默认种子生成的 primary 与 MD3 基线 #6750A4 的 tone 位次不一致",
        )
    }

    // ------------------------------------------------------------------
    // 辅助：关键前景/背景对 AA 断言
    // ------------------------------------------------------------------

    private fun assertAllCriticalPairsMeetAa(colors: Colors, scene: String) {
        val failures = listOf(
            "onPrimary/primary" to MonetColorDefaults.contrastRatio(
                colors.onPrimary.toArgb(), colors.primary.toArgb(),
            ),
            "onPrimaryContainer/primaryContainer" to MonetColorDefaults.contrastRatio(
                colors.onPrimaryContainer.toArgb(), colors.primaryContainer.toArgb(),
            ),
            "onSecondaryContainer/secondaryContainer" to MonetColorDefaults.contrastRatio(
                colors.onSecondaryContainer.toArgb(), colors.secondaryContainer.toArgb(),
            ),
            "onError/error" to MonetColorDefaults.contrastRatio(
                colors.onError.toArgb(), colors.error.toArgb(),
            ),
            "onErrorContainer/errorContainer" to MonetColorDefaults.contrastRatio(
                colors.onErrorContainer.toArgb(), colors.errorContainer.toArgb(),
            ),
            "onBackground/background" to MonetColorDefaults.contrastRatio(
                colors.onBackground.toArgb(), colors.background.toArgb(),
            ),
            "onSurface/surface" to MonetColorDefaults.contrastRatio(
                colors.onSurface.toArgb(), colors.surface.toArgb(),
            ),
        ).filter { it.second < MonetColorDefaults.WCAG_AA_CONTRAST_NORMAL_TEXT }
            .map { "${it.first}=${"%.2f".format(it.second)}" }
        assertTrue(failures.isEmpty(), "[$scene] 以下角色对未达 WCAG AA(4.5:1)：$failures")
    }
}
