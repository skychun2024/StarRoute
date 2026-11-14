
package com.example.starroute

/**
 * 별빛도감 기록 관리 규칙
 *
 * Android나 Pleos SDK에 의존하지 않아
 * 시뮬레이터 없이 자동 테스트할 수 있습니다.
 */

data class StarDiaryEntry(
    val starId: String,
    val spot: String,
    val day: String,
    val time: Long,
    val note: String = ""
)

enum class StarDiarySaveStatus {
    SUCCESS,
    DUPLICATE,
    INVALID,
    NOTE_TOO_LONG
}

data class StarDiarySaveResult(
    val status: StarDiarySaveStatus,
    val records: List<StarDiaryEntry>
)

data class StarDiaryDeleteResult(
    val deleted: Boolean,
    val records: List<StarDiaryEntry>
)

private const val MAX_DIARY_NOTE_LENGTH = 300

/**
 * 동일한 날짜, 장소, 별자리로 저장한
 * 기록이 있는지 검사합니다.
 */
fun isDuplicateStarDiaryEntry(
    existing: List<StarDiaryEntry>,
    candidate: StarDiaryEntry
): Boolean {

    return existing.any { record ->
        record.starId == candidate.starId &&
                record.spot == candidate.spot &&
                record.day == candidate.day
    }
}

/**
 * 새로운 체험 기록을 목록에 추가합니다.
 *
 * 이 함수는 실제 디스크에 저장하지 않습니다.
 * 기존 목록을 변경하지 않고 새 목록을 반환합니다.
 */
fun addStarDiaryEntry(
    existing: List<StarDiaryEntry>,
    candidate: StarDiaryEntry
): StarDiarySaveResult {

    if (
        candidate.starId.isBlank() ||
        candidate.spot.isBlank() ||
        candidate.day.isBlank()
    ) {

        return StarDiarySaveResult(
            status = StarDiarySaveStatus.INVALID,
            records = existing
        )
    }

    if (candidate.note.length > MAX_DIARY_NOTE_LENGTH) {

        return StarDiarySaveResult(
            status = StarDiarySaveStatus.NOTE_TOO_LONG,
            records = existing
        )
    }

    if (isDuplicateStarDiaryEntry(existing, candidate)) {

        return StarDiarySaveResult(
            status = StarDiarySaveStatus.DUPLICATE,
            records = existing
        )
    }

    val cleaned = candidate.copy(
        note = candidate.note.trim()
    )

    val updated = (existing + cleaned)
        .sortedByDescending { it.time }

    return StarDiarySaveResult(
        status = StarDiarySaveStatus.SUCCESS,
        records = updated
    )
}

/**
 * 사용자가 확인한 기록만 삭제합니다.
 *
 * confirmed가 false이면 기록을 변경하지 않습니다.
 */
fun deleteStarDiaryEntry(
    existing: List<StarDiaryEntry>,
    target: StarDiaryEntry,
    confirmed: Boolean
): StarDiaryDeleteResult {

    if (!confirmed) {

        return StarDiaryDeleteResult(
            deleted = false,
            records = existing
        )
    }

    if (!existing.contains(target)) {

        return StarDiaryDeleteResult(
            deleted = false,
            records = existing
        )
    }

    val updated = existing.filterNot {
        it == target
    }

    return StarDiaryDeleteResult(
        deleted = true,
        records = updated
    )
}

/**
 * 별빛도감에서 수집한 별자리 개수
 */
fun countCollectedStars(
    records: List<StarDiaryEntry>
): Int {

    return records
        .map { it.starId }
        .filter { it.isNotBlank() }
        .distinct()
        .size
}
