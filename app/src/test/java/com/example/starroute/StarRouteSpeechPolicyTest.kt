
package com.example.starroute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarRouteSpeechPolicyTest {

    private fun request(
        type: StarRouteSpeechType,
        key: String
    ): StarRouteSpeechRequest {

        return StarRouteSpeechRequest(
            type = type,
            text = "별길 음성 테스트",
            requestKey = key
        )
    }

    @Test
    fun firstSpeechStarts() {

        val policy = StarRouteSpeechPolicy()

        val result = policy.evaluate(
            request(
                StarRouteSpeechType.STAR_STORY,
                "story-1"
            )
        )

        assertEquals(
            StarRouteSpeechDecision.START,
            result.decision
        )
    }

    @Test
    fun duplicateSpeechIsBlocked() {

        val policy = StarRouteSpeechPolicy()

        val speech = request(
            StarRouteSpeechType.DISTANCE_10KM,
            "arrival-10"
        )

        policy.evaluate(speech)

        assertEquals(
            StarRouteSpeechDecision.DUPLICATE,
            policy.evaluate(speech).decision
        )
    }

    @Test
    fun distanceAnnouncementOverridesStory() {

        val policy = StarRouteSpeechPolicy()

        policy.evaluate(
            request(
                StarRouteSpeechType.STAR_STORY,
                "story-1"
            )
        )

        val result = policy.evaluate(
            request(
                StarRouteSpeechType.DISTANCE_10KM,
                "arrival-10"
            )
        )

        assertEquals(
            StarRouteSpeechDecision.REPLACE,
            result.decision
        )

        assertEquals(
            "arrival-10",
            policy.current()?.requestKey
        )
    }

    @Test
    fun storyCannotOverrideDistanceAnnouncement() {

        val policy = StarRouteSpeechPolicy()

        policy.evaluate(
            request(
                StarRouteSpeechType.DISTANCE_2KM,
                "arrival-2"
            )
        )

        val result = policy.evaluate(
            request(
                StarRouteSpeechType.STAR_STORY,
                "story-1"
            )
        )

        assertEquals(
            StarRouteSpeechDecision.BLOCKED,
            result.decision
        )
    }

    @Test
    fun twoKmAnnouncementOverridesTenKm() {

        val policy = StarRouteSpeechPolicy()

        policy.evaluate(
            request(
                StarRouteSpeechType.DISTANCE_10KM,
                "arrival-10"
            )
        )

        val result = policy.evaluate(
            request(
                StarRouteSpeechType.DISTANCE_2KM,
                "arrival-2"
            )
        )

        assertEquals(
            StarRouteSpeechDecision.REPLACE,
            result.decision
        )
    }

    @Test
    fun greetingCannotOverrideTwoKmAnnouncement() {

        val policy = StarRouteSpeechPolicy()

        policy.evaluate(
            request(
                StarRouteSpeechType.DISTANCE_2KM,
                "arrival-2"
            )
        )

        val result = policy.evaluate(
            request(
                StarRouteSpeechType.ARRIVAL_GREETING,
                "greeting-1"
            )
        )

        assertEquals(
            StarRouteSpeechDecision.BLOCKED,
            result.decision
        )
    }

    @Test
    fun finishingOldRequestDoesNotClearNewOne() {

        val policy = StarRouteSpeechPolicy()

        policy.evaluate(
            request(
                StarRouteSpeechType.STAR_STORY,
                "story-1"
            )
        )

        policy.evaluate(
            request(
                StarRouteSpeechType.DISTANCE_2KM,
                "arrival-2"
            )
        )

        assertFalse(policy.finish("story-1"))

        assertEquals(
            "arrival-2",
            policy.current()?.requestKey
        )
    }

    @Test
    fun finishingActiveRequestClearsState() {

        val policy = StarRouteSpeechPolicy()

        policy.evaluate(
            request(
                StarRouteSpeechType.STAR_STORY,
                "story-1"
            )
        )

        assertTrue(policy.finish("story-1"))
        assertNull(policy.current())
    }

    @Test
    fun failedRequestCanBeRetried() {

        val policy = StarRouteSpeechPolicy()

        val speech = request(
            StarRouteSpeechType.STAR_STORY,
            "story-1"
        )

        policy.evaluate(speech)

        assertTrue(policy.cancel("story-1"))

        assertEquals(
            StarRouteSpeechDecision.START,
            policy.evaluate(speech).decision
        )
    }

    @Test
    fun invalidTextIsRejected() {

        val policy = StarRouteSpeechPolicy()

        val result = policy.evaluate(
            StarRouteSpeechRequest(
                type = StarRouteSpeechType.STAR_STORY,
                text = "",
                requestKey = "story-1"
            )
        )

        assertEquals(
            StarRouteSpeechDecision.INVALID,
            result.decision
        )

        assertNull(policy.current())
    }

    @Test
    fun resetClearsCurrentSpeech() {

        val policy = StarRouteSpeechPolicy()

        policy.evaluate(
            request(
                StarRouteSpeechType.DISTANCE_10KM,
                "arrival-10"
            )
        )

        policy.reset()

        assertNull(policy.current())
    }
}
