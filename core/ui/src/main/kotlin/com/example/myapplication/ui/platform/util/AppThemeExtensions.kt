package com.example.myapplication.ui.platform.util

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.example.myapplication.core.model.AppTheme

/**
 * Determines whether the app should currently use the dark theme for the given [AppTheme].
 */
@Composable
@ReadOnlyComposable
fun AppTheme.isDarkMode(): Boolean = when (this) {
    AppTheme.DARK -> true
    AppTheme.LIGHT -> false
    AppTheme.DEFAULT -> isSystemInDarkTheme()
}
