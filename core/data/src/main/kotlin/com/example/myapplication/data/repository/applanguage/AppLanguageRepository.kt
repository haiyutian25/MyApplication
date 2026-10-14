package com.example.myapplication.data.repository.applanguage

import com.example.myapplication.core.model.AppLanguage
import kotlinx.coroutines.flow.StateFlow

/**
 * Holds the language the app is shown in.
 *
 * Where the value lives is left to the implementation: on Android the platform keeps it with the
 * app, so that the system settings screen and the app share one setting.
 */
interface AppLanguageRepository {
    /**
     * The language the app is currently shown in.
     */
    val appLanguageFlow: StateFlow<AppLanguage>

    /**
     * Shows the app in [appLanguage] from now on.
     */
    fun setAppLanguage(appLanguage: AppLanguage)
}
