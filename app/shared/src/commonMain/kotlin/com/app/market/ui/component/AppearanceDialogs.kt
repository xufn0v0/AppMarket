package com.app.market.ui.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import top.yukonga.miuix.kmp.basic.Surface
import top.yukonga.miuix.kmp.overlay.OverlayDialog
import top.yukonga.miuix.kmp.preference.RadioButtonPreference
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.Locale

/**
 * Horizontal padding is left to the dialog's own insets (24dp); preference rows keep their
 * vertical rhythm only, avoiding the double horizontal indent of nesting BasicComponent
 * (16dp inside margin) inside the dialog.
 */
private val SelectionRowMargin = PaddingValues(start = 0.dp, top = 12.dp, end = 0.dp, bottom = 12.dp)

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
        SelectionRow(
            label = stringResource(Res.string.theme_appearance_system),
            selected = modeProvider() == ThemeMode.SYSTEM,
            onClick = { onModeSelect(ThemeMode.SYSTEM) },
        )
        SelectionRow(
            label = stringResource(Res.string.theme_appearance_light),
            selected = modeProvider() == ThemeMode.LIGHT,
            onClick = { onModeSelect(ThemeMode.LIGHT) },
        )
        SelectionRow(
            label = stringResource(Res.string.theme_appearance_dark),
            selected = modeProvider() == ThemeMode.DARK,
            onClick = { onModeSelect(ThemeMode.DARK) },
        )
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
        SelectionRow(
            label = stringResource(Res.string.theme_seed_follow_wallpaper),
            selected = seedProvider() == null,
            onClick = { onSeedSelect(null) },
        )
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PresetSeedColors.take(4).forEach { argb ->
                SeedSwatch(argb = argb, selected = seedProvider() == argb, onClick = { onSeedSelect(argb) })
            }
        }
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            PresetSeedColors.drop(4).forEach { argb ->
                SeedSwatch(argb = argb, selected = seedProvider() == argb, onClick = { onSeedSelect(argb) })
            }
        }
    }
}

/** Shared single-choice row: MUIX [RadioButtonPreference] with dialog-aligned insets. */
@Composable
private fun SelectionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    RadioButtonPreference(
        title = label,
        selected = selected,
        onClick = onClick,
        insideMargin = SelectionRowMargin,
    )
}

@Composable
private fun SeedSwatch(
    argb: Int,
    selected: Boolean,
    onClick: () -> Unit) {
    val hexLabel = "#" + (argb and 0xFFFFFF).toString(16).uppercase(Locale.US).padStart(6, '0')
    Surface(
        onClick = onClick,
        modifier = Modifier
            .size(42.dp)
            .semantics {
                role = Role.RadioButton
                this.selected = selected
                contentDescription = hexLabel
            },
        shape = CircleShape,
        color = Color(argb),
        border = BorderStroke(
            width = if (selected) 3.dp else 1.dp,
            color = if (selected) MiuixTheme.colorScheme.primary else MiuixTheme.colorScheme.dividerLine,
        ),
    ) {
        // MUIX Surface supplies the Miuix press indication; content stays empty.
    }
}
