package com.upratehq.sdk

import com.upratehq.sdk.features.reviews.UprateReviews
import com.upratehq.sdk.networking.APIClient
import com.upratehq.sdk.networking.UprateError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.*

class UprateReviewsTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var reviews: UprateReviews

    @BeforeTest
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        val config = UprateConfiguration(apiKey = testApiKey, baseURL = mockServer.url("/api/sdk/v1").toString())
        val apiClient = APIClient(config, OkHttpClient(), "1.0.0")
        apiClient.userContext = UserContext(userId = "test-user")
        reviews = UprateReviews(apiClient, context = null)
    }

    @AfterTest
    fun teardown() { mockServer.shutdown() }

    @Test
    fun recordPromptReturnsResult() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"uuid":"sig-1","status":"recorded","expires_at":"2025-02-15T10:30:00Z"}"""))
        val result = reviews.recordPrompt()
        assertEquals("sig-1", result.id)
        assertEquals("recorded", result.status)
        assertEquals("2025-02-15T10:30:00Z", result.expiresAt)
    }

    @Test
    fun recordPromptUsesPostMethod() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"uuid":"sig-1","status":"recorded","expires_at":"2025-02-15T10:30:00Z"}"""))
        reviews.recordPrompt()
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.endsWith("/review-signals"))
    }

    @Test
    fun recordPromptSendsBody() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"uuid":"sig-1","status":"recorded","expires_at":"2025-02-15T10:30:00Z"}"""))
        reviews.recordPrompt()
        val request = mockServer.takeRequest()
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertTrue(body.containsKey("triggered_at"))
        assertTrue(body.containsKey("metadata"))
    }

    @Test
    fun recordPromptRateLimited() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "30").setBody("""{"message":"Too many requests"}"""))
        val error = assertFailsWith<UprateError.RateLimited> { reviews.recordPrompt() }
        assertEquals(30, error.retryAfter!!.inWholeSeconds)
    }

    @Test
    fun recordPromptNoUserContext() = runTest {
        val config = UprateConfiguration(apiKey = testApiKey, baseURL = mockServer.url("/api/sdk/v1").toString())
        val apiClient = APIClient(config, OkHttpClient(), "1.0.0")
        val reviewsNoContext = UprateReviews(apiClient, context = null)
        mockServer.enqueue(MockResponse().setBody("""{"uuid":"sig-1","status":"recorded","expires_at":"2025-02-15T10:30:00Z"}"""))
        assertFailsWith<UprateError.UserContextNotSet> { reviewsNoContext.recordPrompt() }
    }
}
