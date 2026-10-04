package com.app.market.data.store

import com.app.market.data.local.PreferencesDataSource
import com.app.market.data.local.preferences.ThemePreferenceKeys
import com.app.market.data.platform.ThemePlatformPreferences
import com.app.market.data.platform.debugLog
import com.app.market.domain.model.preference.ThemeMode
import com.app.market.domain.repository.ThemePreferencesRepository
import com.app.market.domain.theme.MonetColorDefaults
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal class ThemePreferencesRepositoryImpl(
    private val preferences: PreferencesDataSource,
    private val scope: CoroutineScope,
    private val platformPreferences: ThemePlatformPreferences,
) : ThemePreferencesRepository {
    private val _initialized = MutableStateFlow(false)
    override val initialized: StateFlow<Boolean> = _initialized.asStateFlow()
    private val _enableBlur = MutableStateFlow(ThemePreferenceKeys.EnableBlur.default)
    override val enableBlur: StateFlow<Boolean> = _enableBlur.asStateFlow()
    private val _enableFloatingBottomBar = MutableStateFlow(false)
    override val enableFloatingBottomBar: StateFlow<Boolean> = _enableFloatingBottomBar.asStateFlow()
    private val _enableFloatingBottomBarBlur = MutableStateFlow(false)
    override val enableFloatingBottomBarBlur: StateFlow<Boolean> = _enableFloatingBottomBarBlur.asStateFlow()
    private val _enableNavigationBadge = MutableStateFlow(ThemePreferenceKeys.EnableNavigationBadge.default)
    override val enableNavigationBadge: StateFlow<Boolean> = _enableNavigationBadge.asStateFlow()
    private val _enableDynamicColor = MutableStateFlow(ThemePreferenceKeys.EnableDynamicColor.default)
    override val enableDynamicColor: StateFlow<Boolean> = _enableDynamicColor.asStateFlow()
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    private val _monetSeedColor = MutableStateFlow<Int?>(null)
    override val monetSeedColor: StateFlow<Int?> = _monetSeedColor.asStateFlow()
    private val _navRailExpanded = MutableStateFlow(ThemePreferenceKeys.NavRailExpanded.default)
    override val navRailExpanded: StateFlow<Boolean> = _navRailExpanded.asStateFlow()
    private val _enablePredictiveBack = MutableStateFlow(false)
    override val enablePredictiveBack: StateFlow<Boolean> = _enablePredictiveBack.asStateFlow()
    private val _pageScale = MutableStateFlow(1f)
    override val pageScale: StateFlow<Float> = _pageScale.asStateFlow()

    init {
        val arrivals = listOf(
            observe("enableBlur", preferences.observe(ThemePreferenceKeys.EnableBlur)) { _enableBlur.value = it },
            observe("enableFloatingBottomBar", preferences.observe(ThemePreferenceKeys.EnableFloatingBottomBar)) {
                _enableFloatingBottomBar.value = it
            },
            observe("enableFloatingBottomBarBlur", preferences.observe(ThemePreferenceKeys.EnableFloatingBottomBarBlur)) {
                _enableFloatingBottomBarBlur.value = it
            },
            observe("enableNavigationBadge", preferences.observe(ThemePreferenceKeys.EnableNavigationBadge)) {
                _enableNavigationBadge.value = it
            },
            observe("enableDynamicColor", preferences.observe(ThemePreferenceKeys.EnableDynamicColor)) {
                _enableDynamicColor.value = it
            },
            observe("themeMode", preferences.observe(ThemePreferenceKeys.ThemeMode)) {
                _themeMode.value = ThemeMode.fromToken(it)
            },
            observe("monetSeedColor", preferences.observe(ThemePreferenceKeys.MonetSeedColor)) { raw ->
                // 非法 / 损坏的持久化值安全降级为 null（跟随壁纸 / 引擎默认），绝不向 UI 抛异常
                _monetSeedColor.value = MonetColorDefaults.normalizeSeed(MonetColorDefaults.parseStoredSeed(raw))
            },
            observe("navRailExpanded", preferences.observe(ThemePreferenceKeys.NavRailExpanded)) {
                _navRailExpanded.value = it
            },
            observe("enablePredictiveBack", preferences.observe(ThemePreferenceKeys.EnablePredictiveBack)) {
                _enablePredictiveBack.value = it
                platformPreferences.setPredictiveBackEnabled(it)
            },
            observe("pageScale", preferences.observe(ThemePreferenceKeys.PageScale)) {
                _pageScale.value = it?.toFloatOrNull()?.coerceIn(0.8f, 1.1f) ?: 1f
            },
        )
        scope.launch {
            arrivals.forEach { it.await() }
            _initialized.value = true
        }
    }

    private fun <T> observe(
        name: String,
        flow: Flow<T>,
        update: (T) -> Unit,
    ): CompletableDeferred<Unit> {
        val firstValue = CompletableDeferred<Unit>()
        scope.launch {
            try {
                flow.collect {
                    update(it)
                    firstValue.complete(Unit)
                }
            } catch (error: Throwable) {
                if (error is CancellationException) throw error
                debugLog("ThemePreferencesRepository") { "observe $name failed: $error" }
            } finally {
                firstValue.complete(Unit)
            }
        }
        return firstValue
    }

    override suspend fun setEnableBlur(value: Boolean) = preferences.put(ThemePreferenceKeys.EnableBlur, value)
    override suspend fun setEnableFloatingBottomBar(value: Boolean) = preferences.put(ThemePreferenceKeys.EnableFloatingBottomBar, value)

    override suspend fun setEnableFloatingBottomBarBlur(value: Boolean) =
        preferences.put(ThemePreferenceKeys.EnableFloatingBottomBarBlur, value)

    override suspend fun setEnableNavigationBadge(value: Boolean) = preferences.put(ThemePreferenceKeys.EnableNavigationBadge, value)
    override suspend fun setEnableDynamicColor(value: Boolean) = preferences.put(ThemePreferenceKeys.EnableDynamicColor, value)

    override suspend fun setThemeMode(value: ThemeMode) = preferences.put(ThemePreferenceKeys.ThemeMode, value.token)

    override suspend fun setMonetSeedColor(value: Int?) =
        preferences.put(
            ThemePreferenceKeys.MonetSeedColor,
            MonetColorDefaults.formatStoredSeed(MonetColorDefaults.normalizeSeed(value)),
        )

    override suspend fun setNavRailExpanded(value: Boolean) = preferences.put(ThemePreferenceKeys.NavRailExpanded, value)
    override suspend fun setEnablePredictiveBack(value: Boolean) {
        platformPreferences.setPredictiveBackEnabled(value)
        preferences.put(ThemePreferenceKeys.EnablePredictiveBack, value)
    }

    override suspend fun setPageScale(value: Float) =
        preferences.put(ThemePreferenceKeys.PageScale, value.coerceIn(0.8f, 1.1f).toString())
}
