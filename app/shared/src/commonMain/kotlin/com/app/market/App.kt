package com.app.market

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.market.domain.model.install.InstallUserAction
import com.app.market.domain.model.preference.ThemeMode
import com.app.market.domain.repository.DownloadRepository
import com.app.market.domain.repository.InstallerPreferencesRepository
import com.app.market.domain.repository.ProfileRepository
import com.app.market.domain.repository.ThemePreferencesRepository
import com.app.market.domain.repository.UpdatePreferencesRepository
import com.app.market.domain.theme.MaterialDesignColors
import com.app.market.platform.ApplyPredictiveBackPreference
import com.app.market.platform.UiPlatform
import com.app.market.resources.Res
import com.app.market.resources.cancel
import com.app.market.resources.confirm
import com.app.market.resources.go_to_settings
import com.app.market.resources.install_failed_title
import com.app.market.resources.install_permission_message
import com.app.market.resources.install_permission_settings_unavailable
import com.app.market.resources.install_permission_title
import com.app.market.resources.installer_delta_fallback_toast
import com.app.market.ui.component.AppTextButton
import com.app.market.ui.navigation.AppNavigation
import com.app.market.ui.theme.LocalEnableBlur
import com.app.market.ui.theme.LocalEnableFloatingBottomBar
import com.app.market.ui.theme.LocalEnableFloatingBottomBarBlur
import com.app.market.ui.theme.LocalEnableNavigationBadge
import com.app.market.ui.theme.rememberAnimatedMiuixColors
import com.app.market.ui.util.LocalStripAppNameSubtitle
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import androidx.compose.ui.graphics.Color
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.LocalContentColor
import top.yukonga.miuix.kmp.theme.MiuixTheme
import com.app.market.ui.theme.toMiuixPaletteStyle
import com.app.market.ui.theme.toMiuixThemeColorSpec
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.window.WindowDialog

@Composable
fun App(
    externalDetailPackageName: String? = null,
    externalDetailQuery: String? = null,
    onExternalDetailConsumed: (String) -> Unit = {},
    externalSearchKeyword: String? = null,
    onExternalSearchConsumed: (String) -> Unit = {},
    externalOpenDownloads: Boolean = false,
    onExternalDownloadsConsumed: () -> Unit = {},
) {
    val profileStore = koinInject<ProfileRepository>()
    val downloads = koinInject<DownloadRepository>()
    val uiPlatform = koinInject<UiPlatform>()
    val updatePrefs = koinInject<UpdatePreferencesRepository>()
    val themePrefs = koinInject<ThemePreferencesRepository>()
    val enableBlur by themePrefs.enableBlur.collectAsStateWithLifecycle()
    val enableFloatingBottomBar by themePrefs.enableFloatingBottomBar.collectAsStateWithLifecycle()
    val enableFloatingBottomBarBlur by themePrefs.enableFloatingBottomBarBlur.collectAsStateWithLifecycle()
    val enableNavigationBadge by themePrefs.enableNavigationBadge.collectAsStateWithLifecycle()
    val enableDynamicColor by themePrefs.enableDynamicColor.collectAsStateWithLifecycle()
    val themeMode by themePrefs.themeMode.collectAsStateWithLifecycle()
    val paletteStyle by themePrefs.paletteStyle.collectAsStateWithLifecycle()
    val colorSpec by themePrefs.colorSpec.collectAsStateWithLifecycle()
    val enablePredictiveBack by themePrefs.enablePredictiveBack.collectAsStateWithLifecycle()
    val pageScale by themePrefs.pageScale.collectAsStateWithLifecycle()
    val stripAppNameSubtitle by updatePrefs.stripAppNameSubtitle.collectAsStateWithLifecycle()
    val pendingUserAction by downloads.pendingUserAction.collectAsStateWithLifecycle(
        minActiveState = Lifecycle.State.RESUMED,
    )
    val failureMessage = (pendingUserAction as? InstallUserAction.InstallationFailed)?.message
    var retainedFailureMessage by remember { mutableStateOf("") }
    LaunchedEffect(failureMessage) {
        if (failureMessage != null) retainedFailureMessage = failureMessage
    }
    val settingsUnavailableMessage = stringResource(Res.string.install_permission_settings_unavailable)
    val installerPrefs = koinInject<InstallerPreferencesRepository>()
    LaunchedEffect(Unit) {
        downloads.deltaFallbacks.collect { fallback ->
            if (!installerPrefs.deltaFallbackNoticeEnabled()) return@collect
            uiPlatform.showToast(
                getString(Res.string.installer_delta_fallback_toast, fallback.artifactName, fallback.reason)
            )
        }
    }
    LaunchedEffect(Unit) { runCatching { profileStore.syncFromServerIfDue() } }
    ApplyPredictiveBackPreference(enablePredictiveBack)
    // 动态取色开启时 keyColor = null：跟随壁纸（Android）或引擎默认种子（桌面）；
    // 关闭时走系统默认静态方案，不叠加任何预设主题色。
    // 调色板风格与颜色规范可配置；Spec2025 仅支持 TonalSpot/Neutral/Vibrant/Expressive，
    // 其余组合按官方规范自动降级为 Spec2021（resolveSpecAndStyle 纯函数保证）。
    val (effectiveSpec, effectiveStyle) = MaterialDesignColors.resolveSpecAndStyle(colorSpec, paletteStyle)
    val controller = remember(themeMode, enableDynamicColor, effectiveSpec, effectiveStyle) {
        ThemeController(
            colorSchemeMode = colorSchemeModeFor(themeMode, enableDynamicColor),
            keyColor = null,
            colorSpec = effectiveSpec.toMiuixThemeColorSpec(),
            paletteStyle = effectiveStyle.toMiuixPaletteStyle(),
        )
    }
    MiuixTheme(colors = rememberAnimatedMiuixColors(controller.currentColors())) {
        val systemDensity = LocalDensity.current
        val scaledDensity = remember(systemDensity, pageScale) {
            Density(systemDensity.density * pageScale, systemDensity.fontScale)
        }
        CompositionLocalProvider(
            LocalContentColor provides MiuixTheme.colorScheme.onBackground,
            LocalStripAppNameSubtitle provides stripAppNameSubtitle,
            LocalDensity provides scaledDensity,
            LocalEnableBlur provides enableBlur,
            LocalEnableFloatingBottomBar provides enableFloatingBottomBar,
            LocalEnableFloatingBottomBarBlur provides enableFloatingBottomBarBlur,
            LocalEnableNavigationBadge provides enableNavigationBadge,
        ) {
            AppNavigation(
                externalDetailPackageName = externalDetailPackageName,
                externalDetailQuery = externalDetailQuery,
                onExternalDetailConsumed = onExternalDetailConsumed,
                externalSearchKeyword = externalSearchKeyword,
                onExternalSearchConsumed = onExternalSearchConsumed,
                externalOpenDownloads = externalOpenDownloads,
                onExternalDownloadsConsumed = onExternalDownloadsConsumed,
            )
            InstallFailureDialog(
                show = failureMessage != null,
                message = failureMessage ?: retainedFailureMessage,
                onDismiss = downloads::consumePendingUserAction,
            )
            UnknownSourcesPermissionDialog(
                show = pendingUserAction == InstallUserAction.GrantUnknownSourcesPermission,
                onDismiss = downloads::consumePendingUserAction,
                onOpenSettings = {
                    downloads.consumePendingUserAction()
                    if (!uiPlatform.openUnknownSourcesSettings()) {
                        uiPlatform.showToast(settingsUnavailableMessage)
                    }
                },
            )
        }
    }
}

@Composable
private fun InstallFailureDialog(
    show: Boolean,
    message: String,
    onDismiss: () -> Unit,
) {
    WindowDialog(
        show = show,
        title = stringResource(Res.string.install_failed_title),
        onDismissRequest = onDismiss,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                Text(
                    text = message,
                    color = MiuixTheme.colorScheme.onSurface,
                )
            }
            AppTextButton(
                modifier = Modifier.fillMaxWidth(),
                text = stringResource(Res.string.confirm),
                colors = ButtonDefaults.textButtonColorsPrimary(),
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun UnknownSourcesPermissionDialog(
    show: Boolean,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    WindowDialog(
        show = show,
        title = stringResource(Res.string.install_permission_title),
        onDismissRequest = onDismiss,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(Res.string.install_permission_message),
                color = MiuixTheme.colorScheme.onSurface,
            )
            Row(modifier = Modifier.fillMaxWidth()) {
                AppTextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.cancel),
                    onClick = onDismiss,
                )
                Spacer(Modifier.width(16.dp))
                AppTextButton(
                    modifier = Modifier.weight(1f),
                    text = stringResource(Res.string.go_to_settings),
                    colors = ButtonDefaults.textButtonColorsPrimary(),
                    onClick = onOpenSettings,
                )
            }
        }
    }
}

/**
 * Maps the persisted appearance preferences to the Miuix [ColorSchemeMode].
 * [ThemeMode.SYSTEM] follows the OS light/dark setting (auto-detected by Miuix);
 * [ThemeMode.LIGHT]/[ThemeMode.DARK] force a fixed appearance. With [dynamicColor]
 * enabled the Monet counterparts are used, and on platforms where wallpaper colors
 * are unavailable the Miuix library degrades to a static baseline palette.
 */
internal fun colorSchemeModeFor(themeMode: ThemeMode, dynamicColor: Boolean): ColorSchemeMode = when (themeMode) {
    ThemeMode.SYSTEM -> if (dynamicColor) ColorSchemeMode.MonetSystem else ColorSchemeMode.System
    ThemeMode.LIGHT -> if (dynamicColor) ColorSchemeMode.MonetLight else ColorSchemeMode.Light
    ThemeMode.DARK -> if (dynamicColor) ColorSchemeMode.MonetDark else ColorSchemeMode.Dark
}
