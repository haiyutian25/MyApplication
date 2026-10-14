package com.example.myapplication

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * The main [Application] class for the app, responsible for app-wide initialization.
 */
@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.HAS_LOGS_ENABLED) {
            Timber.plant(Timber.DebugTree())
        }
    }
}
