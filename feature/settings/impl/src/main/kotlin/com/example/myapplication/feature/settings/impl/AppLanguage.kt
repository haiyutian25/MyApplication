package com.example.myapplication.feature.settings.impl

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * The languages the app can be shown in, including the entry that follows the language of the
 * device.
 *
 * The choice is kept by AppCompat, which is where the platform expects an app to keep it: on
 * Android 13 and above it is the same setting that the system settings screen offers for the app,
 * and on older versions AppCompat stores it for the app (see the service declared in the manifest).
 * Nothing else in the app has to be told about a change — applying one recreates the activity, and
 * every string is then resolved in the new language.
 */
enum class AppLanguage(private val languageTag: String) {
    /** Follow the language of the device. */
    SYSTEM(languageTag = ""),

    /** English. */
    ENGLISH(languageTag = "en"),

    /** Simplified Chinese. */
    CHINESE(languageTag = "zh"),
    ;

    /**
     * Shows the app in this language from now on.
     *
     * This has to run on the main thread and recreates the activity, which is what makes the whole
     * app resolve its strings again.
     */
    fun apply() {
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags(languageTag),
        )
    }

    /**
     * Reading and comparing the language the app is shown in.
     */
    companion object {
        /**
         * The language the app is currently shown in.
         */
        fun current(): AppLanguage {
            val current = AppCompatDelegate.getApplicationLocales()[0] ?: return SYSTEM
            val match = entries.firstOrNull {
                it != SYSTEM && it.languageTag == current.language
            }
            return match ?: SYSTEM
        }
    }
}
