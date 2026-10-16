package com.example.myapplication.feature.settings.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.ui.platform.components.ImmersiveTopBar
import com.example.myapplication.ui.platform.components.immersiveTopBarContentPadding
import com.example.myapplication.ui.platform.components.mascot.JellyBobView
import com.example.myapplication.ui.platform.theme.MyApplicationTheme

/**
 * The side of the square the mascot is drawn in at the top of the screen.
 */
private val AboutMascotSize: Dp = 170.dp

/**
 * The gap between the mascot, the words under it and the panel under those.
 */
private val AboutSpacing: Dp = 24.dp

/**
 * The gap between two of the lines the screen writes itself.
 */
private val AboutTextSpacing: Dp = 10.dp

/**
 * The extra space between the floating top bar and the mascot, so that the mascot does not sit
 * directly against the floating buttons.
 */
private val AboutContentTopSpacing: Dp = 24.dp

/**
 * The horizontal padding of the lines, so that they stay clear of the edges on a narrow screen.
 */
private val AboutTextPadding: Dp = 24.dp

/**
 * What the app is: the mascot at the top, the name of the app and what it does under it, the entry
 * that looks for a newer build of it, and who it is by at the foot.
 *
 * Only the entry that checks for a newer build sits in a panel. The mascot, the name and the words
 * are drawn on the page itself, and none of them carries a label.
 *
 * The update check is the one the settings keep their state for — the check, the download and the
 * dialog all live there — so this screen uses the settings view model rather than keeping a second
 * copy of any of it. This screen is only another way into the same check.
 */
@Composable
fun AboutScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(
                    paddingValues = immersiveTopBarContentPadding(
                        extraTop = AboutContentTopSpacing,
                    ),
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AboutMascot()
            Spacer(modifier = Modifier.height(height = AboutSpacing))
            AboutWords()
            Spacer(modifier = Modifier.height(height = AboutSpacing))
            UpdatePanel(
                state = state.appUpdate,
                onUpdateClick = { viewModel.trySendAction(SettingsAction.CheckForUpdates) },
            )

            // The foot of the screen sits at the bottom of it, not directly under the words.
            Spacer(modifier = Modifier.weight(weight = 1f))
            AboutFooter(version = state.appUpdate.currentVersion)
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
 * The mascot of the app, centred at the top of the screen with nothing around it. It is a view of
 * its own, so it keeps the animation and the finger tracking it has where it is shown in its
 * states.
 */
@Composable
private fun AboutMascot() {
    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { context -> JellyBobView(context) },
            modifier = Modifier.size(size = AboutMascotSize),
        )
    }
}

/**
 * The name of the app and what it does, centred on the page, without a panel around either of them.
 */
@Composable
private fun AboutWords() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AboutTextPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = AboutTextSpacing),
    ) {
        Text(
            text = stringResource(id = R.string.about_app_name),
            style = MyApplicationTheme.typography.titleLarge,
            color = MyApplicationTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.about_description),
            style = MyApplicationTheme.typography.bodyMedium,
            color = MyApplicationTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/**
 * The foot of the screen: who the app is by, and the version of the build that is installed.
 */
@Composable
private fun AboutFooter(version: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AboutTextPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = AboutTextSpacing),
    ) {
        Text(
            text = stringResource(id = R.string.about_copyright),
            style = MyApplicationTheme.typography.bodySmall,
            color = MyApplicationTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(id = R.string.about_version, version),
            style = MyApplicationTheme.typography.bodySmall,
            color = MyApplicationTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
