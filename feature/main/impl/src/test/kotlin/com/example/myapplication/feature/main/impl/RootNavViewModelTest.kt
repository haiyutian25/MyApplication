package com.example.myapplication.feature.main.impl

import app.cash.turbine.test
import com.example.myapplication.core.model.AppTheme
import com.example.myapplication.data.repository.userpreferences.UserPreferencesRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RootNavViewModelTest {
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
    fun `initial state should be Onboarding when isFirstLaunch is true`() = runTest {
        val fakeRepo = FakeUserPreferencesRepository(initialIsFirstLaunch = true)
        val viewModel = RootNavViewModel(fakeRepo)

        viewModel.stateFlow.test {
            assertEquals(RootNavState.Onboarding, awaitItem())
        }
    }

    @Test
    fun `initial state should be Home when isFirstLaunch is false`() = runTest {
        val fakeRepo = FakeUserPreferencesRepository(initialIsFirstLaunch = false)
        val viewModel = RootNavViewModel(fakeRepo)

        viewModel.stateFlow.test {
            assertEquals(RootNavState.Home, awaitItem())
        }
    }

    @Test
    fun `OnboardingFinished should complete first launch and transition to Home`() = runTest {
        val fakeRepo = FakeUserPreferencesRepository(initialIsFirstLaunch = true)
        val viewModel = RootNavViewModel(fakeRepo)

        viewModel.stateFlow.test {
            assertEquals(RootNavState.Onboarding, awaitItem())

            viewModel.trySendAction(RootNavAction.OnboardingFinished)
            assertEquals(RootNavState.Home, awaitItem())

            assertFalse(fakeRepo.isFirstLaunch)
        }
    }
}

private class FakeUserPreferencesRepository(
    initialIsFirstLaunch: Boolean,
) : UserPreferencesRepository {
    private val mutableAppThemeFlow: MutableStateFlow<AppTheme> =
        MutableStateFlow(AppTheme.DEFAULT)

    private var mutableIsFirstLaunch: Boolean = initialIsFirstLaunch

    override val isFirstLaunch: Boolean
        get() = mutableIsFirstLaunch

    override val appTheme: AppTheme
        get() = mutableAppThemeFlow.value

    override val appThemeFlow: StateFlow<AppTheme> = mutableAppThemeFlow.asStateFlow()

    override fun markFirstLaunchComplete() {
        mutableIsFirstLaunch = false
    }

    override fun setAppTheme(appTheme: AppTheme) {
        mutableAppThemeFlow.value = appTheme
    }
}
