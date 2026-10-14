package com.example.myapplication.feature.main.impl

import android.os.Parcelable
import com.example.myapplication.data.repository.userpreferences.UserPreferencesRepository
import com.example.myapplication.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages root level navigation state of the application.
 */
@HiltViewModel
class RootNavViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel<RootNavState, Unit, RootNavAction>(
    initialState = if (userPreferencesRepository.isFirstLaunch) {
        RootNavState.Onboarding
    } else {
        RootNavState.Home
    },
) {
    override fun handleAction(action: RootNavAction) {
        when (action) {
            RootNavAction.OnboardingFinished -> handleOnboardingFinished()
        }
    }

    private fun handleOnboardingFinished() {
        userPreferencesRepository.markFirstLaunchComplete()
        mutableStateFlow.update { RootNavState.Home }
    }
}

/**
 * Models the state of the root navigation.
 */
@Parcelize
sealed class RootNavState : Parcelable {
    /**
     * The app is displaying the onboarding.
     */
    @Parcelize
    data object Onboarding : RootNavState()

    /**
     * The app is displaying the home screen.
     */
    @Parcelize
    data object Home : RootNavState()
}

/**
 * Models actions that the root navigation may receive.
 */
sealed class RootNavAction {
    /**
     * The user has finished the onboarding.
     */
    data object OnboardingFinished : RootNavAction()
}
