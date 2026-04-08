package com.upratehq.sdk.networking

internal sealed class APIEndpoint(val path: String, val method: String) {
    data object GetRoadmap : APIEndpoint("/roadmap", "GET")
    data class VoteItem(val itemId: String) : APIEndpoint("/roadmap/items/$itemId/vote", "POST")
    data class RemoveVote(val itemId: String) : APIEndpoint("/roadmap/items/$itemId/vote", "DELETE")
    data object SubmitRequest : APIEndpoint("/roadmap/requests", "POST")
    data object GetMyRequests : APIEndpoint("/roadmap/requests", "GET")
    data object SubmitFeedback : APIEndpoint("/feedback", "POST")
    data object GetMyFeedback : APIEndpoint("/feedback", "GET")
    data object RecordPrompt : APIEndpoint("/review-signals", "POST")
}
