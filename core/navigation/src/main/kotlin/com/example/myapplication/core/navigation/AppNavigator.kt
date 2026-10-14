package com.example.myapplication.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.rememberNavBackStack

/**
 * Thin MVVM-friendly wrapper around Navigation 3's [NavBackStack].
 *
 * @param navigationState the underlying [NavBackStack] instance.
 */
@Stable
class AppNavigator(val navigationState: NavBackStack<NavKey>) {

    /**
     * Pushes a new destination [key] onto the back stack.
     */
    fun navigate(key: NavKey) {
        navigationState.add(key)
    }

    /**
     * Clears the back stack and replaces it with the given [key].
     */
    fun replace(key: NavKey) {
        navigationState.clear()
        navigationState.add(key)
    }

    /**
     * Pops the top destination from the back stack if more than one item remains.
     *
     * @return `true` if popped, `false` otherwise.
     */
    fun goBack(): Boolean {
        if (navigationState.size <= 1) return false
        navigationState.removeAt(navigationState.size - 1)
        return true
    }
}

/**
 * Creates an [AppNavigator] whose back stack survives configuration changes and process death.
 *
 * @param startKeys the initial navigation keys on the back stack.
 * @return the remembered [AppNavigator] instance.
 */
@Composable
fun rememberAppNavigator(vararg startKeys: NavKey): AppNavigator {
    val backStack = rememberNavBackStack(*startKeys)
    return remember(backStack) { AppNavigator(backStack) }
}
