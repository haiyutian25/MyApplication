package com.example.myapplication.feature.settings.impl

import android.os.Parcelable
import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.model.AppTheme
import com.example.myapplication.data.repository.userpreferences.UserPreferencesRepository
import com.example.myapplication.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

/**
 * Manages the state and behavior of the settings screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel<SettingsState, Unit, SettingsAction>(
    initialState = SettingsState(appTheme = userPreferencesRepository.appTheme),
) {
    init {
        userPreferencesRepository
            .appThemeFlow
            .onEach { appTheme ->
                sendAction(SettingsAction.Internal.AppThemeUpdateReceive(appTheme = appTheme))
            }
            .launchIn(scope = viewModelScope)
    }

    override fun handleAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.AppThemeSelect -> handleAppThemeSelect(action)
            is SettingsAction.Internal.AppThemeUpdateReceive -> {
                handleAppThemeUpdateReceive(action)
            }
        }
    }

    private fun handleAppThemeSelect(action: SettingsAction.AppThemeSelect) {
        userPreferencesRepository.setAppTheme(appTheme = action.appTheme)
    }

    private fun handleAppThemeUpdateReceive(
        action: SettingsAction.Internal.AppThemeUpdateReceive,
    ) {
        mutableStateFlow.update { it.copy(appTheme = action.appTheme) }
    }
}

/**
 * Models the state of the settings screen.
 */
@Parcelize
data class SettingsState(
    val appTheme: AppTheme,
) : Parcelable

/**
 * Models actions that the settings screen may receive.
 */
sealed class SettingsAction {
    /**
     * The user has selected a new theme.
     */
    data class AppThemeSelect(val appTheme: AppTheme) : SettingsAction()

    /**
     * Models internal actions that come from asynchronous processes within the ViewModel.
     */
    sealed class Internal : SettingsAction() {
        /**
         * The app theme has been updated from the data layer.
         */
        data class AppThemeUpdateReceive(val appTheme: AppTheme) : Internal()
    }
}
