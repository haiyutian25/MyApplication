package com.example.myapplication.feature.main.api

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * Navigation contract of the main feature.
 */
@Serializable
sealed interface MainNavKey : NavKey {
    /**
     * First-launch welcome / root navigation screen.
     */
    @Serializable
    data object RootNav : MainNavKey

    /**
     * Main home screen.
     */
    @Serializable
    data object Home : MainNavKey
}

/**
 * Backward-compatible alias for the root navigation route.
 */
typealias RootNavRoute = MainNavKey.RootNav

/**
 * Backward-compatible alias for the home navigation route.
 */
typealias HomeRoute = MainNavKey.Home
