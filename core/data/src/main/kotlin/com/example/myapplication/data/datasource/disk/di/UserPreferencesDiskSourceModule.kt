package com.example.myapplication.data.datasource.disk.di

import android.content.Context
import com.example.myapplication.data.datasource.disk.UserPreferencesDiskSource
import com.example.myapplication.data.datasource.disk.UserPreferencesDiskSourceImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

private const val USER_PREFERENCES_NAME = "user_preferences"

/**
 * Provides the [UserPreferencesDiskSource] to the dependency injection graph. The associated
 * implementation is manually constructed so that it may not be injected by accident.
 */
@Module
@InstallIn(SingletonComponent::class)
object UserPreferencesDiskSourceModule {
    @Provides
    @Singleton
    fun providesUserPreferencesDiskSource(
        @ApplicationContext context: Context,
    ): UserPreferencesDiskSource = UserPreferencesDiskSourceImpl(
        sharedPreferences = context.getSharedPreferences(
            USER_PREFERENCES_NAME,
            Context.MODE_PRIVATE,
        ),
    )
}
