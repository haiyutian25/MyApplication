package com.example.myapplication.feature.main.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.myapplication.feature.main.impl.onboarding.OnboardingScreen

/**
 * The root-level screen of the app, responsible for state-based navigation between top-level
 * screens.
 *
 * On a first launch it shows the onboarding. When that is finished the state moves to
 * [RootNavState.Home] and the caller swaps this screen for the home screen, so the onboarding is
 * not left on the back stack.
 */
@Composable
fun RootNavScreen(
    onNavigateToHomeScreen: () -> Unit,
    viewModel: RootNavViewModel = hiltViewModel(),
) {
    val state by viewModel.stateFlow.collectAsStateWithLifecycle()

    LaunchedEffect(key1 = state) {
        when (state) {
            RootNavState.Onboarding -> Unit
            RootNavState.Home -> onNavigateToHomeScreen()
        }
    }

    when (state) {
        RootNavState.Onboarding -> {
            OnboardingScreen(
                onFinished = { viewModel.trySendAction(RootNavAction.OnboardingFinished) },
            )
        }

        RootNavState.Home -> Unit
    }
}
