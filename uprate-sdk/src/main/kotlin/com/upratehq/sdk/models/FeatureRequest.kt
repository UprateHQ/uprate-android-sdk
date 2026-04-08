package com.upratehq.sdk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeatureRequest(
    @SerialName("uuid") val id: String,
    val title: String,
    val description: String? = null,
    val status: String,
    @SerialName("created_at") val createdAt: String
)

@Serializable
internal data class FeatureRequestBody(
    val title: String,
    val description: String? = null
)

@Serializable
internal data class FeatureRequestListResponse(
    val requests: List<FeatureRequest>
)
