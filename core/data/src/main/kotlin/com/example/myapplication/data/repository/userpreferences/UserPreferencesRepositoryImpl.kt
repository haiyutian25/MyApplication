package com.example.myapplication.data.repository.userpreferences

import com.example.myapplication.core.model.AppTheme
import com.example.myapplication.data.datasource.disk.UserPreferencesDiskSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The default implementation of [UserPreferencesRepository].
 */
class UserPreferencesRepositoryImpl(
    private val userPreferencesDiskSource: UserPreferencesDiskSource,
) : UserPreferencesRepository {
    private val mutableAppThemeFlow: MutableStateFlow<AppTheme> =
        MutableStateFlow(userPreferencesDiskSource.appTheme)

    override val isFirstLaunch: Boolean
        get() = userPreferencesDiskSource.isFirstLaunch

    override val appTheme: AppTheme
        get() = mutableAppThemeFlow.value

    override val appThemeFlow: StateFlow<AppTheme> = mutableAppThemeFlow.asStateFlow()

    override fun markFirstLaunchComplete() {
        userPreferencesDiskSource.isFirstLaunch = false
    }

    override fun setAppTheme(appTheme: AppTheme) {
        userPreferencesDiskSource.appTheme = appTheme
        mutableAppThemeFlow.value = appTheme
    }
}
