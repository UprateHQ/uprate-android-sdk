package com.upratehq.sdk.networking

import kotlin.time.Duration
import kotlinx.serialization.Serializable

sealed class UprateError : Exception() {

    data object NotInitialized : UprateError() {
        private fun readResolve(): Any = NotInitialized
        override val message: String get() = "UprateSDK has not been configured. Call UprateSDK.configure() first."
    }

    data object UserContextNotSet : UprateError() {
        private fun readResolve(): Any = UserContextNotSet
        override val message: String get() = "User context has not been set. Call setUserContext() first."
    }

    data object InvalidApiKey : UprateError() {
        private fun readResolve(): Any = InvalidApiKey
        override val message: String get() = "Invalid API key. Check your API key and try again."
    }

    data object FeatureNotEnabled : UprateError() {
        private fun readResolve(): Any = FeatureNotEnabled
        override val message: String get() = "This feature is not enabled for your account."
    }

    data object NotFound : UprateError() {
        private fun readResolve(): Any = NotFound
        override val message: String get() = "The requested resource was not found."
    }

    data class ValidationError(
        override val message: String,
        val errors: Map<String, List<String>>
    ) : UprateError()

    data class RateLimited(val retryAfter: Duration?) : UprateError() {
        override val message: String get() = "Rate limited. Please try again later."
    }

    data class NetworkError(val underlying: Throwable) : UprateError() {
        override val message: String get() = "Network error: ${underlying.message}"
    }

    data class ServerError(val statusCode: Int, override val message: String) : UprateError()

    data class UnexpectedResponse(val statusCode: Int) : UprateError() {
        override val message: String get() = "Unexpected response with status code $statusCode."
    }
}

@Serializable
internal data class ErrorResponse(val message: String)

@Serializable
internal data class ValidationErrorResponse(
    val message: String,
    val errors: Map<String, List<String>>
)
