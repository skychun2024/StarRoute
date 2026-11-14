
package com.example.starroute

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservationAccessPolicyTest {

    @Test
    fun demoDrivingMustBeBlocked() {

        val result = checkObservationAccess(
            mode = ObservationMode.DEMO,
            demoParked = false
        )

        assertFalse(result.allowed)
        assertTrue(result.isDemo)
    }

    @Test
    fun demoParkedCanEnter() {

        val result = checkObservationAccess(
            mode = ObservationMode.DEMO,
            demoParked = true
        )

        assertTrue(result.allowed)
        assertTrue(result.isDemo)
    }

    @Test
    fun unknownVehicleStateMustBeBlocked() {

        val result = checkObservationAccess(
            mode = ObservationMode.VEHICLE
        )

        assertFalse(result.allowed)
    }

    @Test
    fun drivingGearMustBeBlocked() {

        val result = checkObservationAccess(
            mode = ObservationMode.VEHICLE,
            vehicleState = ObservationVehicleState(
                gear = StarRouteGear.DRIVE,
                isMoving = false
            )
        )

        assertFalse(result.allowed)
    }

    @Test
    fun reverseGearMustBeBlocked() {

        val result = checkObservationAccess(
            mode = ObservationMode.VEHICLE,
            vehicleState = ObservationVehicleState(
                gear = StarRouteGear.REVERSE,
                isMoving = false
            )
        )

        assertFalse(result.allowed)
    }

    @Test
    fun parkButMovingMustBeBlocked() {

        val result = checkObservationAccess(
            mode = ObservationMode.VEHICLE,
            vehicleState = ObservationVehicleState(
                gear = StarRouteGear.PARK,
                isMoving = true
            )
        )

        assertFalse(result.allowed)
    }

    @Test
    fun parkWithUnknownMotionMustBeBlocked() {

        val result = checkObservationAccess(
            mode = ObservationMode.VEHICLE,
            vehicleState = ObservationVehicleState(
                gear = StarRouteGear.PARK,
                isMoving = null
            )
        )

        assertFalse(result.allowed)
    }

    @Test
    fun confirmedParkedAndStoppedCanEnter() {

        val result = checkObservationAccess(
            mode = ObservationMode.VEHICLE,
            vehicleState = ObservationVehicleState(
                gear = StarRouteGear.PARK,
                isMoving = false
            )
        )

        assertTrue(result.allowed)
        assertFalse(result.isDemo)
    }
}
