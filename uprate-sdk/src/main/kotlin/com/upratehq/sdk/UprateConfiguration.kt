package com.upratehq.sdk

data class UprateConfiguration(
    val apiKey: String,
    val baseURL: String = "https://app.upratehq.com/api/sdk/v1"
)
