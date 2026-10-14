package com.example.myapplication.feature.settings.impl

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import com.example.myapplication.ui.platform.components.SettingsGroupRow

/**
 * The language entry of the appearance group: the same row and the same menu as the theme entry,
 * for the language the app is shown in.
 *
 * Picking one hands it to [AppLanguage], which sets it for the whole app and recreates the
 * activity, so that every string is then read in the new language.
 */
@Composable
internal fun LanguageEntry(
    isMenuExpanded: Boolean,
    onEntryClick: () -> Unit,
    onLanguageSelect: (AppLanguage) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box {
        SettingsGroupRow(
            icon = Icons.Outlined.Info,
            title = stringResource(id = R.string.appearance_language),
            onClick = onEntryClick,
            interactionSource = interactionSource,
            showRipple = false,
            trailingContent = {
                SettingsMenuValue(
                    text = AppLanguage.current().label(),
                    isExpanded = isMenuExpanded,
                    interactionSource = interactionSource,
                )
            },
        )

        // A menu unfolds from the anchor it is placed in: anchoring it to the trailing bottom
        // corner of the row makes it open towards the right edge of the screen, and keeping it
        // non-focusable lets the row keep receiving presses, exactly as the theme entry does.
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .size(size = 0.dp),
        ) {
            DropdownMenu(
                expanded = isMenuExpanded,
                onDismissRequest = {},
                properties = PopupProperties(focusable = false),
            ) {
                AppLanguage.entries.forEach { language ->
                    DropdownMenuItem(
                        text = { Text(text = language.label()) },
                        onClick = { onLanguageSelect(language) },
                    )
                }
            }
        }
    }
}

/**
 * What the language entry shows at its end.
 */
@Composable
@ReadOnlyComposable
private fun AppLanguage.label(): String = stringResource(
    id = when (this) {
        AppLanguage.SYSTEM -> R.string.appearance_language_system
        AppLanguage.ENGLISH -> R.string.appearance_language_english
        AppLanguage.CHINESE -> R.string.appearance_language_chinese
    },
)
