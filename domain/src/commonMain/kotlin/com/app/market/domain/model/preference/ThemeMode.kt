package com.app.market.domain.model.preference

/** User-selected app appearance. [SYSTEM] follows the OS light/dark setting. */
enum class ThemeMode(val token: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        /** Resolve from a persisted [token]; unknown/null falls back to [SYSTEM]. */
        fun fromToken(token: String?): ThemeMode = entries.firstOrNull { it.token == token } ?: SYSTEM
    }
}
