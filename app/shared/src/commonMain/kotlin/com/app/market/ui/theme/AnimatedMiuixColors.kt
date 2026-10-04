package com.app.market.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.theme.Colors

private const val THEME_TRANSITION_MILLIS = 450

private fun themeTransitionSpec(): AnimationSpec<Color> =
    tween(durationMillis = THEME_TRANSITION_MILLIS, easing = FastOutSlowInEasing)

/**
 * Crossfades every Miuix [Colors] role towards [target].
 *
 * Theme switches (light/dark, Monet palette, system dark change) produce a smooth
 * per-role transition instead of an instant swap; the first composition snaps
 * directly to [target] without animating.
 */
@Composable
fun rememberAnimatedMiuixColors(target: Colors): Colors {
    val spec = themeTransitionSpec()

    @Composable
    fun animated(color: Color, label: String) = animateColorAsState(
        targetValue = color,
        animationSpec = spec,
        label = label,
    ).value

    return target.copy(
        primary = animated(target.primary, "primary"),
        onPrimary = animated(target.onPrimary, "onPrimary"),
        primaryVariant = animated(target.primaryVariant, "primaryVariant"),
        onPrimaryVariant = animated(target.onPrimaryVariant, "onPrimaryVariant"),
        error = animated(target.error, "error"),
        onError = animated(target.onError, "onError"),
        errorContainer = animated(target.errorContainer, "errorContainer"),
        onErrorContainer = animated(target.onErrorContainer, "onErrorContainer"),
        disabledPrimary = animated(target.disabledPrimary, "disabledPrimary"),
        disabledOnPrimary = animated(target.disabledOnPrimary, "disabledOnPrimary"),
        disabledPrimaryButton = animated(target.disabledPrimaryButton, "disabledPrimaryButton"),
        disabledOnPrimaryButton = animated(target.disabledOnPrimaryButton, "disabledOnPrimaryButton"),
        disabledPrimarySlider = animated(target.disabledPrimarySlider, "disabledPrimarySlider"),
        primaryContainer = animated(target.primaryContainer, "primaryContainer"),
        onPrimaryContainer = animated(target.onPrimaryContainer, "onPrimaryContainer"),
        secondary = animated(target.secondary, "secondary"),
        onSecondary = animated(target.onSecondary, "onSecondary"),
        secondaryVariant = animated(target.secondaryVariant, "secondaryVariant"),
        onSecondaryVariant = animated(target.onSecondaryVariant, "onSecondaryVariant"),
        disabledSecondary = animated(target.disabledSecondary, "disabledSecondary"),
        disabledOnSecondary = animated(target.disabledOnSecondary, "disabledOnSecondary"),
        disabledSecondaryVariant = animated(target.disabledSecondaryVariant, "disabledSecondaryVariant"),
        disabledOnSecondaryVariant = animated(target.disabledOnSecondaryVariant, "disabledOnSecondaryVariant"),
        secondaryContainer = animated(target.secondaryContainer, "secondaryContainer"),
        onSecondaryContainer = animated(target.onSecondaryContainer, "onSecondaryContainer"),
        secondaryContainerVariant = animated(target.secondaryContainerVariant, "secondaryContainerVariant"),
        onSecondaryContainerVariant = animated(target.onSecondaryContainerVariant, "onSecondaryContainerVariant"),
        tertiaryContainer = animated(target.tertiaryContainer, "tertiaryContainer"),
        onTertiaryContainer = animated(target.onTertiaryContainer, "onTertiaryContainer"),
        tertiaryContainerVariant = animated(target.tertiaryContainerVariant, "tertiaryContainerVariant"),
        background = animated(target.background, "background"),
        onBackground = animated(target.onBackground, "onBackground"),
        onBackgroundVariant = animated(target.onBackgroundVariant, "onBackgroundVariant"),
        surface = animated(target.surface, "surface"),
        onSurface = animated(target.onSurface, "onSurface"),
        surfaceVariant = animated(target.surfaceVariant, "surfaceVariant"),
        onSurfaceSecondary = animated(target.onSurfaceSecondary, "onSurfaceSecondary"),
        onSurfaceVariantSummary = animated(target.onSurfaceVariantSummary, "onSurfaceVariantSummary"),
        onSurfaceVariantActions = animated(target.onSurfaceVariantActions, "onSurfaceVariantActions"),
        disabledOnSurface = animated(target.disabledOnSurface, "disabledOnSurface"),
        surfaceContainer = animated(target.surfaceContainer, "surfaceContainer"),
        onSurfaceContainer = animated(target.onSurfaceContainer, "onSurfaceContainer"),
        onSurfaceContainerVariant = animated(target.onSurfaceContainerVariant, "onSurfaceContainerVariant"),
        surfaceContainerHigh = animated(target.surfaceContainerHigh, "surfaceContainerHigh"),
        onSurfaceContainerHigh = animated(target.onSurfaceContainerHigh, "onSurfaceContainerHigh"),
        surfaceContainerHighest = animated(target.surfaceContainerHighest, "surfaceContainerHighest"),
        onSurfaceContainerHighest = animated(target.onSurfaceContainerHighest, "onSurfaceContainerHighest"),
        outline = animated(target.outline, "outline"),
        dividerLine = animated(target.dividerLine, "dividerLine"),
        windowDimming = animated(target.windowDimming, "windowDimming"),
        sliderKeyPoint = animated(target.sliderKeyPoint, "sliderKeyPoint"),
        sliderKeyPointForeground = animated(target.sliderKeyPointForeground, "sliderKeyPointForeground"),
        sliderBackground = animated(target.sliderBackground, "sliderBackground"),
    )
}
