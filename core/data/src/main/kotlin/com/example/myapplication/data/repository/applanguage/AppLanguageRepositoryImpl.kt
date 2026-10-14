package com.example.myapplication.data.repository.applanguage

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.myapplication.core.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Keeps the language of the app in AppCompat, which is where the platform expects an app to keep
 * it: on Android 13 and above it is the same setting the system settings screen offers for the app,
 * and on older versions AppCompat stores it for the app.
 */
@Singleton
class AppLanguageRepositoryImpl @Inject constructor() : AppLanguageRepository {

    private val mutableAppLanguageFlow: MutableStateFlow<AppLanguage> =
        MutableStateFlow(readFromPlatform())

    override val appLanguageFlow: StateFlow<AppLanguage> = mutableAppLanguageFlow.asStateFlow()

    override fun setAppLanguage(appLanguage: AppLanguage) {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(appLanguage.languageTag),
        )
        mutableAppLanguageFlow.value = appLanguage
    }

    private fun readFromPlatform(): AppLanguage {
        val current = AppCompatDelegate.getApplicationLocales()[0] ?: return AppLanguage.SYSTEM
        val match = AppLanguage.entries.firstOrNull {
            it != AppLanguage.SYSTEM && it.languageTag == current.language
        }
        return match ?: AppLanguage.SYSTEM
    }
}
