package com.kikilog.routinecalendar.data

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.model.Routine
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Android 가 필요 없는 일반 JUnit 시험. */
class MappersTest {

    private fun d(s: String) = LocalDate.parse(s)

    private val entity = RoutineEntity(
        id = 7,
        title = "이불 세탁",
        category = Category.HOUSE,
        intervalAmount = 2,
        intervalUnit = IntervalUnit.MONTH,
        repeats = true,
        firstDue = d("2026-11-06"),
        notify = false,
        archived = true,
        createdAt = 1_000L,
    )

    @Test
    fun `루틴 엔티티를 모델로`() {
        assertEquals(
            Routine(
                id = 7,
                title = "이불 세탁",
                category = Category.HOUSE,
                interval = Interval(2, IntervalUnit.MONTH),
                repeats = true,
                firstDue = d("2026-11-06"),
                notify = false,
                archived = true,
            ),
            entity.toModel(),
        )
    }

    @Test
    fun `완료 기록 엔티티를 모델로`() {
        assertEquals(
            Completion(id = 3, routineId = 7, doneOn = d("2026-11-02")),
            CompletionEntity(id = 3, routineId = 7, doneOn = d("2026-11-02"), createdAt = 2_000L).toModel(),
        )
    }

    @Test
    fun `함께 조회한 결과는 완료일 순으로 정렬`() {
        val row = RoutineWithCompletionsRow(
            routine = entity,
            completions = listOf(
                CompletionEntity(id = 1, routineId = 7, doneOn = d("2026-11-02"), createdAt = 0),
                CompletionEntity(id = 2, routineId = 7, doneOn = d("2026-09-01"), createdAt = 0),
                CompletionEntity(id = 3, routineId = 7, doneOn = d("2026-10-15"), createdAt = 0),
            ),
        )
        assertEquals(
            listOf(d("2026-09-01"), d("2026-10-15"), d("2026-11-02")),
            row.toModel().completions.map { it.doneOn },
        )
    }

    @Test
    fun `등록 값을 엔티티로 - 보관 아님, 첫 예정일과 생성 시각은 받은 값`() {
        val draft = RoutineDraft("이불 세탁", Category.HOUSE, Interval(2, IntervalUnit.MONTH), repeats = true, notify = false)
        assertEquals(
            entity.copy(id = 0, archived = false, createdAt = 9_000L),
            draft.toEntity(firstDue = d("2026-11-06"), createdAt = 9_000L),
        )
    }

    @Test
    fun `수정 값을 열 묶음으로`() {
        val draft = RoutineDraft("젤팩", Category.BEAUTY, Interval(1, IntervalUnit.WEEK), repeats = false, notify = true)
        assertEquals(
            RoutineEditColumns(7, "젤팩", Category.BEAUTY, 1, IntervalUnit.WEEK, repeats = false, notify = true),
            draft.toEditColumns(7),
        )
    }

    @Test
    fun `LocalDate 는 ISO 문자열로 저장하고 되돌린다`() {
        val converters = Converters()
        for (s in listOf("2026-11-02", "2028-02-29", "2026-01-01", "2026-12-31")) {
            assertEquals(s, converters.fromLocalDate(d(s)))
            assertEquals(d(s), converters.toLocalDate(s))
        }
    }

    @Test
    fun `ISO 문자열 순서가 날짜 순서와 같다`() {
        val dates = listOf(d("2026-11-02"), d("2026-02-28"), d("2027-01-01"), d("2026-10-31"))
        val converters = Converters()
        assertEquals(dates.sorted(), dates.sortedBy { converters.fromLocalDate(it) })
    }
}
