package com.app.market.viewmodel

import com.app.market.colorSchemeModeFor
import com.app.market.domain.model.preference.ThemeMode
import com.app.market.domain.repository.ThemePreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSettingsAppearanceTest {

    // --- colorSchemeModeFor: all six combinations ---

    @Test
    fun systemWithoutDynamicColorMapsToSystemMode() {
        assertEquals(ColorSchemeMode.System, colorSchemeModeFor(ThemeMode.SYSTEM, dynamicColor = false))
    }

    @Test
    fun systemWithDynamicColorMapsToMonetSystemMode() {
        assertEquals(ColorSchemeMode.MonetSystem, colorSchemeModeFor(ThemeMode.SYSTEM, dynamicColor = true))
    }

    @Test
    fun lightWithoutDynamicColorMapsToLightMode() {
        assertEquals(ColorSchemeMode.Light, colorSchemeModeFor(ThemeMode.LIGHT, dynamicColor = false))
    }

    @Test
    fun lightWithDynamicColorMapsToMonetLightMode() {
        assertEquals(ColorSchemeMode.MonetLight, colorSchemeModeFor(ThemeMode.LIGHT, dynamicColor = true))
    }

    @Test
    fun darkWithoutDynamicColorMapsToDarkMode() {
        assertEquals(ColorSchemeMode.Dark, colorSchemeModeFor(ThemeMode.DARK, dynamicColor = false))
    }

    @Test
    fun darkWithDynamicColorMapsToMonetDarkMode() {
        assertEquals(ColorSchemeMode.MonetDark, colorSchemeModeFor(ThemeMode.DARK, dynamicColor = true))
    }

    // --- preferences -> ui state ---

    @Test
    fun appearancePreferencesFlowIntoUiState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val preferences = FakeThemePreferences(
                dynamicColor = true,
                themeMode = ThemeMode.DARK,
                monetSeedColor = 0xFF1565C0.toInt(),
            )
            val viewModel = ThemeSettingsViewModel(preferences)

            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertTrue(state.enableDynamicColor)
            assertEquals(ThemeMode.DARK, state.themeMode)
            assertEquals(0xFF1565C0.toInt(), state.monetSeedColor)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun setThemeModePersistsAndUpdatesState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val preferences = FakeThemePreferences()
            val viewModel = ThemeSettingsViewModel(preferences)
            advanceUntilIdle()
            assertEquals(ThemeMode.SYSTEM, viewModel.uiState.value.themeMode)

            viewModel.setThemeMode(ThemeMode.LIGHT)
            advanceUntilIdle()

            assertEquals(ThemeMode.LIGHT, viewModel.uiState.value.themeMode)
            assertEquals(listOf(ThemeMode.LIGHT), preferences.themeModeWrites)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun setMonetSeedColorPersistsRoundTrip() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val preferences = FakeThemePreferences()
            val viewModel = ThemeSettingsViewModel(preferences)
            advanceUntilIdle()
            assertNull(viewModel.uiState.value.monetSeedColor)

            viewModel.setMonetSeedColor(0xFF006A60.toInt())
            advanceUntilIdle()

            assertEquals(0xFF006A60.toInt(), viewModel.uiState.value.monetSeedColor)

            viewModel.setMonetSeedColor(null)
            advanceUntilIdle()

            assertNull(viewModel.uiState.value.monetSeedColor)
            assertEquals(listOf(0xFF006A60.toInt(), null), preferences.seedWrites)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun setEnableDynamicColorPersistsAndUpdatesState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val preferences = FakeThemePreferences()
            val viewModel = ThemeSettingsViewModel(preferences)
            advanceUntilIdle()

            viewModel.setEnableDynamicColor(true)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.enableDynamicColor)
            assertEquals(listOf(true), preferences.dynamicColorWrites)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FakeThemePreferences(
    dynamicColor: Boolean = false,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    monetSeedColor: Int? = null,
) : ThemePreferencesRepository {
    override val initialized: StateFlow<Boolean> = MutableStateFlow(true)
    override val enableBlur = MutableStateFlow(false)
    override val enableFloatingBottomBar = MutableStateFlow(false)
    override val enableFloatingBottomBarBlur = MutableStateFlow(false)
    override val enableNavigationBadge = MutableStateFlow(true)
    override val enableDynamicColor = MutableStateFlow(dynamicColor)
    override val themeMode = MutableStateFlow(themeMode)
    override val monetSeedColor = MutableStateFlow(monetSeedColor)
    override val navRailExpanded = MutableStateFlow(false)
    override val enablePredictiveBack = MutableStateFlow(false)
    override val pageScale = MutableStateFlow(1f)

    val dynamicColorWrites = mutableListOf<Boolean>()
    val themeModeWrites = mutableListOf<ThemeMode>()
    val seedWrites = mutableListOf<Int?>()

    override suspend fun setEnableBlur(value: Boolean) = Unit
    override suspend fun setEnableFloatingBottomBar(value: Boolean) = Unit
    override suspend fun setEnableFloatingBottomBarBlur(value: Boolean) = Unit
    override suspend fun setEnableNavigationBadge(value: Boolean) = Unit

    override suspend fun setEnableDynamicColor(value: Boolean) {
        dynamicColorWrites += value
        enableDynamicColor.value = value
    }

    override suspend fun setThemeMode(value: ThemeMode) {
        themeModeWrites += value
        themeMode.value = value
    }

    override suspend fun setMonetSeedColor(value: Int?) {
        seedWrites += value
        monetSeedColor.value = value
    }

    override suspend fun setNavRailExpanded(value: Boolean) = Unit
    override suspend fun setEnablePredictiveBack(value: Boolean) = Unit
    override suspend fun setPageScale(value: Float) = Unit
}
