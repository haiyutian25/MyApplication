package com.example.myapplication.data.datasource.disk

import com.example.myapplication.core.model.AppTheme

/**
 * A data source for persisting user preferences to disk.
 */
interface UserPreferencesDiskSource {
    /**
     * Whether the app is currently being launched for the very first time.
     */
    var isFirstLaunch: Boolean

    /**
     * The theme that the app should use.
     */
    var appTheme: AppTheme
}
