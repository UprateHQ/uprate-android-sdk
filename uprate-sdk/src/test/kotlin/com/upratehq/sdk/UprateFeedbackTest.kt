package com.upratehq.sdk

import com.upratehq.sdk.features.feedback.UprateFeedback
import com.upratehq.sdk.features.feedback.FeedbackProviding
import com.upratehq.sdk.models.FeedbackReply
import com.upratehq.sdk.networking.APIClient
import com.upratehq.sdk.networking.UprateError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.RecordedRequest
import kotlin.test.*

class UprateFeedbackTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var feedback: UprateFeedback
    private lateinit var apiClient: APIClient
    private val reply = FeedbackReply("reply-1", "sub-1", "Fixed", "2026-10-22T10:00:00+00:00")

    @BeforeTest
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        val config = UprateConfiguration(apiKey = testApiKey, baseURL = mockServer.url("/api/sdk/v1").toString())
        apiClient = APIClient(config, OkHttpClient(), "1.0.0")
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
        assertEquals(emptyList(), submissions[0].replies)
        assertEquals(emptyList(), submissions[1].replies)
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
    fun getMySubmissionsIncludesReplies() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"feedback":[{"uuid":"sub-1","message":"Slow checkout","created_at":"2026-10-21T10:00:00+00:00","replies":[{"uuid":"reply-1","feedback_uuid":"sub-1","body":"Fixed","created_at":"2026-10-22T10:00:00+00:00","read_at":null}]}]}"""))
        assertEquals(listOf(reply), feedback.getMySubmissions().single().replies)
    }

    @Test
    fun getUnreadRepliesUsesGetAndDecodesReplies() = runTest {
        apiClient.userContext = UserContext("test-user", userToken = "signed-token")
        mockServer.enqueue(MockResponse().setBody("""{"replies":[{"uuid":"reply-1","feedback_uuid":"sub-1","body":"Fixed","created_at":"2026-10-22T10:00:00+00:00","read_at":null}]}"""))
        val provider: FeedbackProviding = feedback
        assertEquals(listOf(reply), provider.getUnreadReplies())
        val request = mockServer.takeRequest()
        assertEquals("GET", request.method)
        assertEquals("/api/sdk/v1/replies/unread", request.path)
        assertEquals(0L, request.bodySize)
        assertEquals("signed-token", request.getHeader("X-SDK-User-Token"))
    }

    @Test
    fun getUnreadRepliesEmpty() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"replies":[]}"""))
        assertEquals(emptyList(), feedback.getUnreadReplies())
    }

    @Test
    fun markReadUsesPostWithReplyIds() = runTest {
        apiClient.userContext = UserContext("test-user", userToken = "signed-token")
        mockServer.enqueue(MockResponse().setBody("""{"replies":[{"uuid":"reply-1","feedback_uuid":"sub-1","body":"Fixed","created_at":"2026-10-22T10:00:00+00:00","read_at":"2026-10-22T11:00:00+00:00"}]}"""))
        val provider: FeedbackProviding = feedback
        provider.markRead(listOf(reply, reply.copy(id = "reply-2")))
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/sdk/v1/replies/read", request.path)
        assertEquals("""{"uuids":["reply-1","reply-2"]}""", request.body.readUtf8())
        assertEquals("signed-token", request.getHeader("X-SDK-User-Token"))
    }

    @Test
    fun markReadAcceptsMissingReplyIdsInResponse() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"replies":[]}"""))
        feedback.markRead(listOf(reply))
    }

    @Test
    fun markReadEmptyMakesNoRequestEvenWithoutUserContext() = runTest {
        apiClient.userContext = null
        feedback.markRead(emptyList())
        assertEquals(0, mockServer.requestCount)
    }

    @Test
    fun repliesRequireUserContext() = runTest {
        apiClient.userContext = null
        assertFailsWith<UprateError.UserContextNotSet> { feedback.getUnreadReplies() }
        assertFailsWith<UprateError.UserContextNotSet> { feedback.markRead(listOf(reply)) }
        assertEquals(0, mockServer.requestCount)
    }

    @Test
    fun getUnreadRepliesUsesSnapshotBeforeInterceptorRuns() = runTest {
        val originalContext = UserContext("test-user", "test@example.com", "Test User", "original-token")
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            apiClient.userContext = UserContext("other-user", userToken = "other-token")
            chain.proceed(chain.request())
        }.build()
        apiClient = APIClient(apiClient.configuration, client)
        apiClient.userContext = originalContext
        feedback = UprateFeedback(apiClient, context = null)
        mockServer.enqueue(MockResponse().setBody("""{"replies":[]}"""))

        assertFailsWith<UprateError.UserContextChanged> { feedback.getUnreadReplies() }
        val request = mockServer.takeRequest()
        assertEquals("test-user", request.getHeader("X-SDK-User-Id"))
        assertEquals("test@example.com", request.getHeader("X-SDK-User-Email"))
        assertEquals("Test User", request.getHeader("X-SDK-User-Name"))
        assertEquals("original-token", request.getHeader("X-SDK-User-Token"))
    }

    @Test
    fun markReadUsesSnapshotBeforeInterceptorRuns() = runTest {
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            apiClient.userContext = UserContext("other-user", userToken = "other-token")
            chain.proceed(chain.request())
        }.build()
        apiClient = APIClient(apiClient.configuration, client)
        apiClient.userContext = UserContext("test-user", userToken = "original-token")
        feedback = UprateFeedback(apiClient, context = null)
        mockServer.enqueue(MockResponse().setBody("""{"replies":[]}"""))

        assertFailsWith<UprateError.UserContextChanged> { feedback.markRead(listOf(reply)) }
        val request = mockServer.takeRequest()
        assertEquals("test-user", request.getHeader("X-SDK-User-Id"))
        assertEquals("original-token", request.getHeader("X-SDK-User-Token"))
    }

    @Test
    fun historyAndReplyCallsRejectUserContextChangesDuringRequest() = runTest {
        val originalContext = UserContext("test-user", "test@example.com", "Test User", "original-token")
        val calls: List<suspend () -> Unit> = listOf(
            { feedback.getMySubmissions() },
            { feedback.getUnreadReplies() },
            { feedback.markRead(listOf(reply)) }
        )
        for (call in calls) {
            for (changedContext in listOf(
                null,
                originalContext.copy(userId = "other-user"),
                originalContext.copy(email = "other@example.com"),
                originalContext.copy(name = "Other Name"),
                originalContext.copy(userToken = "other-token")
            )) {
                apiClient.userContext = originalContext
                mockServer.dispatcher = object : Dispatcher() {
                    override fun dispatch(request: RecordedRequest): MockResponse {
                        apiClient.userContext = changedContext
                        return MockResponse().setBody("""{"feedback":[],"replies":[]}""")
                    }
                }
                assertFailsWith<UprateError.UserContextChanged> { call() }
            }
        }
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
