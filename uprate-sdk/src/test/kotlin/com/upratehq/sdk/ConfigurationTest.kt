package com.upratehq.sdk

import com.upratehq.sdk.networking.UprateError
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.*

class ConfigurationTest {

    @AfterTest
    fun teardown() {
        UprateSDK.reset()
    }

    @Test
    fun instanceThrowsBeforeConfigure() {
        assertFailsWith<UprateError.NotInitialized> {
            UprateSDK.instance
        }
    }

    @Test
    fun instanceAvailableAfterConfigure() {
        val sdk = UprateSDK(
            configuration = testConfiguration,
            client = OkHttpClient()
        )
        assertNotNull(sdk.roadmap)
        assertNotNull(sdk.feedback)
        assertNotNull(sdk.reviews)
    }

    @Test
    fun userContextLifecycle() = runTest {
        val sdk = UprateSDK(
            configuration = testConfiguration,
            client = OkHttpClient()
        )
        sdk.setUserContext(userId = "user-1", email = "user@test.com", name = "Test")
        sdk.clearUserContext()
    }

    @Test
    fun customBaseURLUsed() = runTest {
        val mockServer = MockWebServer()
        mockServer.start()

        val config = UprateConfiguration(
            apiKey = testApiKey,
            baseURL = mockServer.url("/custom/api").toString()
        )
        val sdk = UprateSDK(configuration = config, client = OkHttpClient())
        sdk.setUserContext(userId = "user-1")

        mockServer.enqueue(MockResponse().setBody("""
        {"settings":{"voting_enabled":true,"show_vote_count":true,"voting_excluded_statuses":[]},"items":[]}
        """))

        sdk.roadmap.getItems()
        val request = mockServer.takeRequest()
        assertTrue(request.path!!.startsWith("/custom/api/roadmap"))

        mockServer.shutdown()
    }

    @Test
    fun resetClearsSingleton() {
        UprateSDK.configureForTesting(
            configuration = testConfiguration,
            client = OkHttpClient()
        )
        assertNotNull(UprateSDK.instance)

        UprateSDK.reset()
        assertFailsWith<UprateError.NotInitialized> {
            UprateSDK.instance
        }
    }
}
