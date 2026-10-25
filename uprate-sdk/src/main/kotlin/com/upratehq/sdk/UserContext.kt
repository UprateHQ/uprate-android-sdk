package com.upratehq.sdk

data class UserContext @JvmOverloads constructor(
    val userId: String,
    val email: String? = null,
    val name: String? = null,
    val userToken: String? = null
) {
    // Requests carry this as a tag, and OkHttp prints tags wherever an app logs a request.
    override fun toString(): String =
        "UserContext(userId=$userId, email=$email, name=$name, userToken=${userToken?.let { "<redacted>" }})"
}
