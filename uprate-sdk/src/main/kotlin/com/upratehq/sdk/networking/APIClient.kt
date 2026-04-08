package com.upratehq.sdk.networking

import com.upratehq.sdk.UprateConfiguration
import com.upratehq.sdk.UserContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.seconds

internal class APIClient(
    val configuration: UprateConfiguration,
    client: OkHttpClient,
    appVersion: String = "unknown"
) {
    @Volatile
    var userContext: UserContext? = null

    val json = Json { ignoreUnknownKeys = true }

    private val httpClient: OkHttpClient = client.newBuilder()
        .addInterceptor(
            UprateInterceptor(
                configuration = configuration,
                appVersion = appVersion,
                userContextProvider = { userContext }
            )
        )
        .build()

    fun requireUserContext(): UserContext {
        return userContext ?: throw UprateError.UserContextNotSet
    }

    suspend inline fun <reified T> execute(
        endpoint: APIEndpoint,
        body: String? = null
    ): T {
        requireUserContext()

        val url = configuration.baseURL.trimEnd('/') + endpoint.path
        val requestBuilder = Request.Builder().url(url)

        val mediaType = "application/json; charset=utf-8".toMediaType()
        when (endpoint.method) {
            "GET" -> requestBuilder.get()
            "POST" -> requestBuilder.post((body ?: "{}").toRequestBody(mediaType))
            "DELETE" -> {
                if (body != null) {
                    requestBuilder.delete(body.toRequestBody(mediaType))
                } else {
                    requestBuilder.delete("".toRequestBody(null))
                }
            }
        }

        val response = executeRequest(requestBuilder.build())
        val responseBody = response.body?.string() ?: ""

        if (response.isSuccessful) {
            return json.decodeFromString<T>(responseBody)
        }

        throw mapError(response.code, responseBody, response)
    }

    @PublishedApi
    internal suspend fun executeRequest(request: Request): Response {
        return suspendCancellableCoroutine { continuation ->
            val call = httpClient.newCall(request)
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onResponse(call: Call, response: Response) {
                    continuation.resume(response)
                }
                override fun onFailure(call: Call, e: IOException) {
                    continuation.resumeWithException(UprateError.NetworkError(e))
                }
            })
        }
    }

    @PublishedApi
    internal fun mapError(statusCode: Int, responseBody: String, response: Response): UprateError {
        return when (statusCode) {
            401 -> UprateError.InvalidApiKey
            403 -> UprateError.FeatureNotEnabled
            404 -> UprateError.NotFound
            422 -> {
                try {
                    val errorResponse = json.decodeFromString<ValidationErrorResponse>(responseBody)
                    UprateError.ValidationError(errorResponse.message, errorResponse.errors)
                } catch (_: Exception) {
                    UprateError.UnexpectedResponse(statusCode)
                }
            }
            429 -> {
                val retryAfter = response.header("Retry-After")?.toLongOrNull()?.seconds
                UprateError.RateLimited(retryAfter)
            }
            in 500..599 -> {
                val message = try {
                    json.decodeFromString<ErrorResponse>(responseBody).message
                } catch (_: Exception) {
                    "Server error"
                }
                UprateError.ServerError(statusCode, message)
            }
            else -> UprateError.UnexpectedResponse(statusCode)
        }
    }
}
