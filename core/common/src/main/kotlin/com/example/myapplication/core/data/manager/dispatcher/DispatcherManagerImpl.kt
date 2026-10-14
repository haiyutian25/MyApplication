package com.example.myapplication.core.data.manager.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * The default implementation of [DispatcherManager].
 */
class DispatcherManagerImpl : DispatcherManager {
    override val main: CoroutineDispatcher
        get() = Dispatchers.Main

    override val default: CoroutineDispatcher
        get() = Dispatchers.Default

    override val io: CoroutineDispatcher
        get() = Dispatchers.IO
}
