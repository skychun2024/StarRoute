
package com.example.starroute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StarRouteSpeechCoordinatorTest {

    @Test
    fun firstSpeechIsRequested() {

        var callCount = 0

        val coordinator = StarRouteSpeechCoordinator(
            isReady = { true },
            speakAction = {
                callCount++
                true
            },
            stopAction = {}
        )

        val result = coordinator.play(
            StarRouteSpeechType.STAR_STORY,
            "오리온자리 이야기",
            "story-1"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.REQUESTED,
            result.status
        )

        assertEquals(1, callCount)
    }

    @Test
    fun higherPriorityReplacesLowerPriority() {

        var stopCount = 0
        var speakCount = 0

        val coordinator = StarRouteSpeechCoordinator(
            isReady = { true },
            speakAction = {
                speakCount++
                true
            },
            stopAction = {
                stopCount++
            }
        )

        coordinator.play(
            StarRouteSpeechType.STAR_STORY,
            "별자리 신화",
            "story-1"
        )

        val result = coordinator.play(
            StarRouteSpeechType.DISTANCE_2KM,
            "2km 도착 안내",
            "arrival-2"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.REPLACED,
            result.status
        )

        assertEquals(1, stopCount)
        assertEquals(2, speakCount)
    }

    @Test
    fun lowerPriorityIsBlocked() {

        var speakCount = 0

        val coordinator = StarRouteSpeechCoordinator(
            isReady = { true },
            speakAction = {
                speakCount++
                true
            },
            stopAction = {}
        )

        coordinator.play(
            StarRouteSpeechType.DISTANCE_2KM,
            "2km 안내",
            "arrival-2"
        )

        val result = coordinator.play(
            StarRouteSpeechType.STAR_STORY,
            "별자리 이야기",
            "story-1"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.BLOCKED,
            result.status
        )

        assertEquals(1, speakCount)
    }

    @Test
    fun duplicateRequestIsNotPlayedAgain() {

        var speakCount = 0

        val coordinator = StarRouteSpeechCoordinator(
            isReady = { true },
            speakAction = {
                speakCount++
                true
            },
            stopAction = {}
        )

        coordinator.play(
            StarRouteSpeechType.DISTANCE_10KM,
            "10km 안내",
            "arrival-10"
        )

        val result = coordinator.play(
            StarRouteSpeechType.DISTANCE_10KM,
            "10km 안내",
            "arrival-10"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.DUPLICATE,
            result.status
        )

        assertEquals(1, speakCount)
    }

    @Test
    fun unavailableVoiceDoesNotClaimSuccess() {

        var called = false

        val coordinator = StarRouteSpeechCoordinator(
            isReady = { false },
            speakAction = {
                called = true
                true
            },
            stopAction = {}
        )

        val result = coordinator.play(
            StarRouteSpeechType.STAR_STORY,
            "별자리 이야기",
            "story-1"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.NOT_READY,
            result.status
        )

        assertFalse(called)
        assertNull(coordinator.currentRequest())
    }

    @Test
    fun failedSpeechCanBeRetried() {

        var shouldSucceed = false

        val coordinator = StarRouteSpeechCoordinator(
            isReady = { true },
            speakAction = {
                shouldSucceed
            },
            stopAction = {}
        )

        val first = coordinator.play(
            StarRouteSpeechType.STAR_STORY,
            "별자리 이야기",
            "story-1"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.FAILED,
            first.status
        )

        shouldSucceed = true

        val second = coordinator.play(
            StarRouteSpeechType.STAR_STORY,
            "별자리 이야기",
            "story-1"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.REQUESTED,
            second.status
        )
    }

    @Test
    fun stopAllClearsState() {

        var stopped = false

        val coordinator = StarRouteSpeechCoordinator(
            isReady = { true },
            speakAction = { true },
            stopAction = {
                stopped = true
            }
        )

        coordinator.play(
            StarRouteSpeechType.STAR_STORY,
            "별자리 이야기",
            "story-1"
        )

        coordinator.stopAll()

        assertTrue(stopped)
        assertNull(coordinator.currentRequest())
    }

    @Test
    fun finishedSpeechReleasesPriority() {

        val coordinator = StarRouteSpeechCoordinator(
            isReady = { true },
            speakAction = { true },
            stopAction = {}
        )

        coordinator.play(
            StarRouteSpeechType.DISTANCE_2KM,
            "도착 안내",
            "arrival-2"
        )

        assertTrue(
            coordinator.onSpeechFinished("arrival-2")
        )

        val result = coordinator.play(
            StarRouteSpeechType.STAR_STORY,
            "별자리 신화",
            "story-1"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.REQUESTED,
            result.status
        )
    }

    @Test
    fun oldSpeechFinishDoesNotClearNewSpeech() {

        val coordinator = StarRouteSpeechCoordinator(
            isReady = { true },
            speakAction = { true },
            stopAction = {}
        )

        coordinator.play(
            StarRouteSpeechType.STAR_STORY,
            "별자리 신화",
            "story-1"
        )

        coordinator.play(
            StarRouteSpeechType.DISTANCE_2KM,
            "2km 안내",
            "arrival-2"
        )

        assertFalse(
            coordinator.onSpeechFinished("story-1")
        )

        assertEquals(
            "arrival-2",
            coordinator.currentRequest()?.requestKey
        )
    }

    @Test
    fun invalidRequestDoesNotPlay() {

        val coordinator = StarRouteSpeechCoordinator(
            isReady = { true },
            speakAction = { true },
            stopAction = {}
        )

        val result = coordinator.play(
            StarRouteSpeechType.STAR_STORY,
            "",
            "story-1"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.INVALID,
            result.status
        )
    }
}
