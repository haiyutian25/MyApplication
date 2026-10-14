package com.example.myapplication.network.retrofit

import okhttp3.OkHttpClient
import retrofit2.Retrofit

/**
 * A builder for creating the [Retrofit] instances used by the network layer.
 */
interface RetrofitBuilder {
    /**
     * Creates a [Retrofit] instance using the given [baseUrl] and [client].
     */
    fun createRetrofit(
        baseUrl: String,
        client: OkHttpClient,
    ): Retrofit
}
