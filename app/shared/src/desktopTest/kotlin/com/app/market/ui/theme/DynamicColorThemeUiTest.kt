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
 * Integration-level verification of the Monet dynamic-color wiring: a user-facing switch
 * rebuilds the [ThemeController], and the generated palette propagates to every component
 * reading [MiuixTheme.colorScheme], while keeping WCAG-readable contrast.
 */
class DynamicColorThemeUiTest {

    @get:Rule
    val rule = createComposeRule()

    private val primaryColor = SemanticsPropertyKey<Long>("primaryColor")
    private val backgroundColor = SemanticsPropertyKey<Long>("backgroundColor")
    private val onBackgroundColor = SemanticsPropertyKey<Long>("onBackgroundColor")

    @Composable
    private fun DynamicColorHarness(initialDynamic: Boolean) {
        var dynamic by remember { mutableStateOf(initialDynamic) }
        val controller = remember(dynamic) {
            ThemeController(colorSchemeModeFor(dynamicColor = dynamic))
        }
        MiuixTheme(controller = controller) {
            val colors = MiuixTheme.colorScheme
            Column {
                Box(
                    modifier = Modifier
                        .testTag("swatch")
                        .semantics {
                            this[primaryColor] = colors.primary.toArgb().toLong()
                            this[backgroundColor] = colors.background.toArgb().toLong()
                            this[onBackgroundColor] = colors.onBackground.toArgb().toLong()
                        },
                )
                Switch(
                    checked = dynamic,
                    onCheckedChange = { dynamic = it },
                    modifier = Modifier.testTag("toggle"),
                )
            }
        }
    }

    @Test
    fun togglingDynamicColorReplacesThePalette() {
        rule.setContent { DynamicColorHarness(initialDynamic = false) }

        val staticPrimary = rule.onNodeWithTag("swatch").semanticsColor(primaryColor)
        // Default light scheme uses the Miuix brand blue.
        assertNotEquals(Color(0xFF6750A4), staticPrimary)

        rule.onNodeWithTag("toggle").performClick()
        rule.waitForIdle()

        // Desktop Monet degrades to the deterministic baseline TonalSpot seed; primary changes.
        val monetPrimary = rule.onNodeWithTag("swatch").semanticsColor(primaryColor)
        assertNotEquals(staticPrimary, monetPrimary)
    }

    @Test
    fun monetPaletteKeepsReadableContrast() {
        rule.setContent { DynamicColorHarness(initialDynamic = true) }

        val background = rule.onNodeWithTag("swatch").semanticsColor(backgroundColor)
        val onBackground = rule.onNodeWithTag("swatch").semanticsColor(onBackgroundColor)

        assertTrue(
            "Background/onBackground contrast must meet WCAG AA (4.5:1)",
            contrastRatio(onBackground, background) >= 4.5f,
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
