package com.upratehq.sdk.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class VoteResult(
    val voted: Boolean,
    @SerialName("votes_count") val votesCount: Int
)
