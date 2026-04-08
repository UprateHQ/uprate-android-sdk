package com.upratehq.sdk

import com.upratehq.sdk.networking.APIClient
import com.upratehq.sdk.networking.APIEndpoint
import com.upratehq.sdk.networking.UprateError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.*

class APIClientTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var apiClient: APIClient

    @BeforeTest
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        val config = UprateConfiguration(
            apiKey = testApiKey,
            baseURL = mockServer.url("/api/sdk/v1").toString()
        )
        apiClient = APIClient(
            configuration = config,
            client = OkHttpClient(),
            appVersion = "1.0.0"
        )
        apiClient.userContext = UserContext(userId = "test-user", email = "test@example.com", name = "Test User")
    }

    @AfterTest
    fun teardown() {
        mockServer.shutdown()
    }

    @Serializable
    data class TestResponse(val ok: Boolean)

    @Test
    fun throwsWithoutUserContext() = runTest {
        apiClient.userContext = null
        mockServer.enqueue(MockResponse().setBody("""{"ok": true}"""))

        assertFailsWith<UprateError.UserContextNotSet> {
            apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)
        }
    }

    @Test
    fun sendsAuthHeader() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"ok": true}"""))
        apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)

        val request = mockServer.takeRequest()
        assertEquals("Bearer $testApiKey", request.getHeader("Authorization"))
    }

    @Test
    fun sendsUserContextHeaders() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"ok": true}"""))
        apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)

        val request = mockServer.takeRequest()
        assertEquals("test-user", request.getHeader("X-SDK-User-Id"))
        assertEquals("test@example.com", request.getHeader("X-SDK-User-Email"))
        assertEquals("Test User", request.getHeader("X-SDK-User-Name"))
    }

    @Test
    fun sendsPlatformHeaders() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"ok": true}"""))
        apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)

        val request = mockServer.takeRequest()
        assertEquals("android", request.getHeader("X-SDK-Device-Platform"))
        assertEquals("1.0.0", request.getHeader("X-SDK-App-Version"))
    }

    @Test
    fun omitsOptionalHeaders() = runTest {
        apiClient.userContext = UserContext(userId = "test-user")
        mockServer.enqueue(MockResponse().setBody("""{"ok": true}"""))
        apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)

        val request = mockServer.takeRequest()
        assertEquals("test-user", request.getHeader("X-SDK-User-Id"))
        assertNull(request.getHeader("X-SDK-User-Email"))
        assertNull(request.getHeader("X-SDK-User-Name"))
    }

    @Test
    fun maps401ToInvalidApiKey() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(401).setBody("""{"message":"Unauthorized"}"""))

        assertFailsWith<UprateError.InvalidApiKey> {
            apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)
        }
    }

    @Test
    fun maps403ToFeatureNotEnabled() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(403).setBody("""{"message":"Forbidden"}"""))

        assertFailsWith<UprateError.FeatureNotEnabled> {
            apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)
        }
    }

    @Test
    fun maps404ToNotFound() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Not found"}"""))

        assertFailsWith<UprateError.NotFound> {
            apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)
        }
    }

    @Test
    fun maps422ToValidationError() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(422)
                .setBody("""{"message":"Validation failed","errors":{"title":["is required"]}}""")
        )

        val error = assertFailsWith<UprateError.ValidationError> {
            apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)
        }
        assertEquals("Validation failed", error.message)
        assertEquals(listOf("is required"), error.errors["title"])
    }

    @Test
    fun maps429ToRateLimited() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(429)
                .addHeader("Retry-After", "30")
                .setBody("""{"message":"Too many requests"}""")
        )

        val error = assertFailsWith<UprateError.RateLimited> {
            apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)
        }
        assertNotNull(error.retryAfter)
        assertEquals(30, error.retryAfter!!.inWholeSeconds)
    }

    @Test
    fun maps500ToServerError() = runTest {
        mockServer.enqueue(
            MockResponse()
                .setResponseCode(500)
                .setBody("""{"message":"Internal server error"}""")
        )

        val error = assertFailsWith<UprateError.ServerError> {
            apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)
        }
        assertEquals(500, error.statusCode)
        assertEquals("Internal server error", error.message)
    }

    @Test
    fun maps418ToUnexpectedResponse() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(418).setBody("""{"message":"I'm a teapot"}"""))
        val error = assertFailsWith<UprateError.UnexpectedResponse> {
            apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)
        }
        assertEquals(418, error.statusCode)
    }

    @Test
    fun clearUserContextCausesFailure() = runTest {
        apiClient.userContext = null
        mockServer.enqueue(MockResponse().setBody("""{"ok": true}"""))

        assertFailsWith<UprateError.UserContextNotSet> {
            apiClient.execute<TestResponse>(APIEndpoint.GetRoadmap)
        }
    }
}
