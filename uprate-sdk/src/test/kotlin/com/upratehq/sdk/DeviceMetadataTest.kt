package com.upratehq.sdk

import com.upratehq.sdk.networking.DeviceMetadata
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeviceMetadataTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val testMetadata = DeviceMetadata(
        model = "Pixel 7",
        osVersion = "14",
        appVersion = "2.1.0",
        buildNumber = "42",
        locale = "en-US",
        timezone = "America/New_York",
        totalRamMb = 8192,
        freeDiskSpaceMb = 32768
    )

    @Test
    fun encodesToJson() {
        val encoded = json.encodeToString(testMetadata)
        val obj = Json.parseToJsonElement(encoded).jsonObject
        assertEquals("\"Pixel 7\"", obj["model"].toString())
        assertEquals("\"14\"", obj["os_version"].toString())
        assertEquals("\"2.1.0\"", obj["app_version"].toString())
        assertEquals("\"42\"", obj["build_number"].toString())
        assertEquals("\"en-US\"", obj["locale"].toString())
        assertEquals("\"America/New_York\"", obj["timezone"].toString())
        assertEquals("8192", obj["total_ram_mb"].toString())
        assertEquals("32768", obj["free_disk_space_mb"].toString())
    }

    @Test
    fun decodesFromJson() {
        val jsonStr = """
        {
            "model": "Pixel 7",
            "os_version": "14",
            "app_version": "2.1.0",
            "build_number": "42",
            "locale": "en-US",
            "timezone": "America/New_York",
            "total_ram_mb": 8192,
            "free_disk_space_mb": 32768
        }
        """
        val decoded = json.decodeFromString<DeviceMetadata>(jsonStr)
        assertEquals("Pixel 7", decoded.model)
        assertEquals("14", decoded.osVersion)
        assertEquals("2.1.0", decoded.appVersion)
        assertEquals("42", decoded.buildNumber)
        assertEquals("en-US", decoded.locale)
        assertEquals("America/New_York", decoded.timezone)
        assertEquals(8192, decoded.totalRamMb)
        assertEquals(32768, decoded.freeDiskSpaceMb)
    }

    @Test
    fun roundTripSerialization() {
        val encoded = json.encodeToString(testMetadata)
        val decoded = json.decodeFromString<DeviceMetadata>(encoded)
        assertEquals(testMetadata, decoded)
    }
}
