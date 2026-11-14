
package com.example.starroute

/**
 * 별길 앱 음성 우선순위 관리
 *
 * 실제 Pleos TTS를 직접 호출하지 않습니다.
 * 음성 요청을 허용할지 결정하는 규칙입니다.
 */

enum class StarRouteSpeechType(
    val priority: Int
) {
    STAR_STORY(10),
    ARRIVAL_GREETING(20),
    DISTANCE_10KM(30),
    DISTANCE_2KM(40)
}

enum class StarRouteSpeechDecision {
    START,
    REPLACE,
    BLOCKED,
    DUPLICATE,
    INVALID
}

data class StarRouteSpeechRequest(
    val type: StarRouteSpeechType,
    val text: String,
    val requestKey: String
)

data class StarRouteSpeechResult(
    val decision: StarRouteSpeechDecision,
    val message: String
)

class StarRouteSpeechPolicy {

    private var currentRequest:
            StarRouteSpeechRequest? = null

    /**
     * 지금 재생을 허용한 음성 정보를 반환합니다.
     */
    @Synchronized
    fun current(): StarRouteSpeechRequest? {
        return currentRequest
    }

    /**
     * 새 음성 요청 판단
     *
     * 같은 우선순위에서는 기존 요청을 유지합니다.
     * 높은 우선순위는 기존 음성을 대체할 수 있습니다.
     */
    @Synchronized
    fun evaluate(
        request: StarRouteSpeechRequest
    ): StarRouteSpeechResult {

        if (
            request.text.isBlank() ||
            request.requestKey.isBlank()
        ) {
            return StarRouteSpeechResult(
                decision = StarRouteSpeechDecision.INVALID,
                message = "음성 요청 정보가 올바르지 않습니다."
            )
        }

        val active = currentRequest

        if (active == null) {
            currentRequest = request

            return StarRouteSpeechResult(
                decision = StarRouteSpeechDecision.START,
                message = "새로운 음성 안내를 허용했습니다."
            )
        }

        if (active.requestKey == request.requestKey) {
            return StarRouteSpeechResult(
                decision = StarRouteSpeechDecision.DUPLICATE,
                message = "같은 음성 요청이 이미 처리 중입니다."
            )
        }

        if (request.type.priority > active.type.priority) {
            currentRequest = request

            return StarRouteSpeechResult(
                decision = StarRouteSpeechDecision.REPLACE,
                message = "높은 우선순위의 음성 안내로 전환합니다."
            )
        }

        return StarRouteSpeechResult(
            decision = StarRouteSpeechDecision.BLOCKED,
            message = "현재 음성 안내가 우선입니다."
        )
    }

    /**
     * 음성 종료
     *
     * requestKey를 비교하여 이전 음성의
     * 종료 이벤트가 새로운 음성을
     * 잘못 종료 처리하지 않도록 합니다.
     */
    @Synchronized
    fun finish(
        requestKey: String
    ): Boolean {

        if (currentRequest?.requestKey != requestKey) {
            return false
        }

        currentRequest = null
        return true
    }

    /**
     * TTS 호출 자체가 실패한 경우
     * 해당 요청의 점유 상태를 해제합니다.
     */
    @Synchronized
    fun cancel(
        requestKey: String
    ): Boolean {
        return finish(requestKey)
    }

    /**
     * 화면 이탈 또는 전체 음성 중지
     */
    @Synchronized
    fun reset() {
        currentRequest = null
    }
}
