package com.upratehq.sdk.networking

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.Locale
import java.util.TimeZone

@Serializable
internal data class DeviceMetadata(
    val model: String,
    @SerialName("os_version") val osVersion: String,
    @SerialName("app_version") val appVersion: String,
    @SerialName("build_number") val buildNumber: String,
    val locale: String,
    val timezone: String,
    @SerialName("total_ram_mb") val totalRamMb: Long,
    @SerialName("free_disk_space_mb") val freeDiskSpaceMb: Long
) {
    companion object {
        fun collect(context: Context): DeviceMetadata {
            val packageInfo = try {
                context.packageManager.getPackageInfo(context.packageName, 0)
            } catch (_: Exception) {
                null
            }

            val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
            val memoryInfo = ActivityManager.MemoryInfo()
            activityManager?.getMemoryInfo(memoryInfo)

            val stat = StatFs(Environment.getDataDirectory().path)
            val freeDiskMb = stat.availableBytes / (1024 * 1024)

            return DeviceMetadata(
                model = Build.MODEL,
                osVersion = Build.VERSION.RELEASE,
                appVersion = packageInfo?.versionName ?: "unknown",
                buildNumber = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    (packageInfo?.longVersionCode ?: 0).toString()
                } else {
                    @Suppress("DEPRECATION")
                    (packageInfo?.versionCode ?: 0).toString()
                },
                locale = Locale.getDefault().toLanguageTag(),
                timezone = TimeZone.getDefault().id,
                totalRamMb = memoryInfo.totalMem / (1024 * 1024),
                freeDiskSpaceMb = freeDiskMb
            )
        }
    }
}
