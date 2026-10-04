package com.app.market.data.local.preferences

import com.app.market.data.local.BooleanPreferenceKey
import com.app.market.data.local.StringPreferenceKey

object ThemePreferenceKeys {
    private const val NS = "theme_preferences"
    val EnableBlur = BooleanPreferenceKey(NS, "enable_blur")
    val EnableFloatingBottomBar = BooleanPreferenceKey(NS, "enable_floating_bottom_bar")
    val EnableFloatingBottomBarBlur = BooleanPreferenceKey(NS, "enable_floating_bottom_bar_blur")
    val EnableNavigationBadge = BooleanPreferenceKey(NS, "enable_navigation_badge", true)
    val EnableDynamicColor = BooleanPreferenceKey(NS, "enable_dynamic_color")
    val ThemeMode = StringPreferenceKey(NS, "theme_mode")
    /** 调色板风格（specName，如 "TonalSpot"），空串表示使用引擎默认 TonalSpot。 */
    val PaletteStyle = StringPreferenceKey(NS, "palette_style")
    /** 颜色规范版本（specName，如 "Spec2021"/"Spec2025"），空串表示使用默认 Spec2021。 */
    val ColorSpec = StringPreferenceKey(NS, "color_spec")
    val NavRailExpanded = BooleanPreferenceKey(NS, "nav_rail_expanded")
    val EnablePredictiveBack = BooleanPreferenceKey(NS, "enable_predictive_back")
    val PageScale = StringPreferenceKey(NS, "page_scale")
}
