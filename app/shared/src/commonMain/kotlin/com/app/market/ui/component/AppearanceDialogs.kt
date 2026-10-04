package com.app.market.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.app.market.domain.model.preference.ThemeMode
import com.app.market.resources.Res
import com.app.market.resources.theme_appearance
import com.app.market.resources.theme_appearance_dark
import com.app.market.resources.theme_appearance_light
import com.app.market.resources.theme_appearance_system
import com.app.market.resources.theme_seed_color
import com.app.market.resources.theme_seed_follow_wallpaper
import org.jetbrains.compose.resources.stringResource
import top.yukonga.miuix.kmp.basic.RadioButton
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * Dialog for picking the app appearance. Selecting an option applies it immediately so the
 * animated theme transition is visible behind the dialog; dismissal needs no confirmation.
 */
@Composable
fun ThemeModeDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    modeProvider: () -> ThemeMode,
    onModeSelect: (ThemeMode) -> Unit,
) {
    OverlayDialog(
        show = show,
        title = stringResource(Res.string.theme_appearance),
        onDismissRequest = onDismissRequest,
    ) {
        RadioRow(
            selected = modeProvider() == ThemeMode.SYSTEM,
            label = stringResource(Res.string.theme_appearance_system),
            onClick = { onModeSelect(ThemeMode.SYSTEM) },
        )
        RadioRow(
            selected = modeProvider() == ThemeMode.LIGHT,
            label = stringResource(Res.string.theme_appearance_light),
            onClick = { onModeSelect(ThemeMode.LIGHT) },
        )
        RadioRow(
            selected = modeProvider() == ThemeMode.DARK,
            label = stringResource(Res.string.theme_appearance_dark),
            onClick = { onModeSelect(ThemeMode.DARK) },
        )
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}

/** Preset seed colors offered for Monet palette generation (Material You style palette). */
private val PresetSeedColors = listOf(
    0xFF6750A4.toInt(),
    0xFF006A60.toInt(),
    0xFF1565C0.toInt(),
    0xFF2E6B34.toInt(),
    0xFF8A5A00.toInt(),
    0xFFB3261E.toInt(),
    0xFF984061.toInt(),
    0xFF4A4458.toInt(),
)

/**
 * Dialog for choosing the Monet seed color: follow the wallpaper (null) or pick one of the
 * preset colors. Selections apply immediately for a live preview.
 */
@Composable
fun SeedColorDialog(
    show: Boolean,
    onDismissRequest: () -> Unit,
    seedProvider: () -> Int?,
    onSeedSelect: (Int?) -> Unit,
) {
    OverlayDialog(
        show = show,
        title = stringResource(Res.string.theme_seed_color),
        onDismissRequest = onDismissRequest,
    ) {
        RadioRow(
            selected = seedProvider() == null,
            label = stringResource(Res.string.theme_seed_follow_wallpaper),
            onClick = { onSeedSelect(null) },
        )
        Spacer(Modifier.padding(bottom = 12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PresetSeedColors.take(4).forEach { argb ->
                SeedSwatch(argb = argb, selected = seedProvider() == argb, onClick = { onSeedSelect(argb) })
            }
        }
        Spacer(Modifier.padding(bottom = 8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PresetSeedColors.drop(4).forEach { argb ->
                SeedSwatch(argb = argb, selected = seedProvider() == argb, onClick = { onSeedSelect(argb) })
            }
        }
        Spacer(Modifier.padding(bottom = 8.dp))
    }
}

@Composable
private fun RadioRow(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(text = label, color = MiuixTheme.colorScheme.onSurface)
    }
}

@Composable
private fun SeedSwatch(
    argb: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val color = Color(argb)
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                border = BorderStroke(
                    width = if (selected) 3.dp else 1.dp,
                    color = if (selected) MiuixTheme.colorScheme.onSurface else MiuixTheme.colorScheme.dividerLine,
                ),
                shape = CircleShape,
            )
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    )
}
