package com.example.myapplication.ui.platform.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.example.myapplication.core.model.AppTheme

/**
 * Determines whether the given [AppTheme] shows the app in dark mode, given what the system itself
 * is currently set to.
 *
 * This is the form a callback uses: a click handler runs outside composition, so it cannot call the
 * composable [isDarkMode] to find out what the system is set to.
 */
fun AppTheme.isDarkMode(systemIsDark: Boolean): Boolean = when (this) {
    AppTheme.DARK -> true
    AppTheme.LIGHT -> false
    AppTheme.DEFAULT -> systemIsDark
}

/**
 * Determines whether the app should currently use the dark theme for the given [AppTheme].
 */
@Composable
@ReadOnlyComposable
fun AppTheme.isDarkMode(): Boolean = isDarkMode(systemIsDark = isSystemInDarkTheme())
