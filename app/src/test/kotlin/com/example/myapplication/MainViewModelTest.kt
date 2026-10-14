package com.example.myapplication

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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeRepo = FakeUserPreferencesRepository()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should reflect the stored theme and isFirstLaunch`() = runTest {
        val viewModel = MainViewModel(fakeRepo)

        viewModel.stateFlow.test {
            assertEquals(
                MainState(appTheme = AppTheme.DEFAULT, isFirstLaunch = true),
                awaitItem(),
            )
        }
    }

    @Test
    fun `app theme update from repository should update the state`() = runTest {
        val viewModel = MainViewModel(fakeRepo)

        viewModel.stateFlow.test {
            assertEquals(
                MainState(appTheme = AppTheme.DEFAULT, isFirstLaunch = true),
                awaitItem(),
            )

            fakeRepo.setAppTheme(AppTheme.DARK)

            assertEquals(
                MainState(appTheme = AppTheme.DARK, isFirstLaunch = true),
                awaitItem(),
            )
        }
    }
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val mutableAppThemeFlow: MutableStateFlow<AppTheme> =
        MutableStateFlow(AppTheme.DEFAULT)

    override val isFirstLaunch: Boolean = true

    override val appTheme: AppTheme
        get() = mutableAppThemeFlow.value

    override val appThemeFlow: StateFlow<AppTheme> = mutableAppThemeFlow.asStateFlow()

    override fun markFirstLaunchComplete() = Unit

    override fun setAppTheme(appTheme: AppTheme) {
        mutableAppThemeFlow.value = appTheme
    }
}
