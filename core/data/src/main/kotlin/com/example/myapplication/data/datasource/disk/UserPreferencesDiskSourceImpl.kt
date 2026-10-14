package com.example.myapplication.data.datasource.disk

import android.content.SharedPreferences
import androidx.core.content.edit
import com.example.myapplication.core.model.AppTheme

private const val KEY_FIRST_LAUNCH = "isFirstLaunch"
private const val KEY_APP_THEME = "appTheme"

/**
 * The default implementation of [UserPreferencesDiskSource].
 */
class UserPreferencesDiskSourceImpl(
    private val sharedPreferences: SharedPreferences,
) : UserPreferencesDiskSource {
    override var isFirstLaunch: Boolean
        get() = sharedPreferences.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) {
            sharedPreferences.edit { putBoolean(KEY_FIRST_LAUNCH, value) }
        }

    override var appTheme: AppTheme
        get() = sharedPreferences
            .getString(KEY_APP_THEME, null)
            ?.let { storedValue ->
                AppTheme.entries.firstOrNull { it.name == storedValue }
            }
            ?: AppTheme.DEFAULT
        set(value) {
            sharedPreferences.edit { putString(KEY_APP_THEME, value.name) }
        }
}
