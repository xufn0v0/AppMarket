package com.app.market.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.market.domain.model.preference.ThemeMode
import com.app.market.platform.isBlurSettingSupported
import com.app.market.platform.isDynamicColorSupported
import com.app.market.platform.isPredictiveBackSupported
import com.app.market.resources.Res
import com.app.market.resources.cancel
import com.app.market.resources.theme
import com.app.market.resources.theme_appearance_dark
import com.app.market.resources.theme_appearance_dark_summary
import com.app.market.resources.theme_appearance_light
import com.app.market.resources.theme_appearance_light_summary
import com.app.market.resources.theme_appearance_system
import com.app.market.resources.theme_appearance_system_summary
import com.app.market.resources.theme_dynamic_color
import com.app.market.resources.theme_dynamic_color_summary
import com.app.market.resources.theme_enable_blur
import com.app.market.resources.theme_enable_blur_summary
import com.app.market.resources.theme_enable_glass
import com.app.market.resources.theme_enable_glass_summary
import com.app.market.resources.theme_floating_bottom_bar
import com.app.market.resources.theme_floating_bottom_bar_summary
import com.app.market.resources.theme_navigation_badge
import com.app.market.resources.theme_navigation_badge_summary
import com.app.market.resources.theme_page_scale
import com.app.market.resources.theme_page_scale_summary
import com.app.market.resources.theme_predictive_back
import com.app.market.resources.theme_predictive_back_summary
import com.app.market.resources.theme_seed_color
import com.app.market.resources.theme_seed_follow_wallpaper
import com.app.market.ui.component.AppTextButton
import com.app.market.ui.component.MarketScaffold
import com.app.market.ui.component.PageVerticalPadding
import com.app.market.ui.component.ScaleDialog
import com.app.market.viewmodel.ThemeSettingsUiState
import com.app.market.viewmodel.ThemeSettingsViewModel
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.RadioButtonLocation
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun ThemeSettingsScreen(
    viewModel: ThemeSettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    MarketScaffold(
        title = stringResource(Res.string.theme),
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding, backdropModifier, scrollBehavior ->
        val layoutDirection = LocalLayoutDirection.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdropModifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
                end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
                top = innerPadding.calculateTopPadding() + PageVerticalPadding,
                bottom = innerPadding.calculateBottomPadding() + PageVerticalPadding,
            ),
        ) {
            item(key = "appearance") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    AppearanceRow(
                        label = stringResource(Res.string.theme_appearance_system),
                        summary = stringResource(Res.string.theme_appearance_system_summary),
                        selected = state.themeMode == ThemeMode.SYSTEM,
                        onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) },
                    )
                    AppearanceRow(
                        label = stringResource(Res.string.theme_appearance_light),
                        summary = stringResource(Res.string.theme_appearance_light_summary),
                        selected = state.themeMode == ThemeMode.LIGHT,
                        onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) },
                    )
                    AppearanceRow(
                        label = stringResource(Res.string.theme_appearance_dark),
                        summary = stringResource(Res.string.theme_appearance_dark_summary),
                        selected = state.themeMode == ThemeMode.DARK,
                        onClick = { viewModel.setThemeMode(ThemeMode.DARK) },
                    )
                }
            }

            if (isDynamicColorSupported()) {
                item(key = "dynamic-color") {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        SwitchPreference(
                            title = stringResource(Res.string.theme_dynamic_color),
                            summary = stringResource(Res.string.theme_dynamic_color_summary),
                            checked = state.enableDynamicColor,
                            onCheckedChange = viewModel::setEnableDynamicColor,
                        )
                        AnimatedVisibility(
                            visible = state.enableDynamicColor,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            ArrowPreference(
                                title = stringResource(Res.string.theme_seed_color),
                                summary = state.monetSeedColor?.let(::seedHex)
                                    ?: stringResource(Res.string.theme_seed_follow_wallpaper),
                                endActions = {
                                    Surface(
                                        modifier = Modifier.size(18.dp),
                                        shape = CircleShape,
                                        color = state.monetSeedColor?.let { Color(it) }
                                            ?: MiuixTheme.colorScheme.dividerLine,
                                    ) {
                                        // Decorative read-only preview dot.
                                    }
                                },
                                onClick = viewModel::showSeedColorPicker,
                            )
                        }
                    }
                }
            }

            item(key = "effects") {
                val blurSupported = isBlurSettingSupported()
                Card(modifier = Modifier.fillMaxWidth()) {
                    if (blurSupported) {
                        SwitchPreference(
                            title = stringResource(Res.string.theme_enable_blur),
                            summary = stringResource(Res.string.theme_enable_blur_summary),
                            checked = state.enableBlur,
                            onCheckedChange = viewModel::setEnableBlur,
                        )
                    }
                    SwitchPreference(
                        title = stringResource(Res.string.theme_floating_bottom_bar),
                        summary = stringResource(Res.string.theme_floating_bottom_bar_summary),
                        checked = state.enableFloatingBottomBar,
                        onCheckedChange = viewModel::setEnableFloatingBottomBar,
                    )
                    if (blurSupported) {
                        AnimatedVisibility(
                            visible = state.enableFloatingBottomBar,
                            enter = expandVertically(),
                            exit = shrinkVertically(),
                        ) {
                            SwitchPreference(
                                title = stringResource(Res.string.theme_enable_glass),
                                summary = stringResource(Res.string.theme_enable_glass_summary),
                                checked = state.enableFloatingBottomBarBlur,
                                onCheckedChange = viewModel::setEnableFloatingBottomBarBlur,
                            )
                        }
                    }
                }
            }

            item(key = "navigation-badge") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    SwitchPreference(
                        title = stringResource(Res.string.theme_navigation_badge),
                        summary = stringResource(Res.string.theme_navigation_badge_summary),
                        checked = state.enableNavigationBadge,
                        onCheckedChange = viewModel::setEnableNavigationBadge,
                    )
                }
            }

            item(key = "gestures") {
                Card(modifier = Modifier.fillMaxWidth()) {
                    val predictiveBackSupported = isPredictiveBackSupported()
                    if (predictiveBackSupported) {
                        SwitchPreference(
                            title = stringResource(Res.string.theme_predictive_back),
                            summary = stringResource(Res.string.theme_predictive_back_summary),
                            checked = state.enablePredictiveBack,
                            onCheckedChange = viewModel::setEnablePredictiveBack,
                        )
                    }
                    var sliderValue by remember(state.pageScale) { mutableFloatStateOf(state.pageScale) }
                    ArrowPreference(
                        title = stringResource(Res.string.theme_page_scale),
                        summary = stringResource(Res.string.theme_page_scale_summary),
                        endActions = {
                            Text(
                                "${(sliderValue * 100).toInt()}%",
                                color = MiuixTheme.colorScheme.onSurfaceVariantActions,
                            )
                        },
                        onClick = viewModel::showScaleDialog,
                        bottomAction = {
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = { viewModel.setPageScale(sliderValue) },
                                valueRange = 0.8f..1.1f,
                                showKeyPoints = true,
                                keyPoints = listOf(0.8f, 0.9f, 1f, 1.1f),
                                magnetThreshold = 0.01f,
                                hapticEffect = SliderDefaults.SliderHapticEffect.Step,
                            )
                        },
                    )
                }
            }
        }

        ScaleDialog(
            show = state.showScaleDialog,
            onDismissRequest = viewModel::dismissScaleDialog,
            scaleProvider = { state.pageScale },
            onScaleChange = viewModel::setPageScale,
        )
        SeedColorPickerDialog(state = state, viewModel = viewModel)
    }
}

/**
 * Monet seed picker. Mirrors the installer picker: a [WindowDialog] hosting official
 * [RadioButtonPreference] rows (radio at the end) plus a cancel button.
 */
@Composable
private fun SeedColorPickerDialog(
    state: ThemeSettingsUiState,
    viewModel: ThemeSettingsViewModel,
) {
    WindowDialog(
        show = state.showSeedColorPicker,
        title = stringResource(Res.string.theme_seed_color),
        onDismissRequest = viewModel::dismissSeedColorPicker,
        insideMargin = DpSize(0.dp, 24.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 420.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
            ) {
                RadioButtonPreference(
                    title = stringResource(Res.string.theme_seed_follow_wallpaper),
                    selected = state.monetSeedColor == null,
                    radioButtonLocation = RadioButtonLocation.End,
                    insideMargin = PaddingValues(24.dp, 16.dp),
                    onClick = { viewModel.setMonetSeedColor(null) },
                )
                PresetSeedColors.forEach { argb ->
                    RadioButtonPreference(
                        title = seedHex(argb),
                        selected = state.monetSeedColor == argb,
                        radioButtonLocation = RadioButtonLocation.End,
                        insideMargin = PaddingValues(24.dp, 16.dp),
                        endActions = {
                            Surface(
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp),
                                shape = CircleShape,
                                color = Color(argb),
                            ) {
                                // Color preview of the option.
                            }
                        },
                        onClick = { viewModel.setMonetSeedColor(argb) },
                    )
                }
            }
            AppTextButton(
                text = stringResource(Res.string.cancel),
                onClick = viewModel::dismissSeedColorPicker,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
            )
        }
    }
}

/** Single-choice appearance row: official [RadioButtonPreference] with the radio at the end. */
@Composable
private fun AppearanceRow(
    label: String,
    summary: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    RadioButtonPreference(
        title = label,
        summary = summary,
        selected = selected,
        onClick = onClick,
        radioButtonLocation = RadioButtonLocation.End,
    )
}

/** Preset seed colors offered for Monet palette generation (Material You style palette). */
private val PresetSeedColors = listOf(
    0xFF6750A4.toInt(),
    0xFF006A60.toInt(),
    0xFF1565C0.toInt(),
    0xFF2E6B34.toInt(),
    0xFF8A5A00.toInt(),
    0xFFB3261E.toInt(),
    0xFF984061.toInt(),
    0xFF4A4458.toInt(),
)

/** Formats an ARGB seed as "#RRGGBB" for display. */
private fun seedHex(argb: Int): String =
    "#" + (argb and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
