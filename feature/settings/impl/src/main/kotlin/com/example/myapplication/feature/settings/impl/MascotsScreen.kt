package com.example.myapplication.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.myapplication.ui.platform.components.ImmersiveTopBar
import com.example.myapplication.ui.platform.components.ImmersiveTopBarIconButton
import com.example.myapplication.ui.platform.components.SettingsGroup
import com.example.myapplication.ui.platform.components.immersiveTopBarContentPadding
import com.example.myapplication.ui.platform.components.mascot.JasmineSpiritView
import com.example.myapplication.ui.platform.components.mascot.JellyBobView
import com.example.myapplication.ui.platform.theme.MyApplicationTheme

/**
 * The vertical gap between two groups of the screen, so that the page background separates them.
 */
private val MascotsGroupSpacing: Dp = 22.dp

/**
 * The extra space between the floating top bar and the first group, so that the first card does not
 * sit directly against the floating buttons.
 */
private val MascotsContentTopSpacing: Dp = 32.dp

/**
 * The height of the area that a mascot is drawn on.
 */
private val MascotsStageHeight: Dp = 200.dp

/**
 * The side of the square that the jelly mascot is drawn in. It places its antennae and its shadow
 * around its body, so it needs a square area of its own to keep them inside the stage.
 */
private val MascotsJellyBobSize: Dp = 180.dp

/**
 * The side of the square that the jasmine spirit is drawn in. It places its halo, its wings and its
 * shadow around its body, so it needs a square area of its own to keep them inside the stage.
 */
private val MascotsJasmineSize: Dp = 180.dp

/**
 * The space between a mascot and the edge of its stage.
 */
private val MascotsStagePadding: Dp = 16.dp

/**
 * The space around the caption below a stage.
 */
private val MascotsCaptionPadding: Dp = 16.dp

/**
 * The screen that shows the mascot of the app in each of the states it is drawn in.
 *
 * The mascots are painted, not loaded: they are the same composables the app uses elsewhere, placed
 * on a stage that is painted in the color of the page. That color matters, because a mascot draws
 * its eyes in the color it is drawn on so that they read as holes in its silhouette rather than as
 * shapes on top of it.
 *
 * The jelly mascot and the jasmine spirit are views rather than composables, so they are hosted in
 * the stage as they are and keep the animations and the finger tracking they have on their own.
 */
@Composable
fun MascotsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val stageColor: Color = MyApplicationTheme.colorScheme.background

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentPadding = immersiveTopBarContentPadding(extraTop = MascotsContentTopSpacing),
            verticalArrangement = Arrangement.spacedBy(space = MascotsGroupSpacing),
        ) {
            mascotItems(stageColor = stageColor)
        }

        ImmersiveTopBar(
            leadingContent = { MascotsNavigateBackButton(onClick = onNavigateBack) },
        )
    }
}

/**
 * The mascots of the screen, one group each, in the order they are shown.
 */
private fun LazyListScope.mascotItems(stageColor: Color) {
    hostedMascotItems(stageColor = stageColor)
}

/**
 * The mascots that are views of their own, each keeping the animation and the finger tracking it
 * has on its own.
 */
private fun LazyListScope.hostedMascotItems(stageColor: Color) {
    mascotItem(
        titleRes = R.string.mascots_jelly_bob_title,
        captionRes = R.string.mascots_jelly_bob_description,
        stageColor = stageColor,
    ) {
        AndroidView(
            factory = { context -> JellyBobView(context) },
            modifier = Modifier.size(size = MascotsJellyBobSize),
        )
    }

    mascotItem(
        titleRes = R.string.mascots_jasmine_title,
        captionRes = R.string.mascots_jasmine_description,
        stageColor = stageColor,
    ) {
        AndroidView(
            factory = { context -> JasmineSpiritView(context) },
            modifier = Modifier.size(size = MascotsJasmineSize),
        )
    }
}

/**
 * One group of the screen: the title of a mascot state, the stage it is drawn on, and its caption.
 */
private fun LazyListScope.mascotItem(
    titleRes: Int,
    captionRes: Int,
    stageColor: Color,
    stage: @Composable BoxScope.() -> Unit,
) {
    item {
        MascotGroup(
            title = stringResource(id = titleRes),
            caption = stringResource(id = captionRes),
            stageColor = stageColor,
            stage = stage,
        )
    }
}

/**
 * One mascot of the screen: the title of its state, the stage it is drawn on, and what it does.
 */
@Composable
private fun MascotGroup(
    title: String,
    caption: String,
    stageColor: Color,
    stage: @Composable BoxScope.() -> Unit,
) {
    SettingsGroup(title = title) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height = MascotsStageHeight)
                .background(color = stageColor)
                .padding(all = MascotsStagePadding),
            contentAlignment = Alignment.Center,
            content = stage,
        )
        Text(
            text = caption,
            style = MyApplicationTheme.typography.bodyMedium,
            color = MyApplicationTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(
                start = MascotsCaptionPadding,
                end = MascotsCaptionPadding,
                bottom = MascotsCaptionPadding,
            ),
        )
    }
}

@Composable
private fun MascotsNavigateBackButton(onClick: () -> Unit) {
    ImmersiveTopBarIconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(id = R.string.settings_navigate_back),
        )
    }
}
