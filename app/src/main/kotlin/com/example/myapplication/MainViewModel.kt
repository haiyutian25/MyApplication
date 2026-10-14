package com.example.myapplication

import androidx.lifecycle.viewModelScope
import com.example.myapplication.core.model.AppTheme
import com.example.myapplication.data.repository.userpreferences.UserPreferencesRepository
import com.example.myapplication.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/**
 * Manages the top-level state of the app that is required outside of an individual screen.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    userPreferencesRepository: UserPreferencesRepository,
) : BaseViewModel<MainState, Unit, MainAction>(
    initialState = MainState(
        appTheme = userPreferencesRepository.appTheme,
        isFirstLaunch = userPreferencesRepository.isFirstLaunch,
    ),
) {
    init {
        userPreferencesRepository
            .appThemeFlow
            .onEach { appTheme ->
                sendAction(MainAction.Internal.AppThemeUpdateReceive(appTheme = appTheme))
            }
            .launchIn(scope = viewModelScope)
    }

    override fun handleAction(action: MainAction) {
        when (action) {
            is MainAction.Internal.AppThemeUpdateReceive -> handleAppThemeUpdateReceive(action)
        }
    }

    private fun handleAppThemeUpdateReceive(action: MainAction.Internal.AppThemeUpdateReceive) {
        mutableStateFlow.update { it.copy(appTheme = action.appTheme) }
    }
}

/**
 * Models the top-level state of the app.
 */
data class MainState(
    val appTheme: AppTheme,
    val isFirstLaunch: Boolean,
)

/**
 * Models actions that the top-level app state may receive.
 */
sealed class MainAction {
    /**
     * Models internal actions that come from asynchronous processes within the ViewModel.
     */
    sealed class Internal : MainAction() {
        /**
         * The app theme has been updated from the data layer.
         */
        data class AppThemeUpdateReceive(val appTheme: AppTheme) : Internal()
    }
}
