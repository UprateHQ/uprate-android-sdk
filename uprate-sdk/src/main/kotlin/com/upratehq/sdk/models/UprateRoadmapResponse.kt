package com.upratehq.sdk.models

import kotlinx.serialization.Serializable

@Serializable
data class UprateRoadmapResponse(
    val settings: RoadmapSettings,
    val items: List<RoadmapItem>
)
