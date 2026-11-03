package com.kikilog.routinecalendar.ui.routines

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.model.Routine
import com.kikilog.routinecalendar.data.RoutineWithCompletions
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoutineRowsTest {

    private fun d(s: String) = LocalDate.parse(s)

    private fun item(
        id: Long,
        title: String,
        firstDue: String,
        repeats: Boolean = true,
        done: List<String> = emptyList(),
    ) = RoutineWithCompletions(
        routine = Routine(
            id = id,
            title = title,
            categoryId = Category.ETC,
            interval = Interval(7, IntervalUnit.DAY),
            repeats = repeats,
            firstDue = d(firstDue),
            notify = true,
            archived = false,
        ),
        completions = done.map { Completion(id = 0, routineId = id, doneOn = d(it)) },
    )

    @Test
    fun `마지막으로 한 날과 다음 예정일을 채운다`() {
        val row = buildRoutineRows(listOf(item(1, "각질제거", "2026-10-01", done = listOf("2026-10-20", "2026-10-27")))).single()
        assertEquals(d("2026-10-27"), row.lastDoneOn)
        assertEquals(d("2026-11-03"), row.nextDue)
    }

    @Test
    fun `기록이 없으면 마지막은 비고 다음은 첫 예정일`() {
        val row = buildRoutineRows(listOf(item(1, "청소기", "2026-11-05"))).single()
        assertNull(row.lastDoneOn)
        assertEquals(d("2026-11-05"), row.nextDue)
    }

    @Test
    fun `다음 예정일 순, 끝난 것은 맨 아래, 같으면 이름 순`() {
        val rows = buildRoutineRows(
            listOf(
                item(1, "화장지", "2026-11-01", repeats = false, done = listOf("2026-11-01")), // 끝남
                item(2, "다", "2026-11-10"),
                item(3, "나", "2026-11-04"),
                item(4, "가", "2026-11-10"),
            ),
        )
        assertEquals(listOf(3L, 4L, 2L, 1L), rows.map { it.id })
        assertNull(rows.last().nextDue)
    }
}
