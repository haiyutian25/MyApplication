package com.example.myapplication.ui.platform.components

import androidx.compose.runtime.Composable
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect

/**
 * Runs [onDismiss] when the screen that calls this stops being the foreground destination.
 *
 * Screens that host pop-ups — menus, dialogs, sheets — need this. A pop-up is rendered in a window
 * of its own, so it outlives the composable that opened it for as long as the navigation transition
 * keeps that screen composed, and it ends up floating over the screen being revealed.
 *
 * Navigation 3 gives every destination its own lifecycle, and that lifecycle is what
 * `LocalLifecycleOwner` resolves to inside a destination. Pausing therefore happens as the screen
 * is being left, before the transition has finished swapping the screens, which is early enough
 * to keep the pop-up from ever showing up on the next screen.
 *
 * Use it for every layer a screen can leave open:
 *
 * ```kotlin
 * var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
 *
 * DismissOnScreenPause(isExpanded = isMenuExpanded) { isMenuExpanded = false }
 * ```
 *
 * @param isExpanded Whether a pop-up is currently open. The effect only observes the lifecycle
 * while this is true, so a screen that has nothing open pays nothing.
 * @param onDismiss Closes the pop-up. It must be safe to call when nothing is open, and it must not
 * navigate: leaving the screen is the caller's business, and this runs as the screen is going away.
 */
@Composable
fun DismissOnScreenPause(
    isExpanded: Boolean,
    onDismiss: () -> Unit,
) {
    if (isExpanded) {
        LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE, onEvent = onDismiss)
    }
}
