package com.upratehq.sdk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoadmapItem(
    @SerialName("uuid") val id: String,
    val title: String,
    val description: String? = null,
    val status: String,
    @SerialName("status_label") val statusLabel: String,
    @SerialName("votes_count") val votesCount: Int? = null,
    @SerialName("has_voted") val hasVoted: Boolean,
    @SerialName("voting_disabled") val votingDisabled: Boolean
)
