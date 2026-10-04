package com.app.market.domain.repository

import com.app.market.domain.model.preference.ThemeMode
import com.app.market.domain.theme.MaterialDesignColors.MaterialColorSpec
import com.app.market.domain.theme.MaterialDesignColors.MaterialTonalPaletteStyle
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

    /** Monet 动态取色使用的调色板风格（默认 TonalSpot）。 */
    val paletteStyle: StateFlow<MaterialTonalPaletteStyle>

    /** Monet 动态取色使用的颜色规范（默认 Spec2021）。 */
    val colorSpec: StateFlow<MaterialColorSpec>

    val navRailExpanded: StateFlow<Boolean>
    val enablePredictiveBack: StateFlow<Boolean>
    val pageScale: StateFlow<Float>

    suspend fun setEnableBlur(value: Boolean)
    suspend fun setEnableFloatingBottomBar(value: Boolean)
    suspend fun setEnableFloatingBottomBarBlur(value: Boolean)
    suspend fun setEnableNavigationBadge(value: Boolean)
    suspend fun setEnableDynamicColor(value: Boolean)
    suspend fun setThemeMode(value: ThemeMode)
    suspend fun setPaletteStyle(value: MaterialTonalPaletteStyle)
    suspend fun setColorSpec(value: MaterialColorSpec)
    suspend fun setNavRailExpanded(value: Boolean)
    suspend fun setEnablePredictiveBack(value: Boolean)
    suspend fun setPageScale(value: Float)
}
