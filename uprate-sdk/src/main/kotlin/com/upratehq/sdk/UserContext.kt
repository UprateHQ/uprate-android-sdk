package com.upratehq.sdk

data class UserContext(
    val userId: String,
    val email: String? = null,
    val name: String? = null
)
