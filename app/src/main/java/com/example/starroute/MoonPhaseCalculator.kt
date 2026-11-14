
package com.example.starroute

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

// 달 위상 계산 결과
data class StarRouteMoonInfo(
    val name: String,
    val symbol: String,
    val description: String,
    val illuminationPercent: Int,
    val moonAgeDays: Double,
    val nearFullMoon: Boolean,
    val daysUntilFullMoon: Double
)

// 평균 삭망월과 기준 신월
private const val MOON_CYCLE_DAYS = 29.530588853
private const val NEW_MOON_REFERENCE_JD = 2451550.1

// 달 위상 계산
// 천문력 기반의 정밀 계산이 아닌 근사 계산입니다.
fun calculateStarRouteMoon(
    timeMillis: Long
): StarRouteMoonInfo {

    val julianDay =
        timeMillis / 86400000.0 + 2440587.5

    val elapsedDays =
        julianDay - NEW_MOON_REFERENCE_JD

    val moonAge =
        ((elapsedDays % MOON_CYCLE_DAYS) +
                MOON_CYCLE_DAYS) % MOON_CYCLE_DAYS

    // 달의 밝은 면적 비율 근사
    val phaseAngle =
        2.0 * PI * moonAge / MOON_CYCLE_DAYS

    val illuminationPercent =
        (((1.0 - cos(phaseAngle)) / 2.0) * 100.0)
            .roundToInt()
            .coerceIn(0, 100)

    // 여덟 가지 달 위상으로 분류
    val phaseIndex =
        floor(
            moonAge / MOON_CYCLE_DAYS * 8.0 + 0.5
        ).toInt() % 8

    val names = listOf(
        "신월",
        "초승달",
        "상현달",
        "차오르는 둥근달",
        "보름달",
        "기우는 둥근달",
        "하현달",
        "그믐달"
    )

    val symbols = listOf(
        "🌑", "🌒", "🌓", "🌔",
        "🌕", "🌖", "🌗", "🌘"
    )

    val descriptions = listOf(
        "달의 밝은 부분이 거의 보이지 않는 시기예요.",
        "가느다란 달이 조금씩 밝아지는 시기예요.",
        "달의 오른쪽 절반이 밝게 보이는 상현달 무렵이에요.",
        "달이 보름달을 향해 점점 둥글어지고 있어요.",
        "둥글고 밝은 보름달 무렵이에요.",
        "보름달을 지나 밝은 부분이 줄어들고 있어요.",
        "달의 왼쪽 절반이 밝게 보이는 하현달 무렵이에요.",
        "다음 신월을 향해 가느다란 달로 변하고 있어요."
    )

    val fullMoonAge = MOON_CYCLE_DAYS / 2.0

    val nearFullMoon =
        abs(moonAge - fullMoonAge) <= 1.0

    // 다음 보름달까지 대략적인 남은 일수
    val daysUntilFullMoon =
        if (moonAge <= fullMoonAge) {
            fullMoonAge - moonAge
        } else {
            MOON_CYCLE_DAYS + fullMoonAge - moonAge
        }

    return StarRouteMoonInfo(
        name = names[phaseIndex],
        symbol = symbols[phaseIndex],
        description = descriptions[phaseIndex],
        illuminationPercent = illuminationPercent,
        moonAgeDays = moonAge,
        nearFullMoon = nearFullMoon,
        daysUntilFullMoon = daysUntilFullMoon
    )
}
