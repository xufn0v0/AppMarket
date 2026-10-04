package com.app.market.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.app.market.ui.component.CardSegmentContainer
import com.app.market.ui.component.MarketScaffold
import com.app.market.ui.component.PageVerticalPadding
import com.app.market.ui.component.ScaleDialog
import com.app.market.ui.component.SeedColorDialog
import com.app.market.ui.component.ThemeModeDialog
import com.app.market.viewmodel.ThemeSettingsUiState
import com.app.market.viewmodel.ThemeSettingsViewModel
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.Slider
import top.yukonga.miuix.kmp.basic.SliderDefaults
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.preference.ArrowPreference
import top.yukonga.miuix.kmp.preference.SwitchPreference
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
    ThemeSettingsContent(
        state = state,
        onBack = onBack,
        onThemeMode = viewModel::setThemeMode,
        onMonetSeedColor = viewModel::setMonetSeedColor,
        onEnableDynamicColor = viewModel::setEnableDynamicColor,
        onEnableBlur = viewModel::setEnableBlur,
        onEnableFloatingBottomBar = viewModel::setEnableFloatingBottomBar,
        onEnableFloatingBottomBarBlur = viewModel::setEnableFloatingBottomBarBlur,
        onEnableNavigationBadge = viewModel::setEnableNavigationBadge,
        onEnablePredictiveBack = viewModel::setEnablePredictiveBack,
        onPageScale = viewModel::setPageScale,
        dynamicColorSupported = isDynamicColorSupported(),
        blurSupported = isBlurSettingSupported(),
        predictiveBackSupported = isPredictiveBackSupported(),
        modifier = modifier,
    )
}

@Composable
private fun ThemeSettingsContent(
    state: ThemeSettingsUiState,
    onBack: () -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onMonetSeedColor: (Int?) -> Unit,
    onEnableDynamicColor: (Boolean) -> Unit,
    onEnableBlur: (Boolean) -> Unit,
    onEnableFloatingBottomBar: (Boolean) -> Unit,
    onEnableFloatingBottomBarBlur: (Boolean) -> Unit,
    onEnableNavigationBadge: (Boolean) -> Unit,
    onEnablePredictiveBack: (Boolean) -> Unit,
    onPageScale: (Float) -> Unit,
    dynamicColorSupported: Boolean,
    blurSupported: Boolean,
    predictiveBackSupported: Boolean,
    modifier: Modifier = Modifier,
) {
    val layoutDirection = LocalLayoutDirection.current
    var sliderValue by remember(state.pageScale) { mutableFloatStateOf(state.pageScale) }
    var showScaleDialog by rememberSaveable { mutableStateOf(false) }
    var showAppearanceDialog by rememberSaveable { mutableStateOf(false) }
    var showSeedDialog by rememberSaveable { mutableStateOf(false) }
    val appearanceLabel = stringResource(
        when (state.themeMode) {
            ThemeMode.SYSTEM -> Res.string.theme_appearance_system
            ThemeMode.LIGHT -> Res.string.theme_appearance_light
            ThemeMode.DARK -> Res.string.theme_appearance_dark
        },
    )

    MarketScaffold(
        title = stringResource(Res.string.theme),
        onBack = onBack,
        modifier = modifier,
    ) { innerPadding, backdropModifier, scrollBehavior ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .then(backdropModifier)
                .scrollEndHaptic()
                .overScrollVertical()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            contentPadding = PaddingValues(
                start = innerPadding.calculateStartPadding(layoutDirection) + 12.dp,
                end = innerPadding.calculateEndPadding(layoutDirection) + 12.dp,
                top = innerPadding.calculateTopPadding() + PageVerticalPadding,
                bottom = innerPadding.calculateBottomPadding() + PageVerticalPadding,
            ),
        ) {
            item(key = "appearance") {
                CardSegmentContainer(
                    isFirst = true,
                    isLast = false,
                    horizontalPadding = 0.dp,
                ) {
                    ArrowPreference(
                        title = stringResource(Res.string.theme_appearance),
                        endActions = {
                            Text(appearanceLabel, color = MiuixTheme.colorScheme.onSurfaceVariantActions)
                        },
                        onClick = { showAppearanceDialog = !showAppearanceDialog },
                        holdDownState = showAppearanceDialog,
                    )
                }
            }
            if (dynamicColorSupported) {
                item(key = "dynamic-color") {
                    CardSegmentContainer(
                        isFirst = false,
                        isLast = false,
                        horizontalPadding = 0.dp,
                    ) {
                        SwitchPreference(
                            title = stringResource(Res.string.theme_dynamic_color),
                            summary = stringResource(Res.string.theme_dynamic_color_summary),
                            checked = state.enableDynamicColor,
                            onCheckedChange = onEnableDynamicColor,
                        )
                    }
                }
                item(key = "monet-seed") {
                    AnimatedVisibility(
                        visible = state.enableDynamicColor,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        CardSegmentContainer(
                            isFirst = false,
                            isLast = false,
                            horizontalPadding = 0.dp,
                        ) {
                            ArrowPreference(
                                title = stringResource(Res.string.theme_seed_color),
                                summary = state.monetSeedColor?.let { seedHex(it) }
                                    ?: stringResource(Res.string.theme_seed_follow_wallpaper),
                                endActions = {
                                    Surface(
                                        modifier = Modifier.size(18.dp),
                                        shape = CircleShape,
                                        color = state.monetSeedColor?.let { Color(it) }
                                            ?: MiuixTheme.colorScheme.dividerLine,
                                    ) {
                                        // Decorative read-only preview dot (MUIX Surface).
                                    }
                                },
                                onClick = { showSeedDialog = !showSeedDialog },
                                holdDownState = showSeedDialog,
                            )
                        }
                    }
                }
            }
            if (blurSupported) {
                item(key = "blur") {
                    CardSegmentContainer(
                        isFirst = false,
                        isLast = false,
                        horizontalPadding = 0.dp,
                    ) {
                        SwitchPreference(
                            title = stringResource(Res.string.theme_enable_blur),
                            summary = stringResource(Res.string.theme_enable_blur_summary),
                            checked = state.enableBlur,
                            onCheckedChange = onEnableBlur,
                        )
                    }
                }
            }
            item(key = "floating-bottom-bar") {
                CardSegmentContainer(
                    isFirst = false,
                    isLast = false,
                    horizontalPadding = 0.dp,
                ) {
                    SwitchPreference(
                        title = stringResource(Res.string.theme_floating_bottom_bar),
                        summary = stringResource(Res.string.theme_floating_bottom_bar_summary),
                        checked = state.enableFloatingBottomBar,
                        onCheckedChange = onEnableFloatingBottomBar,
                    )
                }
            }
            if (blurSupported) {
                item(key = "floating-bottom-bar-glass") {
                    AnimatedVisibility(
                        visible = state.enableFloatingBottomBar,
                        enter = expandVertically(),
                        exit = shrinkVertically(),
                    ) {
                        CardSegmentContainer(
                            isFirst = false,
                            isLast = false,
                            horizontalPadding = 0.dp,
                        ) {
                            SwitchPreference(
                                title = stringResource(Res.string.theme_enable_glass),
                                summary = stringResource(Res.string.theme_enable_glass_summary),
                                checked = state.enableFloatingBottomBarBlur,
                                onCheckedChange = onEnableFloatingBottomBarBlur,
                            )
                        }
                    }
                }
            }
            item(key = "navigation-badge") {
                CardSegmentContainer(
                    isFirst = false,
                    isLast = true,
                    horizontalPadding = 0.dp,
                ) {
                    SwitchPreference(
                        title = stringResource(Res.string.theme_navigation_badge),
                        summary = stringResource(Res.string.theme_navigation_badge_summary),
                        checked = state.enableNavigationBadge,
                        onCheckedChange = onEnableNavigationBadge,
                    )
                }
            }
            if (predictiveBackSupported) {
                item(key = "predictive-back") {
                    CardSegmentContainer(
                        isFirst = true,
                        isLast = false,
                        modifier = Modifier.padding(top = PageVerticalPadding),
                        horizontalPadding = 0.dp,
                    ) {
                        SwitchPreference(
                            title = stringResource(Res.string.theme_predictive_back),
                            summary = stringResource(Res.string.theme_predictive_back_summary),
                            checked = state.enablePredictiveBack,
                            onCheckedChange = onEnablePredictiveBack,
                        )
                    }
                }
            }
            item(key = "page-scale") {
                CardSegmentContainer(
                    isFirst = !predictiveBackSupported,
                    isLast = true,
                    modifier = if (predictiveBackSupported) Modifier else Modifier.padding(top = PageVerticalPadding),
                    horizontalPadding = 0.dp,
                ) {
                    ArrowPreference(
                        title = stringResource(Res.string.theme_page_scale),
                        summary = stringResource(Res.string.theme_page_scale_summary),
                        endActions = { Text("${(sliderValue * 100).toInt()}%", color = MiuixTheme.colorScheme.onSurfaceVariantActions) },
                        onClick = { showScaleDialog = !showScaleDialog },
                        holdDownState = showScaleDialog,
                        bottomAction = {
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = { onPageScale(sliderValue) },
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
            show = showScaleDialog,
            onDismissRequest = { showScaleDialog = false },
            scaleProvider = { state.pageScale },
            onScaleChange = onPageScale,
        )
        ThemeModeDialog(
            show = showAppearanceDialog,
            onDismissRequest = { showAppearanceDialog = false },
            modeProvider = { state.themeMode },
            onModeSelect = onThemeMode,
        )
        SeedColorDialog(
            show = showSeedDialog,
            onDismissRequest = { showSeedDialog = false },
            seedProvider = { state.monetSeedColor },
            onSeedSelect = onMonetSeedColor,
        )
    }
}

/** Formats an ARGB seed as "#RRGGBB" for display in the preference summary. */
private fun seedHex(argb: Int): String =
    "#" + (argb and 0xFFFFFF).toString(16).uppercase().padStart(6, '0')
