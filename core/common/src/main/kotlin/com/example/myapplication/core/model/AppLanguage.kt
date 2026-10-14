package com.example.myapplication.core.model

/**
 * The languages the app can be shown in, including the entry that follows the language of the
 * device.
 */
enum class AppLanguage(val languageTag: String) {
    /** Follow the language of the device. */
    SYSTEM(languageTag = ""),

    /** English. */
    ENGLISH(languageTag = "en"),

    /** Simplified Chinese. */
    CHINESE(languageTag = "zh"),
}
