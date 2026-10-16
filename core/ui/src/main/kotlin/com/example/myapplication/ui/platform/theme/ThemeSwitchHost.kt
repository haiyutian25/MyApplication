package com.example.myapplication.ui.platform.theme

import android.app.Activity
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.PixelCopy
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import com.example.myapplication.ui.platform.components.ThemeReveal
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Applies the theme, and every change between light and dark goes through the reveal: the change is
 * opened out of the bottom of the screen as a circle onto the page of the theme being switched to,
 * over the page of the theme being switched away from.
 *
 * It sits where the theme is applied — above everything the app draws — rather than on whichever
 * screen asked for the change, so that no change can bypass the reveal: one asked for in a setting,
 * or one the system makes while the app follows it. It also belongs to no screen, so it carries on
 * when the screen that asked for the change goes away.
 *
 * Pass the mode the app should be in as [isDarkMode]. The mode actually in use follows it, one
 * reveal behind it while a reveal is running.
 */
@Composable
fun ThemeSwitchHost(
    isDarkMode: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val activity = context as? Activity
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val animationsEnabled = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) > 0f
    }

    var shownIsDarkMode by remember { mutableStateOf(isDarkMode) }
    var previousPage by remember { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(key1 = isDarkMode, key2 = animationsEnabled) {
        if (isDarkMode == shownIsDarkMode) return@LaunchedEffect

        // The page is taken while the mode on screen is still the old one, which is what the
        // reveal draws over the page of the new one. Where it cannot be taken — with animations
        // switched off system-wide, while a reveal is already running, or on the versions of
        // Android that cannot read a window back — there is nothing for a circle to open onto, so
        // the mode is switched outright.
        if (animationsEnabled && previousPage == null) {
            takePage(activity = activity, view = view, handler = mainHandler)?.let { page ->
                previousPage = page
            }
        }
        shownIsDarkMode = isDarkMode
    }

    MyApplicationTheme(isDarkMode = shownIsDarkMode) {
        Box(modifier = modifier.fillMaxSize()) {
            content()

            previousPage?.let { page ->
                ThemeReveal(
                    previousPage = page,
                    onCovered = { previousPage = null },
                )
            }
        }
    }
}

/**
 * Reads the page back as it is on screen now, or null where it cannot be read: before the view has
 * a size, and on the versions of Android that cannot read a window back.
 */
private suspend fun takePage(
    activity: Activity?,
    view: View,
    handler: Handler,
): ImageBitmap? {
    val window = activity?.window ?: return null
    val width = view.width
    val height = view.height
    val hasSize = width > 0 && height > 0
    if (!hasSize || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return null

    val page = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    return suspendCancellableCoroutine { continuation ->
        PixelCopy.request(
            window,
            page,
            { result ->
                continuation.resume(
                    if (result == PixelCopy.SUCCESS) page.asImageBitmap() else null,
                )
            },
            handler,
        )
    }
}
