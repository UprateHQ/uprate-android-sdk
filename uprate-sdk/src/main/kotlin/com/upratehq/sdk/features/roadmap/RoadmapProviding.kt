package com.upratehq.sdk.features.roadmap

import com.upratehq.sdk.models.FeatureRequest
import com.upratehq.sdk.models.UprateRoadmapResponse
import com.upratehq.sdk.models.VoteResult

interface RoadmapProviding {
    suspend fun getItems(): UprateRoadmapResponse
    suspend fun vote(itemId: String): VoteResult
    suspend fun removeVote(itemId: String): VoteResult
    suspend fun submitRequest(title: String, description: String? = null): FeatureRequest
    suspend fun getMyRequests(): List<FeatureRequest>
}
