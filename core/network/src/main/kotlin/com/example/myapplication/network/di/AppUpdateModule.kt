package com.example.myapplication.network.di

import android.content.Context
import com.example.myapplication.network.AppUpdateManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient

/**
 * Provides the app's in-app update checker and the HTTP client it uses.
 *
 * The client is the app's shared one: nothing else in the app builds an [OkHttpClient] yet, so this
 * is where it is created.
 */
@Module
@InstallIn(SingletonComponent::class)
internal object AppUpdateModule {

    /**
     * Checks GitHub Releases for a newer build of the app and downloads it.
     */
    @Provides
    @Singleton
    fun provideAppUpdateManager(
        @ApplicationContext context: Context,
        httpClient: OkHttpClient,
    ): AppUpdateManager = AppUpdateManager(context = context, httpClient = httpClient)

    /**
     * The HTTP client used to check for and download app updates.
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient = OkHttpClient.Builder().build()
}
