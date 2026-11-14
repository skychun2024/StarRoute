
package com.example.starroute

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StarDiaryRulesTest {

    private val firstRecord = StarDiaryEntry(
        starId = "orion",
        spot = "보현산 일대",
        day = "2026.11.11",
        time = 1000L,
        note = "오늘의 별자리 여행"
    )

    @Test
    fun saveNewRecordTest() {

        val result = addStarDiaryEntry(
            existing = emptyList(),
            candidate = firstRecord
        )

        assertEquals(
            StarDiarySaveStatus.SUCCESS,
            result.status
        )

        assertEquals(1, result.records.size)
    }

    @Test
    fun duplicateRecordMustBeBlockedTest() {

        val duplicate = firstRecord.copy(
            time = 2000L,
            note = "다른 메모"
        )

        val result = addStarDiaryEntry(
            existing = listOf(firstRecord),
            candidate = duplicate
        )

        assertEquals(
            StarDiarySaveStatus.DUPLICATE,
            result.status
        )

        assertEquals(1, result.records.size)
    }

    @Test
    fun differentDayCanBeSavedTest() {

        val nextDay = firstRecord.copy(
            day = "2026.11.12",
            time = 2000L
        )

        val result = addStarDiaryEntry(
            existing = listOf(firstRecord),
            candidate = nextDay
        )

        assertEquals(
            StarDiarySaveStatus.SUCCESS,
            result.status
        )

        assertEquals(2, result.records.size)
    }

    @Test
    fun differentStarCanBeSavedTest() {

        val secondRecord = firstRecord.copy(
            starId = "cassiopeia",
            time = 2000L
        )

        val result = addStarDiaryEntry(
            existing = listOf(firstRecord),
            candidate = secondRecord
        )

        assertEquals(
            StarDiarySaveStatus.SUCCESS,
            result.status
        )

        assertEquals(2, result.records.size)
    }

    @Test
    fun noteOverLimitMustBeBlockedTest() {

        val longNote = firstRecord.copy(
            note = "별".repeat(301)
        )

        val result = addStarDiaryEntry(
            existing = emptyList(),
            candidate = longNote
        )

        assertEquals(
            StarDiarySaveStatus.NOTE_TOO_LONG,
            result.status
        )

        assertTrue(result.records.isEmpty())
    }

    @Test
    fun cancelDeleteMustKeepRecordTest() {

        val result = deleteStarDiaryEntry(
            existing = listOf(firstRecord),
            target = firstRecord,
            confirmed = false
        )

        assertFalse(result.deleted)
        assertEquals(1, result.records.size)
    }

    @Test
    fun confirmedDeleteRemovesRecordTest() {

        val result = deleteStarDiaryEntry(
            existing = listOf(firstRecord),
            target = firstRecord,
            confirmed = true
        )

        assertTrue(result.deleted)
        assertTrue(result.records.isEmpty())
    }

    @Test
    fun unknownRecordCannotBeDeletedTest() {

        val unknown = firstRecord.copy(
            starId = "ursa_major"
        )

        val result = deleteStarDiaryEntry(
            existing = listOf(firstRecord),
            target = unknown,
            confirmed = true
        )

        assertFalse(result.deleted)
        assertEquals(1, result.records.size)
    }

    @Test
    fun collectedStarCountTest() {

        val records = listOf(
            firstRecord,
            firstRecord.copy(
                day = "2026.11.12",
                time = 2000L
            ),
            firstRecord.copy(
                starId = "cassiopeia",
                time = 3000L
            )
        )

        assertEquals(
            2,
            countCollectedStars(records)
        )
    }

    @Test
    fun newestRecordComesFirstTest() {

        val newer = firstRecord.copy(
            day = "2026.11.12",
            time = 3000L
        )

        val result = addStarDiaryEntry(
            existing = listOf(firstRecord),
            candidate = newer
        )

        assertEquals(
            3000L,
            result.records.first().time
        )
    }
}
