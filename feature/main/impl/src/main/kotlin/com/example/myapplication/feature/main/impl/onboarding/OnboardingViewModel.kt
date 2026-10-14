package com.example.myapplication.feature.main.impl.onboarding

import androidx.lifecycle.ViewModel
import com.example.myapplication.core.model.AppTheme
import com.example.myapplication.data.repository.userpreferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
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
}
