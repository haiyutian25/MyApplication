package com.example.myapplication.data.repository.userpreferences

import com.example.myapplication.core.model.AppTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * A repository for managing user preferences. This represents the outermost level of the data
 * layer and is meant to be exposed directly to the UI layer.
 */
interface UserPreferencesRepository {
    /**
     * Whether the app is currently being launched for the very first time.
     */
    val isFirstLaunch: Boolean

    /**
     * The theme that the app should use.
     */
    val appTheme: AppTheme

    /**
     * A [StateFlow] that emits updates to the app theme as they occur.
     */
    val appThemeFlow: StateFlow<AppTheme>

    /**
     * Marks the first launch of the app as having completed.
     */
    fun markFirstLaunchComplete()

    /**
     * Updates the theme that the app should use.
     */
    fun setAppTheme(appTheme: AppTheme)
}
