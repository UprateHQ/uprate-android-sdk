package com.upratehq.sdk.features.feedback

import android.content.Context
import com.upratehq.sdk.models.*
import com.upratehq.sdk.networking.APIClient
import com.upratehq.sdk.networking.APIEndpoint
import com.upratehq.sdk.networking.DeviceMetadata
import com.upratehq.sdk.networking.UprateError
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonPrimitive

class UprateFeedback internal constructor(
    private val apiClient: APIClient,
    private val context: Context?
) : FeedbackProviding {

    override var collectDeviceMetadata: Boolean = true

    override suspend fun submit(
        message: String,
        rating: Int?,
        metadata: Map<String, Any>?
    ): FeedbackResult {
        validate(message, rating)

        val customMetadata = metadata?.mapValues { (_, value) ->
            when (value) {
                is String -> JsonPrimitive(value)
                is Int -> JsonPrimitive(value)
                is Long -> JsonPrimitive(value)
                is Double -> JsonPrimitive(value)
                is Float -> JsonPrimitive(value.toDouble())
                is Boolean -> JsonPrimitive(value)
                else -> JsonPrimitive(value.toString())
            }
        }

        val deviceMetadata = if (collectDeviceMetadata && context != null) {
            DeviceMetadata.collect(context)
        } else {
            null
        }

        val envelope = if (deviceMetadata != null || customMetadata != null) {
            FeedbackMetadataEnvelope(device = deviceMetadata, custom = customMetadata)
        } else {
            null
        }

        val body = apiClient.json.encodeToString(
            FeedbackRequestBody(message = message, rating = rating, metadata = envelope)
        )
        return apiClient.execute(APIEndpoint.SubmitFeedback, body)
    }

    override suspend fun getMySubmissions(): List<FeedbackSubmission> {
        val response: FeedbackListResponse = apiClient.execute(APIEndpoint.GetMyFeedback)
        return response.feedback
    }

    private fun validate(message: String, rating: Int?) {
        val errors = mutableMapOf<String, List<String>>()
        if (message.isBlank()) {
            errors["message"] = listOf("Message is required")
        } else if (message.length > 5000) {
            errors["message"] = listOf("Message must be 5000 characters or less")
        }
        if (rating != null && rating !in 1..5) {
            errors["rating"] = listOf("Rating must be between 1 and 5")
        }
        if (errors.isNotEmpty()) {
            throw UprateError.ValidationError(message = "Validation failed", errors = errors)
        }
    }
}
