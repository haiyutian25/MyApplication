package com.example.myapplication.data.repository.applanguage.di

import com.example.myapplication.data.repository.applanguage.AppLanguageRepository
import com.example.myapplication.data.repository.applanguage.AppLanguageRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the language repository to the implementation that keeps the language with the platform.
 */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class AppLanguageRepositoryModule {
    @Binds
    @Singleton
    internal abstract fun bindAppLanguageRepository(
        impl: AppLanguageRepositoryImpl,
    ): AppLanguageRepository
}
