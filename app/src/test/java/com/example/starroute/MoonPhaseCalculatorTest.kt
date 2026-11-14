
package com.example.starroute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

import kotlin.math.roundToLong

class MoonPhaseCalculatorTest {

    // 달 위상 계산기에 사용한 기준 신월
    private val referenceNewMoon =
        ((2451550.1 - 2440587.5) *
                86400000.0).roundToLong()

    // 평균 달 주기
    private val moonCycle = 29.530588853

    // 기준 신월에서 지정한 날짜만큼 이동
    private fun timeAfterDays(
        days: Double
    ): Long {
        return referenceNewMoon +
                (days * 86400000.0).roundToLong()
    }

    // 1. 신월 테스트
    @Test
    fun newMoonTest() {

        val moon = calculateStarRouteMoon(
            timeAfterDays(0.0)
        )

        assertEquals("신월", moon.name)

        assertTrue(
            moon.illuminationPercent <= 1
        )

        assertFalse(moon.nearFullMoon)
    }

    // 2. 상현달 테스트
    @Test
    fun firstQuarterTest() {

        val moon = calculateStarRouteMoon(
            timeAfterDays(moonCycle / 4.0)
        )

        assertEquals("상현달", moon.name)

        assertTrue(
            moon.illuminationPercent in 45..55
        )
    }

    // 3. 보름달 테스트
    @Test
    fun fullMoonTest() {

        val moon = calculateStarRouteMoon(
            timeAfterDays(moonCycle / 2.0)
        )

        assertEquals("보름달", moon.name)

        assertTrue(moon.nearFullMoon)

        assertTrue(
            moon.illuminationPercent >= 99
        )
    }

    // 4. 하현달 테스트
    @Test
    fun lastQuarterTest() {

        val moon = calculateStarRouteMoon(
            timeAfterDays(moonCycle * 3.0 / 4.0)
        )

        assertEquals("하현달", moon.name)

        assertTrue(
            moon.illuminationPercent in 45..55
        )
    }

    // 5. 달 밝기 비율 범위 테스트
    @Test
    fun illuminationRangeTest() {

        for (day in 0..29) {

            val moon = calculateStarRouteMoon(
                timeAfterDays(day.toDouble())
            )

            assertTrue(
                moon.illuminationPercent in 0..100
            )
        }
    }

    // 6. 보름달 근처 메시지 조건 테스트
    @Test
    fun nearFullMoonTest() {

        val fullMoonDay = moonCycle / 2.0

        val nearMoon = calculateStarRouteMoon(
            timeAfterDays(fullMoonDay - 0.5)
        )

        assertTrue(nearMoon.nearFullMoon)

        val farMoon = calculateStarRouteMoon(
            timeAfterDays(fullMoonDay - 3.0)
        )

        assertFalse(farMoon.nearFullMoon)
    }

    // 7. 다음 보름달까지 남은 날짜
    @Test
    fun nextFullMoonTest() {

        val moon = calculateStarRouteMoon(
            timeAfterDays(0.0)
        )

        assertEquals(
            moonCycle / 2.0,
            moon.daysUntilFullMoon,
            0.01
        )
    }

    // 8. 달 이름과 이모지 확인
    @Test
    fun moonSymbolTest() {

        val moon = calculateStarRouteMoon(
            timeAfterDays(moonCycle / 2.0)
        )

        assertEquals("🌕", moon.symbol)

        assertTrue(
            moon.description.isNotEmpty()
        )
    }
}
