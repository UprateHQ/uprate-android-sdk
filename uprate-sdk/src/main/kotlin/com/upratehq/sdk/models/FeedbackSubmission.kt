package com.upratehq.sdk.models

import com.upratehq.sdk.networking.DeviceMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class FeedbackSubmission(
    @SerialName("uuid") val id: String,
    val rating: Int? = null,
    val message: String,
    val sentiment: String? = null,
    @SerialName("created_at") val createdAt: String
)

@Serializable
internal data class FeedbackRequestBody(
    val message: String,
    val rating: Int? = null,
    val metadata: FeedbackMetadataEnvelope? = null
)

@Serializable
internal data class FeedbackMetadataEnvelope(
    val device: DeviceMetadata? = null,
    val custom: Map<String, JsonPrimitive>? = null
)

@Serializable
internal data class FeedbackListResponse(
    val feedback: List<FeedbackSubmission>
)
