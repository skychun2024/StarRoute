
package com.example.starroute

import org.junit.Assert.*
import org.junit.Test

/**
 * 별길 음성 통합 자동 테스트
 *
 * 실제 Pleos SDK를 실행하지 않고
 * 음성 요청 우선순위와 상태 변경을 테스트합니다.
 */
class StarRouteSpeechIntegrationTest {

    // ========================================
    // 테스트용 가짜 음성 서비스
    // ========================================

    private class FakeVoiceService {

        var ready = true

        var speakResult = true

        var stopCount = 0

        val spokenTexts = mutableListOf<String>()

        fun createCoordinator():
                StarRouteSpeechCoordinator {

            return StarRouteSpeechCoordinator(
                isReady = {
                    ready
                },

                speakAction = { text ->

                    if (speakResult) {
                        spokenTexts.add(text)
                    }

                    speakResult
                },

                stopAction = {
                    stopCount++
                }
            )
        }
    }

    // ========================================
    // 1. 첫 음성 요청 성공
    // ========================================

    @Test
    fun firstSpeechRequestSucceeds() {

        val fake = FakeVoiceService()

        val coordinator = fake.createCoordinator()

        val result = coordinator.play(
            type = StarRouteSpeechType.STAR_STORY,
            text = "오리온자리 이야기입니다.",
            requestKey = "story-1"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.REQUESTED,
            result.status
        )

        assertEquals(
            listOf("오리온자리 이야기입니다."),
            fake.spokenTexts
        )
    }

    // ========================================
    // 2. 높은 우선순위 음성이
    //    별자리 이야기를 교체
    // ========================================

    @Test
    fun distance2KmReplacesStarStory() {

        val fake = FakeVoiceService()

        val coordinator = fake.createCoordinator()

        coordinator.play(
            type = StarRouteSpeechType.STAR_STORY,
            text = "별자리 이야기",
            requestKey = "story-1"
        )

        val result = coordinator.play(
            type = StarRouteSpeechType.DISTANCE_2KM,
            text = "목적지까지 2킬로미터 남았습니다.",
            requestKey = "distance-2km"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.REPLACED,
            result.status
        )

        assertEquals(
            1,
            fake.stopCount
        )

        assertEquals(
            2,
            fake.spokenTexts.size
        )
    }

    // ========================================
    // 3. 높은 우선순위 안내 중
    //    낮은 우선순위 요청 차단
    // ========================================

    @Test
    fun storyCannotInterruptDistance2Km() {

        val fake = FakeVoiceService()

        val coordinator = fake.createCoordinator()

        coordinator.play(
            type = StarRouteSpeechType.DISTANCE_2KM,
            text = "2킬로미터 안내",
            requestKey = "distance-2km"
        )

        val result = coordinator.play(
            type = StarRouteSpeechType.STAR_STORY,
            text = "오리온자리 이야기",
            requestKey = "story-1"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.BLOCKED,
            result.status
        )

        assertEquals(
            1,
            fake.spokenTexts.size
        )
    }

    // ========================================
    // 4. 같은 요청 중복 차단
    // ========================================

    @Test
    fun duplicateSpeechRequestIsBlocked() {

        val fake = FakeVoiceService()

        val coordinator = fake.createCoordinator()

        coordinator.play(
            type = StarRouteSpeechType.DISTANCE_10KM,
            text = "10킬로미터 접근 안내",
            requestKey = "distance-10km"
        )

        val result = coordinator.play(
            type = StarRouteSpeechType.DISTANCE_10KM,
            text = "10킬로미터 접근 안내",
            requestKey = "distance-10km"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.DUPLICATE,
            result.status
        )

        assertEquals(
            1,
            fake.spokenTexts.size
        )
    }

    // ========================================
    // 5. 음성 서비스 준비 전
    //    요청하지 않음
    // ========================================

    @Test
    fun speechWaitsWhenServiceNotReady() {

        val fake = FakeVoiceService()

        fake.ready = false

        val coordinator = fake.createCoordinator()

        val result = coordinator.play(
            type = StarRouteSpeechType.STAR_STORY,
            text = "카시오페이아자리",
            requestKey = "story-2"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.NOT_READY,
            result.status
        )

        assertTrue(
            fake.spokenTexts.isEmpty()
        )
    }

    // ========================================
    // 6. 실패한 요청 재시도
    // ========================================

    @Test
    fun failedSpeechCanBeRetried() {

        val fake = FakeVoiceService()

        fake.speakResult = false

        val coordinator = fake.createCoordinator()

        val first = coordinator.play(
            type = StarRouteSpeechType.STAR_STORY,
            text = "큰곰자리 이야기",
            requestKey = "story-failed"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.FAILED,
            first.status
        )

        fake.speakResult = true

        val retry = coordinator.play(
            type = StarRouteSpeechType.STAR_STORY,
            text = "큰곰자리 이야기",
            requestKey = "story-failed"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.REQUESTED,
            retry.status
        )

        assertEquals(
            1,
            fake.spokenTexts.size
        )
    }

    // ========================================
    // 7. 음성 정지 후
    //    새로운 안내 가능
    // ========================================

    @Test
    fun stopAllAllowsNewSpeech() {

        val fake = FakeVoiceService()

        val coordinator = fake.createCoordinator()

        coordinator.play(
            type = StarRouteSpeechType.DISTANCE_2KM,
            text = "도착 안내",
            requestKey = "distance"
        )

        coordinator.stopAll()

        val result = coordinator.play(
            type = StarRouteSpeechType.STAR_STORY,
            text = "별자리 이야기",
            requestKey = "new-story"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.REQUESTED,
            result.status
        )

        assertEquals(
            2,
            fake.spokenTexts.size
        )
    }

    // ========================================
    // 8. 종료 콜백 이후
    //    낮은 우선순위 음성 허용
    // ========================================

    @Test
    fun speechCompletionAllowsNextRequest() {

        val fake = FakeVoiceService()

        val coordinator = fake.createCoordinator()

        coordinator.play(
            type = StarRouteSpeechType.DISTANCE_2KM,
            text = "2킬로미터 안내",
            requestKey = "distance-2"
        )

        val finished = coordinator.onSpeechFinished(
            "distance-2"
        )

        assertTrue(finished)

        val result = coordinator.play(
            type = StarRouteSpeechType.STAR_STORY,
            text = "오리온자리 이야기",
            requestKey = "story-after-distance"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.REQUESTED,
            result.status
        )
    }

    // ========================================
    // 9. 잘못된 종료 콜백은
    //    현재 음성을 종료하지 않음
    // ========================================

    @Test
    fun wrongCompletionKeyDoesNotClearSpeech() {

        val fake = FakeVoiceService()

        val coordinator = fake.createCoordinator()

        coordinator.play(
            type = StarRouteSpeechType.DISTANCE_2KM,
            text = "도착 안내",
            requestKey = "correct-key"
        )

        val finished = coordinator.onSpeechFinished(
            "wrong-key"
        )

        assertFalse(finished)

        val result = coordinator.play(
            type = StarRouteSpeechType.STAR_STORY,
            text = "별자리 이야기",
            requestKey = "story"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.BLOCKED,
            result.status
        )
    }

    // ========================================
    // 10. 빈 문자열 안내 차단
    // ========================================

    @Test
    fun blankSpeechIsInvalid() {

        val fake = FakeVoiceService()

        val coordinator = fake.createCoordinator()

        val result = coordinator.play(
            type = StarRouteSpeechType.STAR_STORY,
            text = "   ",
            requestKey = "empty-story"
        )

        assertEquals(
            StarRouteSpeechPlayStatus.INVALID,
            result.status
        )

        assertTrue(fake.spokenTexts.isEmpty())
    }
}
