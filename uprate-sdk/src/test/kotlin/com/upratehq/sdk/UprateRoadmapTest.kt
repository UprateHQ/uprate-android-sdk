package com.upratehq.sdk

import com.upratehq.sdk.features.roadmap.UprateRoadmap
import com.upratehq.sdk.networking.APIClient
import com.upratehq.sdk.networking.UprateError
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlin.test.*

class UprateRoadmapTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var roadmap: UprateRoadmap

    @BeforeTest
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        val config = UprateConfiguration(
            apiKey = testApiKey,
            baseURL = mockServer.url("/api/sdk/v1").toString()
        )
        val apiClient = APIClient(config, OkHttpClient(), "1.0.0")
        apiClient.userContext = UserContext(userId = "test-user")
        roadmap = UprateRoadmap(apiClient)
    }

    @AfterTest
    fun teardown() {
        mockServer.shutdown()
    }

    @Test
    fun getItemsReturnsRoadmapResponse() = runTest {
        mockServer.enqueue(MockResponse().setBody("""
        {
            "settings": {"voting_enabled": true, "show_vote_count": true, "voting_excluded_statuses": ["completed"]},
            "items": [{"uuid": "item-1", "title": "Dark mode", "description": "Add dark mode", "status": "planned", "status_label": "Planned", "votes_count": 5, "has_voted": false, "voting_disabled": false}]
        }
        """))
        val response = roadmap.getItems()
        assertEquals(true, response.settings.votingEnabled)
        assertEquals(1, response.items.size)
        assertEquals("Dark mode", response.items[0].title)
        assertEquals(5, response.items[0].votesCount)
    }

    @Test
    fun getItemsUsesGetMethod() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"settings":{"voting_enabled":true,"show_vote_count":true,"voting_excluded_statuses":[]},"items":[]}"""))
        roadmap.getItems()
        val request = mockServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.endsWith("/roadmap"))
    }

    @Test
    fun voteReturnsVoteResult() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"voted": true, "votes_count": 6}"""))
        val result = roadmap.vote("item-1")
        assertEquals(true, result.voted)
        assertEquals(6, result.votesCount)
    }

    @Test
    fun voteUsesPostMethod() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"voted": true, "votes_count": 1}"""))
        roadmap.vote("item-1")
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.endsWith("/roadmap/items/item-1/vote"))
    }

    @Test
    fun removeVoteReturnsVoteResult() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"voted": false, "votes_count": 4}"""))
        val result = roadmap.removeVote("item-1")
        assertEquals(false, result.voted)
        assertEquals(4, result.votesCount)
    }

    @Test
    fun removeVoteUsesDeleteMethod() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"voted": false, "votes_count": 0}"""))
        roadmap.removeVote("item-1")
        val request = mockServer.takeRequest()
        assertEquals("DELETE", request.method)
        assertTrue(request.path!!.endsWith("/roadmap/items/item-1/vote"))
    }

    @Test
    fun submitRequestReturnsFeatureRequest() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"uuid": "req-1", "title": "New feature", "description": "Please add this", "status": "pending", "created_at": "2025-01-15T10:30:00Z"}"""))
        val result = roadmap.submitRequest("New feature", "Please add this")
        assertEquals("req-1", result.id)
        assertEquals("New feature", result.title)
        assertEquals("Please add this", result.description)
    }

    @Test
    fun submitRequestUsesPostWithBody() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"uuid":"req-1","title":"Feature","status":"pending","created_at":"2025-01-15T10:30:00Z"}"""))
        roadmap.submitRequest("Feature", "Details")
        val request = mockServer.takeRequest()
        assertEquals("POST", request.method)
        assertTrue(request.path!!.endsWith("/roadmap/requests"))
        val body = Json.parseToJsonElement(request.body.readUtf8())
        assertEquals("\"Feature\"", body.jsonObject["title"].toString())
        assertEquals("\"Details\"", body.jsonObject["description"].toString())
    }

    @Test
    fun getMyRequestsReturnsList() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"requests": [{"uuid":"req-1","title":"Feature A","status":"pending","created_at":"2025-01-15T10:30:00Z"},{"uuid":"req-2","title":"Feature B","status":"approved","created_at":"2025-01-16T10:30:00Z"}]}"""))
        val requests = roadmap.getMyRequests()
        assertEquals(2, requests.size)
        assertEquals("Feature A", requests[0].title)
        assertEquals("Feature B", requests[1].title)
    }

    @Test
    fun getMyRequestsUsesGetMethod() = runTest {
        mockServer.enqueue(MockResponse().setBody("""{"requests": []}"""))
        roadmap.getMyRequests()
        val request = mockServer.takeRequest()
        assertEquals("GET", request.method)
        assertTrue(request.path!!.endsWith("/roadmap/requests"))
    }

    @Test
    fun voteNotFoundThrows() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(404).setBody("""{"message":"Not found"}"""))
        assertFailsWith<UprateError.NotFound> { roadmap.vote("nonexistent") }
    }

    @Test
    fun submitRequestValidationErrorThrows() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(422).setBody("""{"message":"Validation failed","errors":{"title":["is required"]}}"""))
        val error = assertFailsWith<UprateError.ValidationError> { roadmap.submitRequest("") }
        assertEquals("Validation failed", error.message)
    }
}
