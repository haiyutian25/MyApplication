package com.example.myapplication.data.repository.di

import com.example.myapplication.data.datasource.disk.UserPreferencesDiskSource
import com.example.myapplication.data.repository.userpreferences.UserPreferencesRepository
import com.example.myapplication.data.repository.userpreferences.UserPreferencesRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the [UserPreferencesRepository] to the dependency injection graph. The associated
 * implementation is manually constructed so that it may not be injected by accident.
 */
@Module
@InstallIn(SingletonComponent::class)
object UserPreferencesRepositoryModule {
    @Provides
    @Singleton
    fun providesUserPreferencesRepository(
        userPreferencesDiskSource: UserPreferencesDiskSource,
    ): UserPreferencesRepository = UserPreferencesRepositoryImpl(
        userPreferencesDiskSource = userPreferencesDiskSource,
    )
}
