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

    /**
     * Screen that says what the app is — its name, its version and what it does — and where a newer
     * build of it is looked for.
     */
    @Serializable
    data object About : SettingsNavKey
}

/**
 * Backward-compatible alias for the settings route.
 */
typealias SettingsRoute = SettingsNavKey.Settings

/**
 * Backward-compatible alias for the mascots route.
 */
typealias MascotsRoute = SettingsNavKey.Mascots

/**
 * Backward-compatible alias for the about route.
 */
typealias AboutRoute = SettingsNavKey.About
