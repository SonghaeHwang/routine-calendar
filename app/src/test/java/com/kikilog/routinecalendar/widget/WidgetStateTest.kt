package com.kikilog.routinecalendar.widget

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.DEFAULT_CATEGORIES
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.model.Routine
import com.kikilog.routinecalendar.data.RoutineWithCompletions
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetStateTest {

    private val today = LocalDate.parse("2026-11-03")

    private fun item(
        id: Long,
        title: String,
        firstDue: String,
        category: Long = Category.HOUSE,
        archived: Boolean = false,
        doneOn: List<String> = emptyList(),
    ) = RoutineWithCompletions(
        routine = Routine(
            id = id,
            title = title,
            categoryId = category,
            interval = Interval(7, IntervalUnit.DAY),
            repeats = true,
            firstDue = LocalDate.parse(firstDue),
            notify = true,
            archived = archived,
        ),
        completions = doneOn.mapIndexed { i, d -> Completion(id * 100 + i, id, LocalDate.parse(d)) },
    )

    @Test
    fun `지난 것 먼저 그다음 오늘, 다가오는 것은 넣지 않는다`() {
        val state = buildWidgetState(
            listOf(
                item(1, "모델링팩", "2026-11-03"),
                item(2, "각질제거", "2026-10-31"),
                item(3, "이불 세탁", "2026-11-05"),
            ),
            DEFAULT_CATEGORIES,
            today,
        )

        assertEquals(listOf("각질제거", "모델링팩"), state.rows.map { it.title })
        assertEquals(3, state.rows[0].overdueDays)
        assertNull(state.next)
    }

    @Test
    fun `오늘 체크한 줄은 체크된 채로 남는다`() {
        val state = buildWidgetState(listOf(item(1, "빨래", "2026-11-01", doneOn = listOf("2026-11-03"))), DEFAULT_CATEGORIES, today)

        assertEquals(1, state.rows.size)
        assertTrue(state.rows.single().doneToday)
    }

    @Test
    fun `줄마다 분류 색, 지워진 분류면 null`() {
        val state = buildWidgetState(
            listOf(item(1, "젤팩", "2026-11-03", category = Category.BEAUTY), item(2, "무엇", "2026-11-03", category = 99)),
            DEFAULT_CATEGORIES,
            today,
        )

        val beauty = DEFAULT_CATEGORIES.first { it.id == Category.BEAUTY }.colorArgb
        assertEquals(beauty, state.rows.first { it.title == "젤팩" }.colorArgb)
        assertNull(state.rows.first { it.title == "무엇" }.colorArgb)
    }

    @Test
    fun `오늘 할 것이 없으면 가장 가까운 다음 예정 하나`() {
        val state = buildWidgetState(
            listOf(item(1, "이불 세탁", "2026-12-01"), item(2, "세탁조 청소", "2026-11-20"), item(3, "보관함", "2026-11-04", archived = true)),
            DEFAULT_CATEGORIES,
            today,
        )

        assertTrue(state.rows.isEmpty())
        assertEquals(WidgetNext("세탁조 청소", LocalDate.parse("2026-11-20")), state.next)
        assertTrue(state.hasAnyRoutine)
    }

    @Test
    fun `보관한 루틴만 있으면 등록한 루틴이 없는 것으로 본다`() {
        val state = buildWidgetState(listOf(item(1, "보관함", "2026-11-03", archived = true)), DEFAULT_CATEGORIES, today)

        assertTrue(state.rows.isEmpty())
        assertNull(state.next)
        assertFalse(state.hasAnyRoutine)
    }
}
