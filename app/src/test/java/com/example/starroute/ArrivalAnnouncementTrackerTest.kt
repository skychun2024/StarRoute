
package com.example.starroute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ArrivalAnnouncementTrackerTest {

    @Test
    fun farDistanceDoesNotAnnounce() {
        val tracker = ArrivalAnnouncementTracker()

        assertNull(
            tracker.nextAnnouncement(16.6)
        )
    }

    @Test
    fun tenKmAnnouncementOnlyOnce() {
        val tracker = ArrivalAnnouncementTracker()

        assertEquals(
            ArrivalStage.WITHIN_10_KM,
            tracker.nextAnnouncement(5.5)
        )

        tracker.markRequested(
            ArrivalStage.WITHIN_10_KM
        )

        assertNull(
            tracker.nextAnnouncement(5.5)
        )

        assertTrue(
            tracker.hasAnnounced10Km()
        )
    }

    @Test
    fun twoKmAnnouncementAfterTenKm() {
        val tracker = ArrivalAnnouncementTracker()

        tracker.markRequested(
            ArrivalStage.WITHIN_10_KM
        )

        assertEquals(
            ArrivalStage.WITHIN_2_KM,
            tracker.nextAnnouncement(0.5)
        )

        tracker.markRequested(
            ArrivalStage.WITHIN_2_KM
        )

        assertNull(
            tracker.nextAnnouncement(0.5)
        )
    }

    @Test
    fun startingWithinTwoKmSkipsTenKm() {
        val tracker = ArrivalAnnouncementTracker()

        assertEquals(
            ArrivalStage.WITHIN_2_KM,
            tracker.nextAnnouncement(0.5)
        )

        tracker.markRequested(
            ArrivalStage.WITHIN_2_KM
        )

        assertTrue(tracker.hasAnnounced10Km())
        assertTrue(tracker.hasAnnounced2Km())

        assertNull(
            tracker.nextAnnouncement(5.5)
        )
    }

    @Test
    fun resetAllowsAnnouncementsAgain() {
        val tracker = ArrivalAnnouncementTracker()

        tracker.markRequested(
            ArrivalStage.WITHIN_10_KM
        )

        tracker.reset()

        assertFalse(
            tracker.hasAnnounced10Km()
        )

        assertEquals(
            ArrivalStage.WITHIN_10_KM,
            tracker.nextAnnouncement(5.5)
        )
    }

    @Test
    fun invalidDistanceDoesNotAnnounce() {
        val tracker = ArrivalAnnouncementTracker()

        assertNull(
            tracker.nextAnnouncement(-1.0)
        )

        assertNull(
            tracker.nextAnnouncement(Double.NaN)
        )
    }

    @Test
    fun unconfirmedRequestCanBeRetried() {
        val tracker = ArrivalAnnouncementTracker()

        // 요청 단계만 확인하고 markRequested를
        // 호출하지 않으면 재시도가 가능합니다.
        assertEquals(
            ArrivalStage.WITHIN_10_KM,
            tracker.nextAnnouncement(5.5)
        )

        assertEquals(
            ArrivalStage.WITHIN_10_KM,
            tracker.nextAnnouncement(5.5)
        )
    }
}
