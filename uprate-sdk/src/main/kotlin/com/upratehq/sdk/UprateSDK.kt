package com.upratehq.sdk

import android.annotation.SuppressLint
import android.content.Context
import com.upratehq.sdk.features.feedback.UprateFeedback
import com.upratehq.sdk.features.roadmap.UprateRoadmap
import com.upratehq.sdk.features.reviews.UprateReviews
import com.upratehq.sdk.networking.APIClient
import com.upratehq.sdk.networking.UprateError
import okhttp3.OkHttpClient

class UprateSDK internal constructor(
    private val apiClient: APIClient,
    private val context: Context?
) {
    val roadmap: UprateRoadmap = UprateRoadmap(apiClient)
    val feedback: UprateFeedback = UprateFeedback(apiClient, context)
    val reviews: UprateReviews = UprateReviews(apiClient, context)

    constructor(
        configuration: UprateConfiguration,
        client: OkHttpClient = OkHttpClient()
    ) : this(
        apiClient = APIClient(configuration, client),
        context = null
    )

    fun setUserContext(userId: String, email: String? = null, name: String? = null) {
        apiClient.userContext = UserContext(userId, email, name)
    }

    fun clearUserContext() {
        apiClient.userContext = null
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var _instance: UprateSDK? = null

        val instance: UprateSDK
            get() = _instance ?: throw UprateError.NotInitialized

        fun configure(context: Context, apiKey: String, baseURL: String? = null) {
            synchronized(this) {
                val appContext = context.applicationContext
                val config = UprateConfiguration(
                    apiKey = apiKey,
                    baseURL = baseURL ?: "https://app.upratehq.com/api/sdk/v1"
                )
                val appVersion = try {
                    appContext.packageManager
                        .getPackageInfo(appContext.packageName, 0)
                        .versionName ?: "unknown"
                } catch (_: Exception) {
                    "unknown"
                }
                val apiClient = APIClient(config, OkHttpClient(), appVersion)
                _instance = UprateSDK(apiClient, appContext)
            }
        }

        fun reset() {
            synchronized(this) {
                _instance = null
            }
        }

        internal fun configureForTesting(
            configuration: UprateConfiguration,
            client: OkHttpClient = OkHttpClient()
        ) {
            synchronized(this) {
                val apiClient = APIClient(configuration, client)
                _instance = UprateSDK(apiClient, null)
            }
        }
    }
}
