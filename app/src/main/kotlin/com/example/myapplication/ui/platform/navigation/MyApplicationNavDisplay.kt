package com.example.myapplication.ui.platform.navigation

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.example.myapplication.feature.main.api.HomeRoute
import com.example.myapplication.feature.main.api.RootNavRoute
import com.example.myapplication.feature.main.impl.HomeScreen
import com.example.myapplication.feature.main.impl.RootNavScreen
import com.example.myapplication.feature.settings.api.MascotsRoute
import com.example.myapplication.feature.settings.api.SettingsRoute
import com.example.myapplication.feature.settings.impl.MascotsScreen
import com.example.myapplication.feature.settings.impl.SettingsScreen

/**
 * The top-level navigation for the application, built on Navigation 3.
 *
 * The back stack is a plain observable list of navigation keys that the app owns: navigating
 * forward adds a key to the list and navigating back removes the last key from it.
 * `rememberNavBackStack` keeps that stack across configuration changes and process death, while
 * [NavDisplay] renders the content associated with the key currently on the top of the stack.
 *
 * @param isFirstLaunch whether the app is being launched for the first time, determining
 * whether to start at [RootNavRoute] for onboarding or directly at [HomeRoute].
 */
@Composable
fun MyApplicationNavDisplay(
    isFirstLaunch: Boolean = false,
) {
    val activity = LocalActivity.current
    val backStack = rememberNavBackStack(
        if (isFirstLaunch) RootNavRoute else HomeRoute,
    )

    NavDisplay(
        entryDecorators = listOf(
            // Keeps saveable state (such as scroll position) per destination.
            rememberSaveableStateHolderNavEntryDecorator(),
            // Scopes each destination's ViewModel to its own navigation entry.
            rememberViewModelStoreNavEntryDecorator(),
        ),
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryProvider = entryProvider {
            entry<RootNavRoute> {
                RootNavScreen(
                    onNavigateToHomeScreen = {
                        // Replace RootNavRoute with HomeRoute so the welcome screen is not
                        // retained in the back stack.
                        backStack[backStack.lastIndex] = HomeRoute
                    },
                )
            }
            entry<HomeRoute> {
                HomeScreen(
                    onNavigateBack = {
                        if (backStack.size > 1) {
                            backStack.removeLastOrNull()
                        } else {
                            activity?.finish()
                        }
                    },
                    onNavigateToSettingsScreen = { backStack.add(SettingsRoute) },
                )
            }
            entry<SettingsRoute> {
                SettingsScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                    onNavigateToMascotsScreen = { backStack.add(MascotsRoute) },
                )
            }
            entry<MascotsRoute> {
                MascotsScreen(
                    onNavigateBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
