package com.upratehq.sdk

import com.upratehq.sdk.models.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ModelDecodingTest {

    @Test
    fun decodesRoadmapItemFull() {
        val json = """
        {
            "uuid": "abc-123",
            "title": "Dark mode",
            "description": "Add dark mode support",
            "status": "planned",
            "status_label": "Planned",
            "votes_count": 42,
            "has_voted": true,
            "voting_disabled": false
        }
        """
        val item = uprateJson.decodeFromString<RoadmapItem>(json)
        assertEquals("abc-123", item.id)
        assertEquals("Dark mode", item.title)
        assertEquals("Add dark mode support", item.description)
        assertEquals("planned", item.status)
        assertEquals("Planned", item.statusLabel)
        assertEquals(42, item.votesCount)
        assertEquals(true, item.hasVoted)
        assertEquals(false, item.votingDisabled)
    }

    @Test
    fun decodesRoadmapItemMinimal() {
        val json = """
        {
            "uuid": "abc-123",
            "title": "Feature",
            "status": "planned",
            "status_label": "Planned",
            "has_voted": false,
            "voting_disabled": false
        }
        """
        val item = uprateJson.decodeFromString<RoadmapItem>(json)
        assertNull(item.description)
        assertNull(item.votesCount)
    }

    @Test
    fun decodesRoadmapItemExtraFields() {
        val json = """
        {
            "uuid": "abc-123",
            "title": "Feature",
            "status": "planned",
            "status_label": "Planned",
            "has_voted": false,
            "voting_disabled": false,
            "unknown_field": "should be ignored"
        }
        """
        val item = uprateJson.decodeFromString<RoadmapItem>(json)
        assertEquals("abc-123", item.id)
    }

    @Test
    fun decodesRoadmapSettings() {
        val json = """
        {
            "voting_enabled": true,
            "show_vote_count": false,
            "voting_excluded_statuses": ["completed", "rejected"]
        }
        """
        val settings = uprateJson.decodeFromString<RoadmapSettings>(json)
        assertEquals(true, settings.votingEnabled)
        assertEquals(false, settings.showVoteCount)
        assertEquals(listOf("completed", "rejected"), settings.votingExcludedStatuses)
    }

    @Test
    fun decodesRoadmapResponse() {
        val json = """
        {
            "settings": {
                "voting_enabled": true,
                "show_vote_count": true,
                "voting_excluded_statuses": []
            },
            "items": [{
                "uuid": "item-1",
                "title": "Feature A",
                "status": "planned",
                "status_label": "Planned",
                "has_voted": false,
                "voting_disabled": false
            }]
        }
        """
        val response = uprateJson.decodeFromString<UprateRoadmapResponse>(json)
        assertEquals(true, response.settings.votingEnabled)
        assertEquals(1, response.items.size)
        assertEquals("Feature A", response.items[0].title)
    }

    @Test
    fun decodesRoadmapResponseEmpty() {
        val json = """
        {
            "settings": {
                "voting_enabled": false,
                "show_vote_count": false,
                "voting_excluded_statuses": []
            },
            "items": []
        }
        """
        val response = uprateJson.decodeFromString<UprateRoadmapResponse>(json)
        assertEquals(0, response.items.size)
    }

    @Test
    fun decodesVoteResult() {
        val json = """{"voted": true, "votes_count": 10}"""
        val result = uprateJson.decodeFromString<VoteResult>(json)
        assertEquals(true, result.voted)
        assertEquals(10, result.votesCount)
    }

    @Test
    fun decodesFeatureRequestFull() {
        val json = """
        {
            "uuid": "req-1",
            "title": "My request",
            "description": "Details here",
            "status": "pending",
            "created_at": "2025-01-15T10:30:00Z"
        }
        """
        val request = uprateJson.decodeFromString<FeatureRequest>(json)
        assertEquals("req-1", request.id)
        assertEquals("My request", request.title)
        assertEquals("Details here", request.description)
        assertEquals("pending", request.status)
        assertEquals("2025-01-15T10:30:00Z", request.createdAt)
    }

    @Test
    fun decodesFeatureRequestNullDescription() {
        val json = """
        {
            "uuid": "req-1",
            "title": "My request",
            "status": "pending",
            "created_at": "2025-01-15T10:30:00Z"
        }
        """
        val request = uprateJson.decodeFromString<FeatureRequest>(json)
        assertNull(request.description)
    }

    @Test
    fun decodesFeedbackResultFull() {
        val json = """
        {
            "uuid": "fb-1",
            "rating": 4,
            "message": "Great app!",
            "status": "new",
            "created_at": "2025-01-15T10:30:00Z"
        }
        """
        val result = uprateJson.decodeFromString<FeedbackResult>(json)
        assertEquals("fb-1", result.id)
        assertEquals(4, result.rating)
        assertEquals("Great app!", result.message)
        assertEquals("new", result.status)
    }

    @Test
    fun decodesFeedbackResultNoRating() {
        val json = """
        {
            "uuid": "fb-1",
            "message": "Feedback",
            "status": "new",
            "created_at": "2025-01-15T10:30:00Z"
        }
        """
        val result = uprateJson.decodeFromString<FeedbackResult>(json)
        assertNull(result.rating)
    }

    @Test
    fun decodesFeedbackSubmissionFull() {
        val json = """
        {
            "uuid": "sub-1",
            "rating": 5,
            "message": "Love it",
            "sentiment": "positive",
            "created_at": "2025-01-15T10:30:00Z"
        }
        """
        val submission = uprateJson.decodeFromString<FeedbackSubmission>(json)
        assertEquals("sub-1", submission.id)
        assertEquals(5, submission.rating)
        assertEquals("Love it", submission.message)
        assertEquals("positive", submission.sentiment)
    }

    @Test
    fun decodesFeedbackSubmissionMinimal() {
        val json = """
        {
            "uuid": "sub-1",
            "message": "OK",
            "created_at": "2025-01-15T10:30:00Z"
        }
        """
        val submission = uprateJson.decodeFromString<FeedbackSubmission>(json)
        assertNull(submission.rating)
        assertNull(submission.sentiment)
    }

    @Test
    fun decodesReviewSignalResult() {
        val json = """
        {
            "uuid": "sig-1",
            "status": "recorded",
            "expires_at": "2025-02-15T10:30:00Z"
        }
        """
        val result = uprateJson.decodeFromString<ReviewSignalResult>(json)
        assertEquals("sig-1", result.id)
        assertEquals("recorded", result.status)
        assertEquals("2025-02-15T10:30:00Z", result.expiresAt)
    }

    @Test
    fun decodesReviewSignalResultExtraFields() {
        val json = """
        {
            "uuid": "sig-1",
            "status": "recorded",
            "expires_at": "2025-02-15T10:30:00Z",
            "extra": true
        }
        """
        val result = uprateJson.decodeFromString<ReviewSignalResult>(json)
        assertEquals("sig-1", result.id)
    }
}
