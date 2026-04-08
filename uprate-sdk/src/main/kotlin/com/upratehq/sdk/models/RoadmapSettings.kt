package com.upratehq.sdk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoadmapSettings(
    @SerialName("voting_enabled") val votingEnabled: Boolean,
    @SerialName("show_vote_count") val showVoteCount: Boolean,
    @SerialName("voting_excluded_statuses") val votingExcludedStatuses: List<String>
)
