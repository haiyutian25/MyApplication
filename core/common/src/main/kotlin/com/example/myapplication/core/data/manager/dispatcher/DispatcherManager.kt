package com.example.myapplication.core.data.manager.dispatcher

import kotlinx.coroutines.CoroutineDispatcher

/**
 * A manager for providing the various [CoroutineDispatcher] instances used throughout the app.
 */
interface DispatcherManager {
    /**
     * The dispatcher used for work that must run on the main thread.
     */
    val main: CoroutineDispatcher

    /**
     * The dispatcher used for work that requires a lot of CPU time.
     */
    val default: CoroutineDispatcher

    /**
     * The dispatcher used for blocking IO work.
     */
    val io: CoroutineDispatcher
}
