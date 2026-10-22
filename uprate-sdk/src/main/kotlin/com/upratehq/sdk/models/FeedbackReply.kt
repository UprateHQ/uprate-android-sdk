package com.upratehq.sdk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FeedbackReply(
    @SerialName("uuid") val id: String,
    @SerialName("feedback_uuid") val feedbackId: String,
    val body: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("read_at") val readAt: String? = null
)

@Serializable
internal data class FeedbackRepliesResponse(val replies: List<FeedbackReply>)

@Serializable
internal data class MarkRepliesReadRequestBody(val uuids: List<String>)
