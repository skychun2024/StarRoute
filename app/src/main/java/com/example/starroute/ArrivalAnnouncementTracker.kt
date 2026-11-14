
package com.example.starroute

/**
 * 목적지 접근 음성이 중복 요청되지 않도록
 * 안내 기록을 관리하는 클래스입니다.
 *
 * 실제 음성을 재생하지는 않습니다.
 */
class ArrivalAnnouncementTracker {

    private var announced10Km = false
    private var announced2Km = false

    /**
     * 현재 거리에서 새로 안내해야 할 단계를 반환합니다.
     * 이미 요청한 안내는 다시 반환하지 않습니다.
     */
    fun nextAnnouncement(
        distanceKm: Double
    ): ArrivalStage? {

        return when (getArrivalStage(distanceKm)) {

            ArrivalStage.WITHIN_2_KM -> {
                if (!announced2Km) {
                    ArrivalStage.WITHIN_2_KM
                } else {
                    null
                }
            }

            ArrivalStage.WITHIN_10_KM -> {
                if (!announced10Km) {
                    ArrivalStage.WITHIN_10_KM
                } else {
                    null
                }
            }

            ArrivalStage.FAR,
            ArrivalStage.INVALID -> null
        }
    }

    /**
     * 음성 요청을 성공적으로 전달한 경우에만
     * 해당 단계의 안내 기록을 저장합니다.
     */
    fun markRequested(stage: ArrivalStage) {

        when (stage) {

            ArrivalStage.WITHIN_10_KM -> {
                announced10Km = true
            }

            ArrivalStage.WITHIN_2_KM -> {
                announced2Km = true

                // 처음부터 2km 이내였다면
                // 10km 안내는 생략합니다.
                announced10Km = true
            }

            ArrivalStage.FAR,
            ArrivalStage.INVALID -> Unit
        }
    }

    // 새로운 목적지나 테스트 시작 시 초기화
    fun reset() {
        announced10Km = false
        announced2Km = false
    }

    fun hasAnnounced10Km(): Boolean {
        return announced10Km
    }

    fun hasAnnounced2Km(): Boolean {
        return announced2Km
    }
}
