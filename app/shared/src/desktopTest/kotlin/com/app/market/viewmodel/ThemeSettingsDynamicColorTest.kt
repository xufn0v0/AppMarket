package com.app.market.viewmodel

import com.app.market.colorSchemeModeFor
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeSettingsDynamicColorTest {

    @Test
    fun disabledDynamicColorMapsToSystemMode() {
        assertEquals(ColorSchemeMode.System, colorSchemeModeFor(dynamicColor = false))
    }

    @Test
    fun enabledDynamicColorMapsToMonetSystemMode() {
        assertEquals(ColorSchemeMode.MonetSystem, colorSchemeModeFor(dynamicColor = true))
    }

    @Test
    fun dynamicColorPreferenceFlowsIntoUiState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val preferences = FakeThemePreferences(dynamicColor = true)
            val viewModel = ThemeSettingsViewModel(preferences)

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.enableDynamicColor)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun setEnableDynamicColorPersistsAndUpdatesState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val preferences = FakeThemePreferences(dynamicColor = false)
            val viewModel = ThemeSettingsViewModel(preferences)
            advanceUntilIdle()
            assertFalse(viewModel.uiState.value.enableDynamicColor)

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
    dynamicColor: Boolean,
) : ThemePreferencesRepository {
    override val initialized: StateFlow<Boolean> = MutableStateFlow(true)
    override val enableBlur = MutableStateFlow(false)
    override val enableFloatingBottomBar = MutableStateFlow(false)
    override val enableFloatingBottomBarBlur = MutableStateFlow(false)
    override val enableNavigationBadge = MutableStateFlow(true)
    override val enableDynamicColor = MutableStateFlow(dynamicColor)
    override val navRailExpanded = MutableStateFlow(false)
    override val enablePredictiveBack = MutableStateFlow(false)
    override val pageScale = MutableStateFlow(1f)

    val dynamicColorWrites = mutableListOf<Boolean>()

    override suspend fun setEnableBlur(value: Boolean) = Unit
    override suspend fun setEnableFloatingBottomBar(value: Boolean) = Unit
    override suspend fun setEnableFloatingBottomBarBlur(value: Boolean) = Unit
    override suspend fun setEnableNavigationBadge(value: Boolean) = Unit

    override suspend fun setEnableDynamicColor(value: Boolean) {
        dynamicColorWrites += value
        enableDynamicColor.value = value
    }

    override suspend fun setNavRailExpanded(value: Boolean) = Unit
    override suspend fun setEnablePredictiveBack(value: Boolean) = Unit
    override suspend fun setPageScale(value: Float) = Unit
}
