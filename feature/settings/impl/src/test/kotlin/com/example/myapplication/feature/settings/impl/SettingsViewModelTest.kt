package com.example.myapplication.feature.settings.impl

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
class SettingsViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()
    private val fakeUserPreferencesRepository = FakeUserPreferencesRepository()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should reflect the stored theme`() = runTest {
        val viewModel = SettingsViewModel(fakeUserPreferencesRepository)

        viewModel.stateFlow.test {
            assertEquals(
                SettingsState(appTheme = AppTheme.DEFAULT),
                awaitItem(),
            )
        }
    }

    @Test
    fun `AppThemeSelect should persist the theme and update the state`() = runTest {
        val viewModel = SettingsViewModel(fakeUserPreferencesRepository)

        viewModel.stateFlow.test {
            assertEquals(
                SettingsState(appTheme = AppTheme.DEFAULT),
                awaitItem(),
            )

            viewModel.trySendAction(SettingsAction.AppThemeSelect(appTheme = AppTheme.DARK))
            assertEquals(
                SettingsState(appTheme = AppTheme.DARK),
                awaitItem(),
            )

            assertEquals(AppTheme.DARK, fakeUserPreferencesRepository.appTheme)
        }
    }
}

private class FakeUserPreferencesRepository : UserPreferencesRepository {
    private val mutableAppThemeFlow: MutableStateFlow<AppTheme> =
        MutableStateFlow(AppTheme.DEFAULT)

    override val isFirstLaunch: Boolean = false

    override val appTheme: AppTheme
        get() = mutableAppThemeFlow.value

    override val appThemeFlow: StateFlow<AppTheme> = mutableAppThemeFlow.asStateFlow()

    override fun markFirstLaunchComplete() = Unit

    override fun setAppTheme(appTheme: AppTheme) {
        mutableAppThemeFlow.value = appTheme
    }
}
