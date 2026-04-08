package com.upratehq.sdk.features.reviews

import android.content.Context
import android.os.Build
import com.upratehq.sdk.models.ReviewSignalRequestBody
import com.upratehq.sdk.models.ReviewSignalMetadataEnvelope
import com.upratehq.sdk.models.ReviewSignalResult
import com.upratehq.sdk.networking.APIClient
import com.upratehq.sdk.networking.APIEndpoint
import com.upratehq.sdk.networking.DeviceMetadata
import kotlinx.serialization.encodeToString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class UprateReviews internal constructor(
    private val apiClient: APIClient,
    private val context: Context?
) : ReviewSignalProviding {

    override suspend fun recordPrompt(): ReviewSignalResult {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        dateFormat.timeZone = TimeZone.getTimeZone("UTC")
        val triggeredAt = dateFormat.format(Date())

        val deviceMetadata = if (context != null) {
            DeviceMetadata.collect(context)
        } else {
            DeviceMetadata(
                model = Build.MODEL ?: "unknown",
                osVersion = Build.VERSION.RELEASE ?: "unknown",
                appVersion = "unknown",
                buildNumber = "0",
                locale = Locale.getDefault().toLanguageTag(),
                timezone = TimeZone.getDefault().id,
                totalRamMb = 0,
                freeDiskSpaceMb = 0
            )
        }

        val body = apiClient.json.encodeToString(
            ReviewSignalRequestBody(
                triggeredAt = triggeredAt,
                metadata = ReviewSignalMetadataEnvelope(device = deviceMetadata)
            )
        )
        return apiClient.execute(APIEndpoint.RecordPrompt, body)
    }
}
