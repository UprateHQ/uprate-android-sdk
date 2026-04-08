package com.upratehq.sdk

import com.upratehq.sdk.features.feedback.UprateFeedback
import com.upratehq.sdk.networking.APIClient
import com.upratehq.sdk.networking.UprateError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.*

class UprateFeedbackTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var feedback: UprateFeedback

    @BeforeTest
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        val config = UprateConfiguration(apiKey = testApiKey, baseURL = mockServer.url("/api/sdk/v1").toString())
        val apiClient = APIClient(config, OkHttpClient(), "1.0.0")
        apiClient.userContext = UserContext(userId = "test-user")
        feedback = UprateFeedback(apiClient, context = null)
        feedback.collectDeviceMetadata = false
    }

    @AfterTest
    fun teardown() { mockServer.shutdown() }

    @Test
    fun submitReturnsFeedbackResult() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"uuid":"fb-1","rating":4,"message":"Great app!","status":"new","created_at":"2025-01-15T10:30:00Z"}"""))
        val result = feedback.submit("Great app!", rating = 4)
        assertEquals("fb-1", result.id)
        assertEquals(4, result.rating)
        assertEquals("Great app!", result.message)
    }

    @Test
    fun submitMessageOnly() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"uuid":"fb-1","message":"Feedback","status":"new","created_at":"2025-01-15T10:30:00Z"}"""))
        val result = feedback.submit("Feedback")
        assertEquals("Feedback", result.message)
        assertNull(result.rating)
    }

    @Test
    fun submitUsesPostMethod() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"uuid":"fb-1","message":"Test","status":"new","created_at":"2025-01-15T10:30:00Z"}"""))
        feedback.submit("Test")
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.endsWith("/feedback"))
    }

    @Test
    fun submitWithMetadata() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"uuid":"fb-1","message":"Bug","status":"new","created_at":"2025-01-15T10:30:00Z"}"""))
        feedback.submit("Bug", metadata = mapOf("page" to "settings", "version" to "2.0"))
        val request = mockServer.takeRequest()
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        val metadata = body["metadata"]!!.jsonObject
        val custom = metadata["custom"]!!.jsonObject
        assertEquals("\"settings\"", custom["page"].toString())
        assertEquals("\"2.0\"", custom["version"].toString())
    }

    @Test
    fun submitEmptyMessageThrows() = runTest {
        val error = assertFailsWith<UprateError.ValidationError> { feedback.submit("") }
        assertTrue(error.errors.containsKey("message"))
    }

    @Test
    fun submitBlankMessageThrows() = runTest {
        val error = assertFailsWith<UprateError.ValidationError> { feedback.submit("   ") }
        assertTrue(error.errors.containsKey("message"))
    }

    @Test
    fun submitMessageTooLongThrows() = runTest {
        val error = assertFailsWith<UprateError.ValidationError> { feedback.submit("a".repeat(5001)) }
        assertTrue(error.errors.containsKey("message"))
    }

    @Test
    fun submitRatingZeroThrows() = runTest {
        val error = assertFailsWith<UprateError.ValidationError> { feedback.submit("Good", rating = 0) }
        assertTrue(error.errors.containsKey("rating"))
    }

    @Test
    fun submitRatingSixThrows() = runTest {
        val error = assertFailsWith<UprateError.ValidationError> { feedback.submit("Good", rating = 6) }
        assertTrue(error.errors.containsKey("rating"))
    }

    @Test
    fun submitValidRatingBounds() = runTest {
        for (rating in 1..5) {
            mockServer.enqueue(MockResponse().setBody("""{"uuid":"fb-1","rating":$rating,"message":"OK","status":"new","created_at":"2025-01-15T10:30:00Z"}"""))
            val result = feedback.submit("OK", rating = rating)
            assertEquals(rating, result.rating)
        }
    }

    @Test
    fun submitRateLimited() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(429).addHeader("Retry-After", "60").setBody("""{"message":"Too many requests"}"""))
        val error = assertFailsWith<UprateError.RateLimited> { feedback.submit("Test") }
        assertEquals(60, error.retryAfter!!.inWholeSeconds)
    }

    @Test
    fun getMySubmissionsReturnsList() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"feedback":[{"uuid":"sub-1","rating":5,"message":"Great","sentiment":"positive","created_at":"2025-01-15T10:30:00Z"},{"uuid":"sub-2","message":"OK","created_at":"2025-01-16T10:30:00Z"}]}"""))
        val submissions = feedback.getMySubmissions()
        assertEquals(2, submissions.size)
        assertEquals("positive", submissions[0].sentiment)
        assertNull(submissions[1].rating)
    }

    @Test
    fun getMySubmissionsEmpty() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"feedback":[]}"""))
        assertEquals(0, feedback.getMySubmissions().size)
    }

    @Test
    fun getMySubmissionsUsesGetMethod() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"feedback":[]}"""))
        feedback.getMySubmissions()
        val request = mockServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.endsWith("/feedback"))
    }

    @Test
    fun deviceMetadataDisabledOmitsDevice() = runTest {
        feedback.collectDeviceMetadata = false
        mockServer.enqueue(MockResponse().setBody("""{"uuid":"fb-1","message":"Test","status":"new","created_at":"2025-01-15T10:30:00Z"}"""))
        feedback.submit("Test")
        val request = mockServer.takeRequest()
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        val metadata = body["metadata"]
        if (metadata != null) {
            val device = metadata.jsonObject["device"]
            assertTrue(device == null || device.toString() == "null")
        }
    }
}
