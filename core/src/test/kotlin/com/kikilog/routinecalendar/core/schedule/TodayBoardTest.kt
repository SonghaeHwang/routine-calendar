package com.kikilog.routinecalendar.core.schedule

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.model.Routine
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayBoardTest {

    private fun d(s: String) = LocalDate.parse(s)

    /** 2026-11-02 는 월요일. 다가오는 7일은 11-03 ~ 11-09. */
    private val monday = d("2026-11-02")

    private fun routine(
        id: Long,
        title: String = "루틴$id",
        firstDue: String,
        amount: Int = 7,
        repeats: Boolean = true,
        archived: Boolean = false,
    ) = Routine(
        id = id,
        title = title,
        category = Category.ETC,
        interval = Interval(amount, IntervalUnit.DAY),
        repeats = repeats,
        firstDue = d(firstDue),
        notify = true,
        archived = archived,
    )

    private fun done(routineId: Long, on: String) = Completion(id = 0, routineId = routineId, doneOn = d(on))

    private fun board(vararg items: Pair<Routine, List<Completion>>, today: LocalDate = monday) =
        buildTodayBoard(items.toList(), today)

    private fun List<TodayEntry>.ids() = map { it.routine.id }

    @Test
    fun `지난 것 · 오늘 · 다가오는 7일로 나뉜다`() {
        val b = board(
            routine(1, firstDue = "2026-10-30") to emptyList(),
            routine(2, firstDue = "2026-11-02") to emptyList(),
            routine(3, firstDue = "2026-11-05") to emptyList(),
        )
        assertEquals(listOf(1L), b.overdue.ids())
        assertEquals(3, b.overdue.single().overdueDays)
        assertEquals(listOf(2L), b.dueToday.ids())
        assertEquals(listOf(3L), b.upcoming.ids())
    }

    @Test
    fun `다가오는 7일은 7일 뒤까지 포함하고 8일 뒤는 뺀다`() {
        val b = board(
            routine(1, firstDue = "2026-11-09") to emptyList(),
            routine(2, firstDue = "2026-11-10") to emptyList(),
        )
        assertEquals(listOf(1L), b.upcoming.ids())
        assertTrue(b.dueToday.isEmpty() && b.overdue.isEmpty())
    }

    @Test
    fun `토요일에도 다음 주 루틴이 보인다 - 요일과 상관없음`() {
        val b = board(
            routine(1, firstDue = "2026-11-08") to emptyList(),
            routine(2, firstDue = "2026-11-14") to emptyList(),
            today = d("2026-11-07"),
        )
        assertEquals(listOf(1L, 2L), b.upcoming.ids())
    }

    @Test
    fun `어제 한 1주 루틴을 등록하면 다가오는 7일에 보인다`() {
        // 화요일에 「어제(월) 했음, 1주마다」로 등록 → 다음 예정 다음 주 월요일
        val b = board(routine(1, firstDue = "2026-11-09") to listOf(done(1, "2026-11-02")), today = d("2026-11-03"))
        assertEquals(listOf(1L), b.upcoming.ids())
        assertEquals(d("2026-11-09"), b.upcoming.single().due)
    }

    @Test
    fun `오늘 체크하면 오늘 칸에 체크된 채로 남는다`() {
        // 지난 것이던 루틴을 오늘 체크 → 다음 예정일은 11-09 이지만 오늘 칸에 남음
        val b = board(routine(1, firstDue = "2026-10-30") to listOf(done(1, "2026-11-02")))
        assertEquals(listOf(1L), b.dueToday.ids())
        assertTrue(b.dueToday.single().doneToday)
        assertEquals(monday, b.dueToday.single().due)
        assertTrue(b.overdue.isEmpty() && b.upcoming.isEmpty())
    }

    @Test
    fun `다가오는 루틴을 일찍 체크해도 오늘 칸으로 옮겨 온다`() {
        val b = board(routine(1, firstDue = "2026-11-06") to listOf(done(1, "2026-11-02")))
        assertEquals(listOf(1L), b.dueToday.ids())
        assertTrue(b.upcoming.isEmpty())
    }

    @Test
    fun `반복 없는 루틴도 오늘 끝냈으면 오늘 칸에 남고, 지난날 끝냈으면 빠진다`() {
        val today = board(routine(1, firstDue = "2026-11-02", repeats = false) to listOf(done(1, "2026-11-02")))
        assertEquals(listOf(1L), today.dueToday.ids())
        assertTrue(today.dueToday.single().doneToday)

        val earlier = board(routine(1, firstDue = "2026-11-01", repeats = false) to listOf(done(1, "2026-11-01")))
        assertTrue(earlier.isEmpty)
    }

    @Test
    fun `체크를 취소하면 원래 칸으로 돌아간다`() {
        val r = routine(1, firstDue = "2026-10-30")
        assertTrue(board(r to listOf(done(1, "2026-11-02"))).dueToday.single().doneToday)

        val b = board(r to emptyList())
        assertEquals(listOf(1L), b.overdue.ids())
        assertTrue(b.dueToday.isEmpty())
    }

    @Test
    fun `보관한 루틴은 빠진다`() {
        assertTrue(board(routine(1, firstDue = "2026-11-02", archived = true) to emptyList()).isEmpty)
    }

    @Test
    fun `다른 루틴의 오늘 기록은 영향을 주지 않는다`() {
        val b = board(routine(1, firstDue = "2026-10-30") to listOf(done(2, "2026-11-02")))
        assertEquals(listOf(1L), b.overdue.ids())
    }

    @Test
    fun `지난 것은 많이 지난 순, 같으면 이름 순`() {
        val b = board(
            routine(1, title = "나", firstDue = "2026-10-31") to emptyList(),
            routine(2, title = "다", firstDue = "2026-10-25") to emptyList(),
            routine(3, title = "가", firstDue = "2026-10-31") to emptyList(),
        )
        assertEquals(listOf(2L, 3L, 1L), b.overdue.ids())
        assertEquals(listOf(8, 2, 2), b.overdue.map { it.overdueDays })
    }

    @Test
    fun `오늘 칸은 안 한 것 먼저, 같으면 이름 순`() {
        val b = board(
            routine(1, title = "가", firstDue = "2026-11-02") to listOf(done(1, "2026-11-02")),
            routine(2, title = "다", firstDue = "2026-11-02") to emptyList(),
            routine(3, title = "나", firstDue = "2026-11-02") to emptyList(),
        )
        assertEquals(listOf(3L, 2L, 1L), b.dueToday.ids())
    }

    @Test
    fun `다가오는 7일은 예정일 순, 같으면 이름 순`() {
        val b = board(
            routine(1, title = "가", firstDue = "2026-11-06") to emptyList(),
            routine(2, title = "다", firstDue = "2026-11-04") to emptyList(),
            routine(3, title = "나", firstDue = "2026-11-04") to emptyList(),
        )
        assertEquals(listOf(3L, 2L, 1L), b.upcoming.ids())
    }

    @Test
    fun `루틴이 없으면 빈 화면`() {
        assertEquals(TodayBoard.EMPTY, board())
    }
}
