package com.example.myapplication.feature.main.impl

import android.os.Parcelable
import com.example.myapplication.ui.platform.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

private const val MAX_COUNT = 3

/**
 * Manages the state and behavior of the home screen.
 */
@HiltViewModel
class HomeViewModel @Inject constructor() : BaseViewModel<HomeState, HomeEvent, HomeAction>(
    initialState = HomeState(count = 0),
) {
    override fun handleAction(action: HomeAction) {
        when (action) {
            HomeAction.IncrementClick -> handleIncrementClick()
        }
    }

    private fun handleIncrementClick() {
        val updatedCount = state.count + 1
        mutableStateFlow.update { it.copy(count = updatedCount) }
        if (updatedCount >= MAX_COUNT) {
            sendEvent(HomeEvent.NavigateBack)
        }
    }
}

/**
 * Models the state of the home screen.
 */
@Parcelize
data class HomeState(
    val count: Int,
) : Parcelable

/**
 * Models one-shot events that the home screen may emit.
 */
sealed class HomeEvent {
    /**
     * Navigate back in the back stack.
     */
    data object NavigateBack : HomeEvent()
}

/**
 * Models actions that the home screen may receive.
 */
sealed class HomeAction {
    /**
     * The user has clicked the increment button.
     */
    data object IncrementClick : HomeAction()
}
