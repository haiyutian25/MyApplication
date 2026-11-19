package com.example.myapplication.feature.settings.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.example.myapplication.ui.platform.theme.MyApplicationTheme
import kotlinx.coroutines.delay

/**
 * How long the word is held after it lands, before the launch hands the screen back to the app.
 */
private const val LAUNCH_HOLD_SECONDS = 1.1f

/**
 * The reel as the app opens with it: the same particles and the same word as the screen the
 * settings opens, with nothing of that screen over it.
 *
 * It plays until the word has landed and held for a beat, then hands back so that the app can be
 * shown under it. A tap ends it at once — a launch is not a place to wait.
 */
@Composable
fun ParticleLaunchReel(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            // The page colour under the reel, so that nothing of the app shows through it.
            .background(color = MyApplicationTheme.colorScheme.background)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onFinished,
            ),
    ) {
        ParticleReel(restartOnTap = false)
    }

    LaunchedEffect(Unit) {
        delay(timeMillis = ((IMPACT_AT + LAUNCH_HOLD_SECONDS) * 1000f).toLong())
        onFinished()
    }
}
