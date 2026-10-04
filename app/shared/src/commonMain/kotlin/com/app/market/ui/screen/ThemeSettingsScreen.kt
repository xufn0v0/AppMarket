package com.app.market.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.market.domain.model.preference.ThemeMode
import com.app.market.platform.isBlurSettingSupported
import com.app.market.platform.isDynamicColorSupported
import com.app.market.platform.isPredictiveBackSupported
import com.app.market.resources.Res
import com.app.market.resources.theme
import com.app.market.resources.theme_appearance
import com.app.market.resources.theme_appearance_dark
import com.app.market.resources.theme_appearance_light
import com.app.market.resources.theme_appearance_system
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
import com.app.market.ui.component.MarketScaffold
import com.app.market.ui.component.PageVerticalPadding
import com.app.market.ui.component.ScaleDialog
import com.app.market.viewmodel.ThemeSettingsViewModel
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.DropdownEntry
import top.yukonga.miuix.kmp.basic.DropdownItem
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
import top.yukonga.miuix.kmp.preference.WindowDropdownPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.scrollEndHaptic

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
                    val modeOptions = listOf(
                        stringResource(Res.string.theme_appearance_system),
                        stringResource(Res.string.theme_appearance_light),
                        stringResource(Res.string.theme_appearance_dark),
                    )
                    WindowDropdownPreference(
                        title = stringResource(Res.string.theme_appearance),
                        items = modeOptions,
                        selectedIndex = state.themeMode.ordinal,
                        onSelectedIndexChange = { viewModel.setThemeMode(ThemeMode.entries[it]) },
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
                            val followWallpaperLabel = stringResource(Res.string.theme_seed_follow_wallpaper)
                            val seedEntry = remember(state.monetSeedColor, followWallpaperLabel) {
                                DropdownEntry(
                                    items = buildList {
                                        add(
                                            DropdownItem(
                                                text = followWallpaperLabel,
                                                selected = state.monetSeedColor == null,
                                                onClick = { viewModel.setMonetSeedColor(null) },
                                            ),
                                        )
                                        addAll(
                                            PresetSeedColors.map { argb ->
                                                DropdownItem(
                                                    text = seedHex(argb),
                                                    selected = state.monetSeedColor == argb,
                                                    onClick = { viewModel.setMonetSeedColor(argb) },
                                                    icon = { iconModifier ->
                                                        Surface(
                                                            modifier = iconModifier,
                                                            shape = CircleShape,
                                                            color = Color(argb),
                                                        ) {
                                                            Spacer(Modifier.size(18.dp))
                                                        }
                                                    },
                                                )
                                            },
                                        )
                                    },
                                )
                            }
                            WindowDropdownPreference(
                                title = stringResource(Res.string.theme_seed_color),
                                entry = seedEntry,
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
    }
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
