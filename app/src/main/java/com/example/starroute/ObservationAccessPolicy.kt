
package com.example.starroute

/**
 * 별자리 관측실 진입 조건을 판단하는 코드입니다.
 *
 * DEMO:
 * 개발자가 직접 시연용 주차 상태를 선택합니다.
 *
 * VEHICLE:
 * 실제 차량에서 확인된 상태만 사용합니다.
 * 현재는 차량 데이터 연결 전이므로 UNKNOWN으로 처리합니다.
 */

enum class ObservationMode {
    DEMO,
    VEHICLE
}

enum class StarRouteGear {
    PARK,
    DRIVE,
    REVERSE,
    NEUTRAL,
    UNKNOWN
}

data class ObservationVehicleState(
    val gear: StarRouteGear = StarRouteGear.UNKNOWN,
    val isMoving: Boolean? = null
)

data class ObservationAccessResult(
    val allowed: Boolean,
    val message: String,
    val isDemo: Boolean
)

/**
 * 별자리 신화 화면 진입 가능 여부
 *
 * 실제 차량 모드에서는 다음 조건이
 * 모두 확인되어야 합니다.
 *
 * 1. 기어 상태가 PARK
 * 2. 차량이 움직이지 않음
 *
 * 상태를 확인할 수 없으면 진입을 막습니다.
 */
fun checkObservationAccess(
    mode: ObservationMode,
    demoParked: Boolean = false,
    vehicleState: ObservationVehicleState =
        ObservationVehicleState()
): ObservationAccessResult {

    if (mode == ObservationMode.DEMO) {

        return if (demoParked) {

            ObservationAccessResult(
                allowed = true,
                message =
                    "시연용 주차 상태입니다. " +
                            "별자리 관측실을 체험할 수 있습니다.",
                isDemo = true
            )

        } else {

            ObservationAccessResult(
                allowed = false,
                message =
                    "시연용 P단으로 전환한 뒤 " +
                            "별자리 관측실에 입장해 주세요.",
                isDemo = true
            )
        }
    }

    // 실제 차량 모드:
    // 주차 상태와 정지 여부가 모두 확인되어야 합니다.

    if (vehicleState.gear == StarRouteGear.UNKNOWN) {

        return ObservationAccessResult(
            allowed = false,
            message =
                "차량 기어 상태를 확인할 수 없습니다. " +
                        "관측실 진입을 제한합니다.",
            isDemo = false
        )
    }

    if (vehicleState.gear != StarRouteGear.PARK) {

        return ObservationAccessResult(
            allowed = false,
            message =
                "차량이 주차 상태가 아닙니다. " +
                        "안전하게 주차한 뒤 이용해 주세요.",
            isDemo = false
        )
    }

    if (vehicleState.isMoving != false) {

        return ObservationAccessResult(
            allowed = false,
            message =
                "차량 정지 여부가 확인되지 않았습니다. " +
                        "관측실 진입을 제한합니다.",
            isDemo = false
        )
    }

    return ObservationAccessResult(
        allowed = true,
        message =
            "차량의 주차 및 정지 상태가 " +
                    "확인되었습니다.",
        isDemo = false
    )
}
