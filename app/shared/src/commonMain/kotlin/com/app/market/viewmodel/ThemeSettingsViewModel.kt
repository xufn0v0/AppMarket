package com.app.market.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.market.domain.model.preference.ThemeMode
import com.app.market.domain.repository.ThemePreferencesRepository
import com.app.market.domain.theme.MaterialDesignColors
import com.app.market.domain.theme.MaterialDesignColors.MaterialColorSpec
import com.app.market.domain.theme.MaterialDesignColors.MaterialTonalPaletteStyle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class ThemeSettingsUiState(
    val enableBlur: Boolean = false,
    val enableFloatingBottomBar: Boolean = false,
    val enableFloatingBottomBarBlur: Boolean = false,
    val enableNavigationBadge: Boolean = true,
    val enableDynamicColor: Boolean = false,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val paletteStyle: MaterialTonalPaletteStyle = MaterialDesignColors.DEFAULT_PALETTE_STYLE,
    val colorSpec: MaterialColorSpec = MaterialDesignColors.DEFAULT_SPEC,
    /** 实际生效的规范版本（Spec2025 + 不兼容风格时自动降级为 Spec2021）。 */
    val effectiveColorSpec: MaterialColorSpec = MaterialDesignColors.DEFAULT_SPEC,
    val showScaleDialog: Boolean = false,
    val enablePredictiveBack: Boolean = false,
    val pageScale: Float = 1f,
)

class ThemeSettingsViewModel(
    private val preferences: ThemePreferencesRepository,
) : ViewModel() {
    private data class AppearancePreferences(
        val enableBlur: Boolean,
        val enableFloatingBottomBar: Boolean,
        val enableDynamicColor: Boolean,
        val themeMode: ThemeMode,
        val paletteStyle: MaterialTonalPaletteStyle,
        val colorSpec: MaterialColorSpec,
    )

    private val appearance = combine(
        combine(
            preferences.enableBlur,
            preferences.enableFloatingBottomBar,
            preferences.enableDynamicColor,
            preferences.themeMode,
        ) { blur, floating, dynamicColor, themeMode ->
            AppearancePreferences(
                enableBlur = blur,
                enableFloatingBottomBar = floating,
                enableDynamicColor = dynamicColor,
                themeMode = themeMode,
                paletteStyle = MaterialDesignColors.DEFAULT_PALETTE_STYLE,
                colorSpec = MaterialDesignColors.DEFAULT_SPEC,
            )
        },
        preferences.paletteStyle,
        preferences.colorSpec,
    ) { appearance, paletteStyle, colorSpec ->
        appearance.copy(paletteStyle = paletteStyle, colorSpec = colorSpec)
    }

    private val showScaleDialog = MutableStateFlow(false)

    val uiState: StateFlow<ThemeSettingsUiState> = combine(
        combine(
            appearance,
            preferences.enableFloatingBottomBarBlur,
            preferences.enableNavigationBadge,
            preferences.enablePredictiveBack,
            preferences.pageScale,
        ) { appearance, glass, badge, predictiveBack, scale ->
            val (effectiveSpec, _) = MaterialDesignColors.resolveSpecAndStyle(
                appearance.colorSpec, appearance.paletteStyle,
            )
            ThemeSettingsUiState(
                enableBlur = appearance.enableBlur,
                enableFloatingBottomBar = appearance.enableFloatingBottomBar,
                enableDynamicColor = appearance.enableDynamicColor,
                themeMode = appearance.themeMode,
                paletteStyle = appearance.paletteStyle,
                colorSpec = appearance.colorSpec,
                effectiveColorSpec = effectiveSpec,
                enableFloatingBottomBarBlur = glass,
                enableNavigationBadge = badge,
                enablePredictiveBack = predictiveBack,
                pageScale = scale,
            )
        },
        showScaleDialog,
    ) { state, showScale ->
        state.copy(showScaleDialog = showScale)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeSettingsUiState())

    fun setEnableBlur(value: Boolean) = persist { preferences.setEnableBlur(value) }
    fun setEnableFloatingBottomBar(value: Boolean) = persist { preferences.setEnableFloatingBottomBar(value) }
    fun setEnableFloatingBottomBarBlur(value: Boolean) = persist { preferences.setEnableFloatingBottomBarBlur(value) }
    fun setEnableNavigationBadge(value: Boolean) = persist { preferences.setEnableNavigationBadge(value) }
    fun setEnableDynamicColor(value: Boolean) = persist { preferences.setEnableDynamicColor(value) }
    fun setThemeMode(value: ThemeMode) = persist { preferences.setThemeMode(value) }
    fun setPaletteStyle(value: MaterialTonalPaletteStyle) = persist { preferences.setPaletteStyle(value) }
    fun setColorSpec(value: MaterialColorSpec) = persist { preferences.setColorSpec(value) }

    fun showScaleDialog() {
        showScaleDialog.value = true
    }

    fun dismissScaleDialog() {
        showScaleDialog.value = false
    }

    fun setEnablePredictiveBack(value: Boolean) = persist { preferences.setEnablePredictiveBack(value) }
    fun setPageScale(value: Float) = persist { preferences.setPageScale(value) }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
