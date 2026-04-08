package com.upratehq.sdk.features.feedback

import com.upratehq.sdk.models.FeedbackResult
import com.upratehq.sdk.models.FeedbackSubmission

interface FeedbackProviding {
    var collectDeviceMetadata: Boolean
    suspend fun submit(
        message: String,
        rating: Int? = null,
        metadata: Map<String, Any>? = null
    ): FeedbackResult
    suspend fun getMySubmissions(): List<FeedbackSubmission>
}
