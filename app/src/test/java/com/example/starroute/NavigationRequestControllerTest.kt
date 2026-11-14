
package com.example.starroute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationRequestControllerTest {

    @Test
    fun firstRequestSucceeds() {

        val controller =
            NavigationRequestController()

        var callCount = 0

        val result = controller.requestRoute(
            destinationKey = "spot-a"
        ) {
            callCount++
        }

        assertEquals(
            NavigationRequestResult.REQUEST_SENT,
            result
        )

        assertEquals(1, callCount)

        assertEquals(
            NavigationRequestState.REQUEST_SENT,
            controller.getInfo().state
        )
    }

    @Test
    fun duplicateRequestIsBlocked() {

        val controller =
            NavigationRequestController()

        var callCount = 0

        controller.requestRoute("spot-a") {
            callCount++
        }

        val result =
            controller.requestRoute("spot-a") {
                callCount++
            }

        assertEquals(
            NavigationRequestResult.ALREADY_REQUESTED,
            result
        )

        assertEquals(1, callCount)
    }

    @Test
    fun anotherDestinationCanBeRequested() {

        val controller =
            NavigationRequestController()

        var callCount = 0

        controller.requestRoute("spot-a") {
            callCount++
        }

        val result =
            controller.requestRoute("spot-b") {
                callCount++
            }

        assertEquals(
            NavigationRequestResult.REQUEST_SENT,
            result
        )

        assertEquals(2, callCount)

        assertEquals(
            "spot-b",
            controller.getInfo().destinationKey
        )
    }

    @Test
    fun invalidDestinationIsBlocked() {

        val controller =
            NavigationRequestController()

        var called = false

        val result =
            controller.requestRoute("   ") {
                called = true
            }

        assertEquals(
            NavigationRequestResult.INVALID_DESTINATION,
            result
        )

        assertFalse(called)
    }

    @Test
    fun sdkErrorAllowsRetry() {

        val controller =
            NavigationRequestController()

        val first =
            controller.requestRoute("spot-a") {
                throw IllegalStateException(
                    "Mock SDK error"
                )
            }

        assertEquals(
            NavigationRequestResult.SDK_ERROR,
            first
        )

        assertEquals(
            NavigationRequestState.IDLE,
            controller.getInfo().state
        )

        val second =
            controller.requestRoute("spot-a") {
                // 재시도 성공을 가정
            }

        assertEquals(
            NavigationRequestResult.REQUEST_SENT,
            second
        )
    }

    @Test
    fun resetAllowsSameDestinationRetry() {

        val controller =
            NavigationRequestController()

        controller.requestRoute("spot-a") {
            // 첫 요청
        }

        assertTrue(controller.reset())

        val result =
            controller.requestRoute("spot-a") {
                // 두 번째 요청
            }

        assertEquals(
            NavigationRequestResult.REQUEST_SENT,
            result
        )
    }

    @Test
    fun requestInProgressIsBlocked() {

        val controller =
            NavigationRequestController()

        var secondActionCalled = false

        controller.requestRoute("spot-a") {

            val secondResult =
                controller.requestRoute("spot-b") {
                    secondActionCalled = true
                }

            assertEquals(
                NavigationRequestResult.REQUEST_IN_PROGRESS,
                secondResult
            )

            assertFalse(secondActionCalled)
        }

        assertEquals(
            NavigationRequestState.REQUEST_SENT,
            controller.getInfo().state
        )
    }

    @Test
    fun resetDuringRequestIsBlocked() {

        val controller =
            NavigationRequestController()

        controller.requestRoute("spot-a") {

            val resetResult = controller.reset()

            assertFalse(resetResult)

            assertEquals(
                NavigationRequestState.REQUESTING,
                controller.getInfo().state
            )
        }

        assertEquals(
            NavigationRequestState.REQUEST_SENT,
            controller.getInfo().state
        )
    }
}
