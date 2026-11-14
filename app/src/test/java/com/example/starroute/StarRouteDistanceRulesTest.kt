
package com.example.starroute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarRouteDistanceRulesTest {

    @Test
    fun farDistanceTest() {
        // 목적지 16.6km 전: 안내하지 않음
        assertEquals(
            ArrivalStage.FAR,
            getArrivalStage(16.6)
        )
    }

    @Test
    fun tenKmDistanceTest() {
        // 10km 경계 및 5.5km 위치 확인
        assertEquals(
            ArrivalStage.WITHIN_10_KM,
            getArrivalStage(10.0)
        )

        assertEquals(
            ArrivalStage.WITHIN_10_KM,
            getArrivalStage(5.5)
        )
    }

    @Test
    fun twoKmDistanceTest() {
        // 2km 경계 및 0.5km 위치 확인
        assertEquals(
            ArrivalStage.WITHIN_2_KM,
            getArrivalStage(2.0)
        )

        assertEquals(
            ArrivalStage.WITHIN_2_KM,
            getArrivalStage(0.5)
        )
    }

    @Test
    fun invalidDistanceTest() {
        // 잘못된 거리 입력 처리
        assertEquals(
            ArrivalStage.INVALID,
            getArrivalStage(-1.0)
        )

        assertEquals(
            ArrivalStage.INVALID,
            getArrivalStage(Double.NaN)
        )

        assertEquals(
            ArrivalStage.INVALID,
            getArrivalStage(Double.POSITIVE_INFINITY)
        )
    }

    @Test
    fun messageTest() {
        // 거리별 메시지 확인
        val message = getArrivalStageMessage(
            "보현산 일대",
            0.5
        )

        assertTrue(message.contains("보현산 일대"))
        assertTrue(message.contains("정차"))

        val farMessage = getArrivalStageMessage(
            "보현산 일대",
            16.6
        )

        assertFalse(farMessage.contains("도착했어요"))
    }

    @Test
    fun allRulesTest() {
        // 기존 공통 거리 규칙 점검
        assertTrue(checkArrivalDistanceRules())
    }
}
