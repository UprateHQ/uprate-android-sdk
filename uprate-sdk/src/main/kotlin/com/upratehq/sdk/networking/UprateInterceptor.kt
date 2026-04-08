package com.upratehq.sdk.networking

import com.upratehq.sdk.UprateConfiguration
import com.upratehq.sdk.UserContext
import okhttp3.Interceptor
import okhttp3.Response

internal class UprateInterceptor(
    private val configuration: UprateConfiguration,
    private val appVersion: String,
    private val userContextProvider: () -> UserContext?
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
            .header("Authorization", "Bearer ${configuration.apiKey}")
            .header("Accept", "application/json")
            .header("X-SDK-Device-Platform", "android")
            .header("X-SDK-App-Version", appVersion)

        val userContext = userContextProvider()
        if (userContext != null) {
            builder.header("X-SDK-User-Id", userContext.userId)
            userContext.email?.let { builder.header("X-SDK-User-Email", it) }
            userContext.name?.let { builder.header("X-SDK-User-Name", it) }
        }

        return chain.proceed(builder.build())
    }
}
