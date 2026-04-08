package com.upratehq.sdk.models

import com.upratehq.sdk.networking.DeviceMetadata
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewSignalResult(
    @SerialName("uuid") val id: String,
    val status: String,
    @SerialName("expires_at") val expiresAt: String
)

@Serializable
internal data class ReviewSignalRequestBody(
    @SerialName("triggered_at") val triggeredAt: String,
    val metadata: ReviewSignalMetadataEnvelope
)

@Serializable
internal data class ReviewSignalMetadataEnvelope(
    val device: DeviceMetadata
)
