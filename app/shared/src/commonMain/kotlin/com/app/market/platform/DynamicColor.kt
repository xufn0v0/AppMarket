package com.app.market.platform

/**
 * Whether wallpaper-derived dynamic colors (Android "Material You" / Monet) are available.
 * Android 12 (API 31) exposes the system tonal palettes; on older versions and on desktop
 * the toggle is hidden and the static color scheme is used.
 */
expect fun isDynamicColorSupported(): Boolean
