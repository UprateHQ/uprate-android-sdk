package com.upratehq.sdk.features.roadmap

import com.upratehq.sdk.models.*
import com.upratehq.sdk.networking.APIClient
import com.upratehq.sdk.networking.APIEndpoint
import kotlinx.serialization.encodeToString

class UprateRoadmap internal constructor(
    private val apiClient: APIClient
) : RoadmapProviding {

    override suspend fun getItems(): UprateRoadmapResponse {
        return apiClient.execute(APIEndpoint.GetRoadmap)
    }

    override suspend fun vote(itemId: String): VoteResult {
        return apiClient.execute(APIEndpoint.VoteItem(itemId))
    }

    override suspend fun removeVote(itemId: String): VoteResult {
        return apiClient.execute(APIEndpoint.RemoveVote(itemId))
    }

    override suspend fun submitRequest(title: String, description: String?): FeatureRequest {
        val body = apiClient.json.encodeToString(FeatureRequestBody(title, description))
        return apiClient.execute(APIEndpoint.SubmitRequest, body)
    }

    override suspend fun getMyRequests(): List<FeatureRequest> {
        val response: FeatureRequestListResponse = apiClient.execute(APIEndpoint.GetMyRequests)
        return response.requests
    }
}
