package com.example.myapplication.feature.main.impl.onboarding

import androidx.lifecycle.ViewModel
import com.example.myapplication.core.model.AppTheme
import com.example.myapplication.data.repository.userpreferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Holds the choices the onboarding writes: the theme it offers, and the flag that says the app has
 * been introduced and should not be introduced again.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    /**
     * The theme the app is currently set to.
     */
    val appTheme: StateFlow<AppTheme> = userPreferencesRepository.appThemeFlow

    /**
     * Updates the theme that the app should use.
     */
    fun setAppTheme(appTheme: AppTheme) {
        userPreferencesRepository.setAppTheme(appTheme)
    }

    /**
     * Marks the first launch of the app as having completed, so the onboarding does not show again.
     */
    fun completeOnboarding() {
        userPreferencesRepository.markFirstLaunchComplete()
    }

    private val mutableCurrentStepIndex: MutableStateFlow<Int> = MutableStateFlow(0)

    /**
     * The steps the flow walks through, in order.
     */
    internal val steps: List<OnboardingStep> = onboardingSteps()

    /**
     * How far the flow has come: the index of the step the onboarding is showing.
     */
    val currentStepIndex: StateFlow<Int> = mutableCurrentStepIndex.asStateFlow()

    /**
     * Moves on to the step after the current one, if there is one.
     */
    fun goToNextStep() {
        if (mutableCurrentStepIndex.value < steps.lastIndex) {
            mutableCurrentStepIndex.update { it + 1 }
        }
    }

    /**
     * Moves back to the step before the current one, if there is one.
     */
    fun goToPreviousStep() {
        if (mutableCurrentStepIndex.value > 0) {
            mutableCurrentStepIndex.update { it - 1 }
        }
    }
}
