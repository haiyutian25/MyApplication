package com.example.myapplication.network.retrofit

import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

private const val JSON_MEDIA_TYPE = "application/json"

/**
 * The default implementation of [RetrofitBuilder].
 */
class RetrofitBuilderImpl(
    private val json: Json,
) : RetrofitBuilder {
    override fun createRetrofit(
        baseUrl: String,
        client: OkHttpClient,
    ): Retrofit = Retrofit
        .Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory(JSON_MEDIA_TYPE.toMediaType()))
        .build()
}
