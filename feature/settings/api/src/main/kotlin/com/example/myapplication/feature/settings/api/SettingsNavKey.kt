package com.example.myapplication.feature.settings.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation contract of the settings feature.
 */
@Serializable
sealed interface SettingsNavKey : NavKey {
    /**
     * Top-level settings screen.
     */
    @Serializable
    data object Settings : SettingsNavKey

    /**
     * Screen that shows the mascot of the app in each of the states it is drawn in.
     */
    @Serializable
    data object Mascots : SettingsNavKey
}

/**
 * Backward-compatible alias for the settings route.
 */
typealias SettingsRoute = SettingsNavKey.Settings

/**
 * Backward-compatible alias for the mascots route.
 */
typealias MascotsRoute = SettingsNavKey.Mascots
