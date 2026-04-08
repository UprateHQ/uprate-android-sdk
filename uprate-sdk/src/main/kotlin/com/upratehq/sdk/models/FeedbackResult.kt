package com.upratehq.sdk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedbackResult(
    @SerialName("uuid") val id: String,
    val rating: Int? = null,
    val message: String,
    val status: String,
    @SerialName("created_at") val createdAt: String
)
