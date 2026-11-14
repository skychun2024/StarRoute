
package com.example.starroute

/**
 * 별길 앱의 목적지 접근 안내 규칙
 *
 * 실제 GPS 위치와 테스트 GPS 위치 모두
 * 동일한 거리 기준을 적용할 수 있도록
 * 규칙을 별도 파일로 분리합니다.
 */

// 목적지 접근 단계
enum class ArrivalStage {
    FAR,
    WITHIN_10_KM,
    WITHIN_2_KM,
    INVALID
}

// 거리별 접근 단계 판단
fun getArrivalStage(distanceKm: Double): ArrivalStage {

    if (!distanceKm.isFinite() || distanceKm < 0.0) {
        return ArrivalStage.INVALID
    }

    return when {
        distanceKm <= 2.0 ->
            ArrivalStage.WITHIN_2_KM

        distanceKm <= 10.0 ->
            ArrivalStage.WITHIN_10_KM

        else ->
            ArrivalStage.FAR
    }
}

// 거리별 화면 표시 메시지
fun getArrivalStageMessage(
    destinationName: String,
    distanceKm: Double
): String {

    return when (getArrivalStage(distanceKm)) {

        ArrivalStage.WITHIN_2_KM ->
            "${destinationName} 근처에 도착했어요. " +
                    "안전한 장소에 정차한 후 " +
                    "별자리를 감상해 보세요."

        ArrivalStage.WITHIN_10_KM ->
            "${destinationName}가 가까워지고 있어요. " +
                    "곧 별빛 가득한 여행이 시작됩니다."

        ArrivalStage.FAR ->
            "목적지로 이동 중입니다. " +
                    "10km 이내에 접근하면 안내를 시작합니다."

        ArrivalStage.INVALID ->
            "GPS 거리 정보를 확인할 수 없습니다."
    }
}

// 개발용 거리 규칙 점검
// 실제 음성을 재생하거나 GPS를 변경하지 않습니다.
fun checkArrivalDistanceRules(): Boolean {

    val cases = listOf(
        16.6 to ArrivalStage.FAR,
        10.0 to ArrivalStage.WITHIN_10_KM,
        5.5 to ArrivalStage.WITHIN_10_KM,
        2.0 to ArrivalStage.WITHIN_2_KM,
        0.5 to ArrivalStage.WITHIN_2_KM,
        0.0 to ArrivalStage.WITHIN_2_KM,
        -1.0 to ArrivalStage.INVALID,
        Double.NaN to ArrivalStage.INVALID
    )

    return cases.all { (distance, expected) ->
        getArrivalStage(distance) == expected
    }
}
