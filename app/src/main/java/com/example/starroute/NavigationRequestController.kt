
package com.example.starroute

/**
 * 별길 내비게이션 경로 요청 관리
 *
 * Pleos SDK 자체를 변경하지 않고
 * 경로 요청의 중복과 실패를 관리합니다.
 */

// 현재 요청 상태
enum class NavigationRequestState {
    IDLE,
    REQUESTING,
    REQUEST_SENT
}

// 경로 요청 결과
enum class NavigationRequestResult {
    REQUEST_SENT,
    ALREADY_REQUESTED,
    REQUEST_IN_PROGRESS,
    INVALID_DESTINATION,
    SDK_ERROR
}

// 화면에 표시할 상태 정보
data class NavigationRequestInfo(
    val state: NavigationRequestState,
    val destinationKey: String?,
    val message: String
)

class NavigationRequestController {

    private val lock = Any()

    private var currentState =
        NavigationRequestState.IDLE

    private var currentDestination: String? = null

    private var statusMessage =
        "경로 요청 준비 완료"

    /**
     * 현재 경로 요청 상태 확인
     */
    fun getInfo(): NavigationRequestInfo {
        return synchronized(lock) {
            NavigationRequestInfo(
                state = currentState,
                destinationKey = currentDestination,
                message = statusMessage
            )
        }
    }

    /**
     * Pleos SDK 경로 요청 실행
     *
     * destinationKey:
     * 목적지별 고유 문자열입니다.
     * 예: 위도와 경도를 조합한 값
     *
     * requestAction:
     * 실제 Pleos NaviHelper.requestRoute()
     * 호출을 전달합니다.
     *
     * 주의:
     * SDK 호출에서 예외가 발생하지 않았다는
     * 사실과 실제 경로 안내가 시작되었다는
     * 사실은 서로 다릅니다.
     */
    fun requestRoute(
        destinationKey: String,
        requestAction: () -> Unit
    ): NavigationRequestResult {

        val normalizedKey = destinationKey.trim()

        if (normalizedKey.isEmpty()) {
            return NavigationRequestResult.INVALID_DESTINATION
        }

        // 요청 시작 전에 상태 검사
        synchronized(lock) {

            if (
                currentState ==
                NavigationRequestState.REQUESTING
            ) {
                return NavigationRequestResult.REQUEST_IN_PROGRESS
            }

            if (
                currentState ==
                NavigationRequestState.REQUEST_SENT &&
                currentDestination == normalizedKey
            ) {
                return NavigationRequestResult.ALREADY_REQUESTED
            }

            currentState =
                NavigationRequestState.REQUESTING

            currentDestination = normalizedKey

            statusMessage =
                "Pleos 경로 요청 함수를 호출하고 있습니다."
        }

        // SDK 호출은 잠금 밖에서 수행
        return try {

            requestAction()

            synchronized(lock) {

                currentState =
                    NavigationRequestState.REQUEST_SENT

                statusMessage =
                    "경로 요청 함수를 호출했습니다. " +
                            "실제 내비게이션 시작 여부는 " +
                            "별도 확인이 필요합니다."
            }

            NavigationRequestResult.REQUEST_SENT

        } catch (_: Exception) {

            synchronized(lock) {

                currentState =
                    NavigationRequestState.IDLE

                currentDestination = null

                statusMessage =
                    "경로 요청 중 오류가 발생했습니다. " +
                            "다시 요청할 수 있습니다."
            }

            NavigationRequestResult.SDK_ERROR
        }
    }

    /**
     * 사용자가 수동으로 재시도를 선택할 때 사용
     *
     * 요청 함수가 현재 실행 중이면
     * 초기화하지 않습니다.
     */
    fun reset(): Boolean {

        return synchronized(lock) {

            if (
                currentState ==
                NavigationRequestState.REQUESTING
            ) {
                false

            } else {

                currentState =
                    NavigationRequestState.IDLE

                currentDestination = null

                statusMessage =
                    "경로 요청을 다시 시도할 수 있습니다."

                true
            }
        }
    }
}

/**
 * 앱에 표시할 결과 메시지
 */
fun navigationResultMessage(
    result: NavigationRequestResult
): String {

    return when (result) {

        NavigationRequestResult.REQUEST_SENT ->
            "Pleos 경로 요청 함수를 호출했습니다. " +
                    "실제 경로 안내 시작 여부는 확인이 필요합니다."

        NavigationRequestResult.ALREADY_REQUESTED ->
            "이 목적지로 이미 경로 요청을 보냈습니다. " +
                    "다시 요청하려면 재시도 버튼을 눌러주세요."

        NavigationRequestResult.REQUEST_IN_PROGRESS ->
            "이전 경로 요청이 처리 중입니다."

        NavigationRequestResult.INVALID_DESTINATION ->
            "올바른 목적지 정보가 필요합니다."

        NavigationRequestResult.SDK_ERROR ->
            "경로 요청 중 오류가 발생했습니다. " +
                    "연결 상태를 확인한 뒤 다시 시도해 주세요."
    }
}
