package com.app.market.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.market.domain.model.preference.ThemeMode
import com.app.market.domain.repository.ThemePreferencesRepository
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
    val monetSeedColor: Int? = null,
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
        val monetSeedColor: Int?,
    )

    private val appearance = combine(
        preferences.enableBlur,
        preferences.enableFloatingBottomBar,
        preferences.enableDynamicColor,
        preferences.themeMode,
        preferences.monetSeedColor,
    ) { blur, floating, dynamicColor, themeMode, seedColor ->
        AppearancePreferences(blur, floating, dynamicColor, themeMode, seedColor)
    }

    val uiState: StateFlow<ThemeSettingsUiState> = combine(
        appearance,
        preferences.enableFloatingBottomBarBlur,
        preferences.enableNavigationBadge,
        preferences.enablePredictiveBack,
        preferences.pageScale,
    ) { appearance, glass, badge, predictiveBack, scale ->
        ThemeSettingsUiState(
            enableBlur = appearance.enableBlur,
            enableFloatingBottomBar = appearance.enableFloatingBottomBar,
            enableDynamicColor = appearance.enableDynamicColor,
            themeMode = appearance.themeMode,
            monetSeedColor = appearance.monetSeedColor,
            enableFloatingBottomBarBlur = glass,
            enableNavigationBadge = badge,
            enablePredictiveBack = predictiveBack,
            pageScale = scale,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeSettingsUiState())

    fun setEnableBlur(value: Boolean) = persist { preferences.setEnableBlur(value) }
    fun setEnableFloatingBottomBar(value: Boolean) = persist { preferences.setEnableFloatingBottomBar(value) }
    fun setEnableFloatingBottomBarBlur(value: Boolean) = persist { preferences.setEnableFloatingBottomBarBlur(value) }
    fun setEnableNavigationBadge(value: Boolean) = persist { preferences.setEnableNavigationBadge(value) }
    fun setEnableDynamicColor(value: Boolean) = persist { preferences.setEnableDynamicColor(value) }
    fun setThemeMode(value: ThemeMode) = persist { preferences.setThemeMode(value) }
    fun setMonetSeedColor(value: Int?) = persist { preferences.setMonetSeedColor(value) }
    fun setEnablePredictiveBack(value: Boolean) = persist { preferences.setEnablePredictiveBack(value) }
    fun setPageScale(value: Float) = persist { preferences.setPageScale(value) }

    private fun persist(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }
}
