package com.example.myapplication.core.data.manager.di

import com.example.myapplication.core.data.manager.dispatcher.DispatcherManager
import com.example.myapplication.core.data.manager.dispatcher.DispatcherManagerImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Provides the [DispatcherManager] to the dependency injection graph.
 */
@Module
@InstallIn(SingletonComponent::class)
object DispatcherManagerModule {
    @Provides
    @Singleton
    fun providesDispatcherManager(): DispatcherManager = DispatcherManagerImpl()
}
