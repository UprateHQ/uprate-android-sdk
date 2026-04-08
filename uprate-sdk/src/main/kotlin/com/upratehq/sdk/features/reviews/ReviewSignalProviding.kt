package com.upratehq.sdk.features.reviews

import com.upratehq.sdk.models.ReviewSignalResult

interface ReviewSignalProviding {
    suspend fun recordPrompt(): ReviewSignalResult
}
