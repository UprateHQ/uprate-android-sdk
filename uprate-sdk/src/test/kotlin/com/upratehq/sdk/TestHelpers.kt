package com.upratehq.sdk

import kotlinx.serialization.json.Json

val testApiKey = "uprt_pub_${"a".repeat(64)}"
const val testBaseURL = "https://test.upratehq.com/api/sdk/v1"
val testConfiguration = UprateConfiguration(apiKey = testApiKey, baseURL = testBaseURL)

val uprateJson = Json { ignoreUnknownKeys = true }
