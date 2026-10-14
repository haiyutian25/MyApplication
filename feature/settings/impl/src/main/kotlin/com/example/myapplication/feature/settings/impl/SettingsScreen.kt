package com.example.myapplication.feature.settings.impl

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.outlined.Face
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.PopupProperties
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.core.model.AppTheme
import com.example.myapplication.ui.platform.components.DismissOnScreenPause
import com.example.myapplication.ui.platform.components.ImmersiveTopBar
import com.example.myapplication.ui.platform.components.ImmersiveTopBarIconButton
import com.example.myapplication.ui.platform.components.SettingsGroup
import com.example.myapplication.ui.platform.components.SettingsGroupDivider
import com.example.myapplication.ui.platform.components.SettingsGroupRow
import com.example.myapplication.ui.platform.components.immersiveTopBarContentPadding
import com.example.myapplication.ui.platform.theme.MyApplicationTheme

/**
 * The vertical gap between two groups of settings, so that the page background separates them.
 */
private val SettingsGroupSpacing: Dp = 22.dp

/**
 * The extra space between the floating top bar and the first group of settings, so that the first
 * card does not sit directly against the floating buttons.
 */
private val SettingsContentTopSpacing: Dp = 32.dp

/**
 * The rotation of the chevron of a menu row while the menu is closed.
 */
private const val CHEVRON_COLLAPSED_ROTATION = 0f

/**
 * The rotation of the chevron of a menu row while the menu is open: the chevron points up.
 */
private const val CHEVRON_EXPANDED_ROTATION = 180f

/**
 * The settings screen, which provides the entry points into the app's settings.
 *
 * The entries are laid out as groups of labelled raised cards rather than as a flat list: one group
 * holds the entries of one topic, and the gap between the groups exposes the page background. The
 * screen uses an immersive top layout as well, so the cards scroll all the way to the top of the
 * screen and pass beneath the floating back button.
 *
 * A setting that only has a handful of mutually exclusive values is changed in place, from a menu
 * that opens on its own row, rather than on a screen of its own.
 */
@Suppress("LongMethod") // One layout: the groups of entries, and the dialog they open.
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    onNavigateToMascotsScreen: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()
    var isThemeMenuExpanded by rememberSaveable { mutableStateOf(false) }
    var isLanguageMenuExpanded by rememberSaveable { mutableStateOf(false) }
    val outsidePressSource = remember { MutableInteractionSource() }

    // The menu lives in a pop-up window of its own, so it would otherwise outlive this screen for
    // as long as the navigation transition keeps the screen composed, floating over the screen
    // being revealed. Navigation 3 gives every destination its own lifecycle, so closing the menu
    // when the screen pauses dismisses it before it can leak into the next screen.
    DismissOnScreenPause(isExpanded = isThemeMenuExpanded) { isThemeMenuExpanded = false }
    DismissOnScreenPause(isExpanded = isLanguageMenuExpanded) { isLanguageMenuExpanded = false }

    // While the theme menu is open, a press anywhere outside the row closes it. The row handles its
    // own presses because it is nested deeper: Compose delivers a press to the innermost handler
    // first and stops there once it is consumed, so this layer only ever receives the presses that
    // land on the page behind the panel. It draws no ripple, so the feedback stays on the chevron.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(
                if (isThemeMenuExpanded || isLanguageMenuExpanded) {
                    Modifier.clickable(
                        interactionSource = outsidePressSource,
                        indication = null,
                        onClick = {
                            isThemeMenuExpanded = false
                            isLanguageMenuExpanded = false
                        },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = immersiveTopBarContentPadding(extraTop = SettingsContentTopSpacing),
            verticalArrangement = Arrangement.spacedBy(space = SettingsGroupSpacing),
        ) {
            item {
                SettingsGroup(title = stringResource(id = R.string.settings_appearance)) {
                    ThemeEntry(
                        appTheme = state.appTheme,
                        isMenuExpanded = isThemeMenuExpanded,
                        onEntryClick = { isThemeMenuExpanded = !isThemeMenuExpanded },
                        onThemeSelect = { appTheme ->
                            viewModel.trySendAction(
                                SettingsAction.AppThemeSelect(appTheme = appTheme),
                            )
                            isThemeMenuExpanded = false
                        },
                    )
                    SettingsGroupDivider()
                    LanguageEntry(
                        appLanguage = state.appLanguage,
                        isMenuExpanded = isLanguageMenuExpanded,
                        onEntryClick = { isLanguageMenuExpanded = !isLanguageMenuExpanded },
                        onLanguageSelect = { language ->
                            viewModel.trySendAction(
                                SettingsAction.AppLanguageSelect(appLanguage = language),
                            )
                            isLanguageMenuExpanded = false
                        },
                    )
                    SettingsGroupDivider()
                    NavigateEntry(
                        icon = Icons.Outlined.Face,
                        title = stringResource(id = R.string.settings_mascots),
                        onClick = onNavigateToMascotsScreen,
                    )
                }
            }

            updateItem(
                state = state.appUpdate,
                onUpdateClick = { viewModel.trySendAction(SettingsAction.CheckForUpdates) },
            )
        }

        ImmersiveTopBar(
            leadingContent = { NavigateBackButton(onClick = onNavigateBack) },
        )

        UpdateDialog(
            state = state.appUpdate,
            onDownload = { viewModel.trySendAction(SettingsAction.DownloadAndInstallUpdate) },
            onOpenRelease = { viewModel.trySendAction(SettingsAction.OpenReleasePage) },
            onDismiss = { viewModel.trySendAction(SettingsAction.DismissUpdate) },
        )
    }
}

/**
 * The theme entry of the appearance group: a row that shows the current theme and opens the menu
 * that changes it.
 *
 * The row is only a large tap target; the visible press feedback belongs to the chevron at its end,
 * so the panel itself does not flash when the menu is opened.
 */
@Composable
private fun ThemeEntry(
    appTheme: AppTheme,
    isMenuExpanded: Boolean,
    onEntryClick: () -> Unit,
    onThemeSelect: (AppTheme) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }

    Box {
        SettingsGroupRow(
            icon = Icons.Outlined.Settings,
            title = stringResource(id = R.string.appearance_theme),
            onClick = onEntryClick,
            interactionSource = interactionSource,
            showRipple = false,
            trailingContent = {
                SettingsMenuValue(
                    text = appTheme.label(),
                    isExpanded = isMenuExpanded,
                    interactionSource = interactionSource,
                )
            },
        )

        // A menu unfolds from the anchor it is placed in. Anchoring it to the trailing bottom
        // corner of the row makes it open towards the right edge of the screen, not the left.
        Box(
            modifier = Modifier
                .align(alignment = Alignment.BottomEnd)
                .size(size = 0.dp),
        ) {
            DropdownMenu(
                expanded = isMenuExpanded,
                // A focusable menu puts an invisible full-screen layer over the screen to catch
                // outside presses, which would swallow the presses on the row itself and leave its
                // chevron without a ripple. Keeping the menu non-focusable lets the row keep
                // receiving presses, so it can toggle the menu closed the same way it opens it.
                //
                // Closing is therefore driven by the row alone. An empty dismiss handler keeps the
                // pop-up from writing to the same state a second time during the same press, which
                // would turn one tap into a close followed by an immediate reopen.
                onDismissRequest = {},
                properties = PopupProperties(focusable = false),
            ) {
                AppTheme.entries.forEach { theme ->
                    DropdownMenuItem(
                        text = { Text(text = theme.label()) },
                        onClick = { onThemeSelect(theme) },
                    )
                }
            }
        }
    }
}

/**
 * An entry that opens a screen of its own: the same row as the rest of its group, with an arrow at
 * its end that shows that it leads somewhere rather than expanding in place.
 */
@Composable
private fun NavigateEntry(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
) {
    SettingsGroupRow(
        icon = icon,
        title = title,
        onClick = onClick,
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MyApplicationTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

@Composable
private fun NavigateBackButton(onClick: () -> Unit) {
    ImmersiveTopBarIconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(id = R.string.settings_navigate_back),
        )
    }
}

/**
 * The trailing content of a row that opens a menu: the current value of the setting, followed by a
 * chevron that points down while the menu is closed and rotates to point up while it is open.
 *
 * The chevron is the only part that reacts to a press. It observes the interaction source of the
 * row through `Modifier.indication` and is clipped to a circle, so the ripple is a circle around
 * the icon itself while neither the value next to it nor the panel behind them draws one. The
 * press itself is still handled by the row, which keeps the whole panel clickable.
 */
@Composable
internal fun SettingsMenuValue(
    text: String,
    isExpanded: Boolean,
    interactionSource: MutableInteractionSource,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) CHEVRON_EXPANDED_ROTATION else CHEVRON_COLLAPSED_ROTATION,
        label = "menuChevronRotation",
    )

    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = text,
            style = MyApplicationTheme.typography.bodyMedium,
            color = MyApplicationTheme.colorScheme.onSurfaceVariant,
        )
        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            modifier = Modifier
                .padding(start = 4.dp)
                .clip(shape = CircleShape)
                .indication(interactionSource = interactionSource, indication = ripple())
                .rotate(degrees = rotation),
        )
    }
}

@Composable
@ReadOnlyComposable
private fun AppTheme.label(): String = stringResource(
    id = when (this) {
        AppTheme.DEFAULT -> R.string.appearance_theme_system
        AppTheme.LIGHT -> R.string.appearance_theme_light
        AppTheme.DARK -> R.string.appearance_theme_dark
    },
)

/**
 * The language entry lives in its own file: it is the same row and menu as the theme entry, and
 * keeping it apart keeps this file to the screen itself.
 */

/**
 * The group of the screen that holds the entry that checks for a newer build of the app.
 */
private fun LazyListScope.updateItem(
    state: AppUpdateUiState,
    onUpdateClick: () -> Unit,
) {
    item {
        SettingsGroup(title = stringResource(id = R.string.settings_about)) {
            UpdateEntry(state = state, onClick = onUpdateClick)
        }
    }
}

/**
 * The entry that checks the published releases of the app for a newer build and, once one is found,
 * downloads it and hands it to the system installer.
 *
 * A single row does both: its first press runs the check, and a press after a newer build was found
 * downloads it. What the row shows at its end follows the same state, so the row says what the next
 * press will do.
 */
@Composable
private fun UpdateEntry(
    state: AppUpdateUiState,
    onClick: () -> Unit,
) {
    SettingsGroupRow(
        icon = Icons.Outlined.Info,
        title = stringResource(id = R.string.settings_check_updates),
        onClick = onClick,
        trailingContent = {
            Text(
                text = state.entryLabel(),
                style = MyApplicationTheme.typography.bodyMedium,
                color = MyApplicationTheme.colorScheme.onSurfaceVariant,
            )
        },
    )
}

/**
 * What the update entry shows at its end: the version of the installed app, or that the check its
 * last press started is still running.
 */
@Composable
@ReadOnlyComposable
private fun AppUpdateUiState.entryLabel(): String = if (isChecking) {
    stringResource(id = R.string.update_checking)
} else {
    "v$currentVersion"
}

/**
 * The dialog the check opens, which is where the update is offered: it shows the release notes of
 * the newer build and only downloads it once the user asks for it, or says that the installed build
 * is the newest one, or reports why the check or the download failed.
 */
@Suppress("LongMethod") // The dialog is one layout: what it says, the progress and its actions.
@Composable
private fun UpdateDialog(
    state: AppUpdateUiState,
    onDownload: () -> Unit,
    onOpenRelease: () -> Unit,
    onDismiss: () -> Unit,
) {
    val info = state.info
    val title: String
    val message: String
    when {
        state.errorMessage != null -> {
            title = stringResource(id = R.string.update_failed_title)
            message = stringResource(id = R.string.update_failed_message, state.errorMessage)
        }
        info == null -> return
        info.hasUpdate -> {
            title = stringResource(id = R.string.update_available_title, info.latestVersion)
            message = info.releaseNotes.ifBlank { info.releaseTitle }
        }
        else -> {
            title = stringResource(id = R.string.update_up_to_date_title)
            message = stringResource(id = R.string.update_up_to_date_message, info.currentVersion)
        }
    }

    val canDownload = info != null && info.hasUpdate && info.apkDownloadUrl != null

    AlertDialog(
        onDismissRequest = onDismiss,
        // The dialog belongs to the page it is opened from, so it takes the page background rather
        // than the container colour Material picks for dialogs (surfaceContainerHigh), and no
        // tonal elevation so that the colour stays exactly the one the pages use.
        containerColor = MyApplicationTheme.colorScheme.background,
        tonalElevation = 0.dp,
        title = { Text(text = title) },
        text = {
            Column {
                Text(text = message)
                if (state.isDownloading) {
                    LinearProgressIndicator(
                        progress = { state.downloadProgress ?: 0f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                    )
                    Text(
                        text = stringResource(id = R.string.update_downloading),
                        style = MyApplicationTheme.typography.bodySmall,
                        color = MyApplicationTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
        },
        confirmButton = {
            if (canDownload) {
                TextButton(
                    onClick = onDownload,
                    enabled = !state.isDownloading,
                ) {
                    Text(text = stringResource(id = R.string.update_download))
                }
            } else {
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.update_dismiss))
                }
            }
        },
        dismissButton = {
            if (canDownload) {
                TextButton(onClick = onOpenRelease) {
                    Text(text = stringResource(id = R.string.update_open_release))
                }
                TextButton(onClick = onDismiss) {
                    Text(text = stringResource(id = R.string.update_dismiss))
                }
            }
        },
    )
}
