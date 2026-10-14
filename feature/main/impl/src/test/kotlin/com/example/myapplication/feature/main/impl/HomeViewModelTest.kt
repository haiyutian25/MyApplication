package com.example.myapplication.feature.main.impl

import app.cash.turbine.test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should contain a count of zero`() = runTest {
        val viewModel = HomeViewModel()

        viewModel.stateFlow.test {
            assertEquals(HomeState(count = 0), awaitItem())
        }
    }

    @Test
    fun `increment click should increase the count`() = runTest {
        val viewModel = HomeViewModel()

        viewModel.stateFlow.test {
            assertEquals(HomeState(count = 0), awaitItem())

            viewModel.trySendAction(HomeAction.IncrementClick)
            assertEquals(HomeState(count = 1), awaitItem())

            viewModel.trySendAction(HomeAction.IncrementClick)
            assertEquals(HomeState(count = 2), awaitItem())
        }
    }

    @Test
    fun `reaching max count should emit NavigateBack event`() = runTest {
        val viewModel = HomeViewModel()

        viewModel.eventFlow.test {
            viewModel.trySendAction(HomeAction.IncrementClick)
            viewModel.trySendAction(HomeAction.IncrementClick)
            viewModel.trySendAction(HomeAction.IncrementClick)

            assertEquals(HomeEvent.NavigateBack, awaitItem())
        }
    }
}
