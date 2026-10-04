package com.app.market.domain.repository

import com.app.market.domain.model.preference.ThemeMode
import kotlinx.coroutines.flow.StateFlow

/** Persisted appearance and navigation preferences shared by all UI targets. */
interface ThemePreferencesRepository {
    val initialized: StateFlow<Boolean>
    val enableBlur: StateFlow<Boolean>
    val enableFloatingBottomBar: StateFlow<Boolean>
    val enableFloatingBottomBarBlur: StateFlow<Boolean>
    val enableNavigationBadge: StateFlow<Boolean>
    val enableDynamicColor: StateFlow<Boolean>
    val themeMode: StateFlow<ThemeMode>

    /** ARGB seed overriding wallpaper extraction for Monet palettes; null follows the wallpaper. */
    val monetSeedColor: StateFlow<Int?>
    val navRailExpanded: StateFlow<Boolean>
    val enablePredictiveBack: StateFlow<Boolean>
    val pageScale: StateFlow<Float>

    suspend fun setEnableBlur(value: Boolean)
    suspend fun setEnableFloatingBottomBar(value: Boolean)
    suspend fun setEnableFloatingBottomBarBlur(value: Boolean)
    suspend fun setEnableNavigationBadge(value: Boolean)
    suspend fun setEnableDynamicColor(value: Boolean)
    suspend fun setThemeMode(value: ThemeMode)
    suspend fun setMonetSeedColor(value: Int?)
    suspend fun setNavRailExpanded(value: Boolean)
    suspend fun setEnablePredictiveBack(value: Boolean)
    suspend fun setPageScale(value: Float)
}
