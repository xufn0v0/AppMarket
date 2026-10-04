package com.app.market.ui.theme

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import com.app.market.colorSchemeModeFor
import com.app.market.domain.model.preference.ThemeMode
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import top.yukonga.miuix.kmp.basic.Switch
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import kotlin.math.max
import kotlin.math.min
import kotlin.test.assertNotEquals

/**
 * Integration-level verification of the appearance wiring: theme-mode toggles rebuild the
 * [ThemeController], the Monet palette propagates to every component reading
 * [MiuixTheme.colorScheme], and generated palettes keep WCAG-readable contrast in both
 * light and dark appearances.
 */
class DynamicColorThemeUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val primaryColor = SemanticsPropertyKey<Long>("primaryColor")
    private val onPrimaryColor = SemanticsPropertyKey<Long>("onPrimaryColor")
    private val backgroundColor = SemanticsPropertyKey<Long>("backgroundColor")
    private val onBackgroundColor = SemanticsPropertyKey<Long>("onBackgroundColor")

    @Composable
    private fun ThemeHarness(
        initialMode: ThemeMode = ThemeMode.SYSTEM,
        initialDynamic: Boolean = false,
        initialSeed: Int? = null,
    ) {
        var mode by remember { mutableStateOf(initialMode) }
        var dynamic by remember { mutableStateOf(initialDynamic) }
        var seed by remember { mutableStateOf(initialSeed) }
        val controller = remember(mode, dynamic, seed) {
            ThemeController(
                colorSchemeMode = colorSchemeModeFor(mode, dynamic),
                keyColor = seed?.let { Color(it) },
            )
        }
        MiuixTheme(colors = rememberAnimatedMiuixColors(controller.currentColors())) {
            val colors = MiuixTheme.colorScheme
            Column {
                Box(
                    modifier = Modifier
                        .testTag("swatch")
                        .semantics {
                            this[primaryColor] = colors.primary.toArgb().toLong()
                            this[onPrimaryColor] = colors.onPrimary.toArgb().toLong()
                            this[backgroundColor] = colors.background.toArgb().toLong()
                            this[onBackgroundColor] = colors.onBackground.toArgb().toLong()
                        },
                )
                Switch(
                    checked = dynamic,
                    onCheckedChange = { dynamic = it },
                    modifier = Modifier.testTag("dynamic-toggle"),
                )
                Switch(
                    checked = mode == ThemeMode.DARK,
                    onCheckedChange = { dark -> mode = if (dark) ThemeMode.DARK else ThemeMode.SYSTEM },
                    modifier = Modifier.testTag("dark-toggle"),
                )
            }
        }
    }

    @Test
    fun togglingDynamicColorReplacesThePalette() {
        rule.setContent { ThemeHarness(initialDynamic = false) }

        val staticPrimary = rule.onNodeWithTag("swatch").semanticsColor(primaryColor)
        // Default light scheme uses the Miuix brand blue.
        assertNotEquals(Color(0xFF6750A4), staticPrimary)

        rule.onNodeWithTag("dynamic-toggle").performClick()
        rule.waitForIdle()

        // Desktop Monet degrades to the deterministic baseline TonalSpot seed; primary changes.
        val monetPrimary = rule.onNodeWithTag("swatch").semanticsColor(primaryColor)
        assertNotEquals(staticPrimary, monetPrimary)
    }

    @Test
    fun forcingDarkModeSwitchesToDarkPalette() {
        // MonetLight is deterministic on desktop; MonetSystem would depend on the OS setting.
        rule.setContent { ThemeHarness(initialMode = ThemeMode.LIGHT, initialDynamic = true) }
        rule.waitForIdle()

        val lightBackground = rule.onNodeWithTag("swatch").semanticsColor(backgroundColor)
        val lightPrimary = rule.onNodeWithTag("swatch").semanticsColor(primaryColor)

        rule.onNodeWithTag("dark-toggle").performClick()
        rule.waitForIdle()

        val darkBackground = rule.onNodeWithTag("swatch").semanticsColor(backgroundColor)
        val darkPrimary = rule.onNodeWithTag("swatch").semanticsColor(primaryColor)
        assertTrue(
            "Forcing dark must darken the background",
            darkBackground.luminance() < lightBackground.luminance(),
        )
        assertNotEquals(lightPrimary, darkPrimary)
    }

    @Test
    fun customSeedColorOverridesTheBaselinePalette() {
        rule.setContent { ThemeHarness(initialMode = ThemeMode.LIGHT, initialDynamic = true) }
        rule.waitForIdle()
        val baselinePrimary = rule.onNodeWithTag("swatch").semanticsColor(primaryColor)

        rule.setContent {
            ThemeHarness(initialMode = ThemeMode.LIGHT, initialDynamic = true, initialSeed = 0xFF1565C0.toInt())
        }
        rule.waitForIdle()

        val seededPrimary = rule.onNodeWithTag("swatch").semanticsColor(primaryColor)
        assertNotEquals(baselinePrimary, seededPrimary)
        val background = rule.onNodeWithTag("swatch").semanticsColor(backgroundColor)
        val onBackground = rule.onNodeWithTag("swatch").semanticsColor(onBackgroundColor)
        assertMeetsWcagAA(onBackground, background, "seeded palette")
    }

    @Test
    fun monetLightPaletteKeepsReadableContrast() {
        rule.setContent { ThemeHarness(initialMode = ThemeMode.LIGHT, initialDynamic = true) }
        rule.waitForIdle()

        assertMeetsWcagAA(
            rule.onNodeWithTag("swatch").semanticsColor(onBackgroundColor),
            rule.onNodeWithTag("swatch").semanticsColor(backgroundColor),
            "Monet light background",
        )
        assertMeetsWcagAA(
            rule.onNodeWithTag("swatch").semanticsColor(onPrimaryColor),
            rule.onNodeWithTag("swatch").semanticsColor(primaryColor),
            "Monet light primary",
        )
    }

    @Test
    fun monetDarkPaletteKeepsReadableContrast() {
        rule.setContent { ThemeHarness(initialMode = ThemeMode.DARK, initialDynamic = true) }
        rule.waitForIdle()

        assertMeetsWcagAA(
            rule.onNodeWithTag("swatch").semanticsColor(onBackgroundColor),
            rule.onNodeWithTag("swatch").semanticsColor(backgroundColor),
            "Monet dark background",
        )
        assertMeetsWcagAA(
            rule.onNodeWithTag("swatch").semanticsColor(onPrimaryColor),
            rule.onNodeWithTag("swatch").semanticsColor(primaryColor),
            "Monet dark primary",
        )
    }

    private fun assertMeetsWcagAA(foreground: Color, background: Color, label: String) {
        assertTrue(
            "$label contrast must meet WCAG AA (4.5:1)",
            contrastRatio(foreground, background) >= 4.5f,
        )
    }

    private fun SemanticsNodeInteraction.semanticsColor(key: SemanticsPropertyKey<Long>): Color =
        Color(fetchSemanticsNode().config[key].toInt())

    /** WCAG 2.x contrast ratio between two opaque colors. */
    private fun contrastRatio(foreground: Color, background: Color): Float {
        val lighter = max(foreground.luminance(), background.luminance())
        val darker = min(foreground.luminance(), background.luminance())
        return (lighter + 0.05f) / (darker + 0.05f)
    }
}
