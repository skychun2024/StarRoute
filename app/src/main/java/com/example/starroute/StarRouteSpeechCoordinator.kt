
package com.example.starroute

import java.lang.ref.WeakReference
import java.util.WeakHashMap

// ========================================
// 음성 요청 결과
// ========================================

enum class StarRouteSpeechPlayStatus {
    REQUESTED,
    REPLACED,
    BLOCKED,
    DUPLICATE,
    NOT_READY,
    FAILED,
    INVALID
}

data class StarRouteSpeechPlayResult(
    val status: StarRouteSpeechPlayStatus,
    val message: String
)

// ========================================
// 음성 통합 관리자
// ========================================

class StarRouteSpeechCoordinator(
    private val isReady: () -> Boolean,
    private val speakAction: (String) -> Boolean,
    private val stopAction: () -> Unit
) {

    private val policy =
        StarRouteSpeechPolicy()

    @Synchronized
    fun currentRequest(): StarRouteSpeechRequest? {
        return policy.current()
    }

    // ====================================
    // 음성 요청
    // ====================================

    @Synchronized
    fun play(
        type: StarRouteSpeechType,
        text: String,
        requestKey: String
    ): StarRouteSpeechPlayResult {

        if (
            text.isBlank() ||
            requestKey.isBlank()
        ) {

            return StarRouteSpeechPlayResult(
                status = StarRouteSpeechPlayStatus.INVALID,
                message = "음성 요청 정보가 올바르지 않습니다."
            )
        }

        val ready = try {
            isReady()
        } catch (_: Exception) {
            false
        }

        if (!ready) {

            return StarRouteSpeechPlayResult(
                status = StarRouteSpeechPlayStatus.NOT_READY,
                message = "Pleos 음성 서비스가 준비되지 않았습니다."
            )
        }

        val request = StarRouteSpeechRequest(
            type = type,
            text = text,
            requestKey = requestKey
        )

        val decision =
            policy.evaluate(request)

        when (decision.decision) {

            StarRouteSpeechDecision.BLOCKED -> {

                return StarRouteSpeechPlayResult(
                    status = StarRouteSpeechPlayStatus.BLOCKED,
                    message =
                        "우선순위가 높은 음성 안내가 진행 중입니다."
                )
            }

            StarRouteSpeechDecision.DUPLICATE -> {

                return StarRouteSpeechPlayResult(
                    status = StarRouteSpeechPlayStatus.DUPLICATE,
                    message =
                        "같은 음성 요청이 이미 처리 중입니다."
                )
            }

            StarRouteSpeechDecision.INVALID -> {

                return StarRouteSpeechPlayResult(
                    status = StarRouteSpeechPlayStatus.INVALID,
                    message = "올바르지 않은 음성 요청입니다."
                )
            }

            else -> Unit
        }

        val replacing =
            decision.decision ==
                    StarRouteSpeechDecision.REPLACE

        val successful = try {

            if (replacing) {
                stopAction()
            }

            speakAction(text)

        } catch (_: Exception) {
            false
        }

        if (!successful) {

            policy.cancel(requestKey)

            return StarRouteSpeechPlayResult(
                status = StarRouteSpeechPlayStatus.FAILED,
                message =
                    "음성 재생 요청에 실패했습니다."
            )
        }

        return StarRouteSpeechPlayResult(
            status = if (replacing) {
                StarRouteSpeechPlayStatus.REPLACED
            } else {
                StarRouteSpeechPlayStatus.REQUESTED
            },

            message = if (replacing) {
                "기존 음성을 교체하고 새로운 안내를 요청했습니다."
            } else {
                "음성 재생을 요청했습니다."
            }
        )
    }

    // ====================================
    // 실제 TTS 완료 처리
    // ====================================

    /**
     * 음성 완료 이벤트 수신.
     *
     * Pleos EventListener.onDone() 또는
     * onError()를 통해 호출됩니다.
     */
    @Synchronized
    fun onPlaybackFinished() {

        val current = policy.current()
            ?: return

        policy.finish(
            current.requestKey
        )
    }

    /**
     * 기존 테스트 코드와의 호환성 유지.
     */
    @Synchronized
    fun onSpeechFinished(
        requestKey: String
    ): Boolean {

        return policy.finish(
            requestKey
        )
    }

    // ====================================
    // 음성 중지
    // ====================================

    @Synchronized
    fun stopAll() {

        try {
            stopAction()
        } catch (_: Exception) {
        }

        policy.reset()
    }
}

// ========================================
// 공유 음성 관리자 저장소
// ========================================

private val sharedCoordinatorLock = Any()

private val sharedCoordinators =
    WeakHashMap<
            StarRouteVoiceManager,
            WeakReference<StarRouteSpeechCoordinator>
            >()

// ========================================
// 동일한 TTS 관리자 공유
// ========================================

fun createStarRouteSpeechCoordinator(
    voiceManager: StarRouteVoiceManager
): StarRouteSpeechCoordinator {

    return synchronized(
        sharedCoordinatorLock
    ) {

        val existing =
            sharedCoordinators[voiceManager]?.get()

        if (existing != null) {

            existing

        } else {

            val created =
                StarRouteSpeechCoordinator(
                    isReady = {
                        voiceManager.isReady()
                    },

                    speakAction = { text ->
                        voiceManager.speak(text)
                    },

                    stopAction = {
                        voiceManager.stop()
                    }
                )

            // Pleos SDK 실제 음성 종료 이벤트 연결
            voiceManager.onPlaybackFinished = {

                // 완료 또는 오류 이벤트로
                // 현재 음성 요청 상태를 해제
                created.onPlaybackFinished()
            }

            sharedCoordinators[voiceManager] =
                WeakReference(created)

            created
        }
    }
}
