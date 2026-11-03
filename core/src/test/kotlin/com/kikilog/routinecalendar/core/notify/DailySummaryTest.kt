package com.kikilog.routinecalendar.core.notify

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.model.Routine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DailySummaryTest {

    private fun d(s: String) = LocalDate.parse(s)
    private fun dt(s: String) = LocalDateTime.parse(s)

    private val today = d("2026-11-03")

    private fun routine(
        id: Long,
        firstDue: String,
        notify: Boolean = true,
        archived: Boolean = false,
    ) = Routine(
        id = id,
        title = "루틴$id",
        categoryId = Category.ETC,
        interval = Interval(7, IntervalUnit.DAY),
        repeats = true,
        firstDue = d(firstDue),
        notify = notify,
        archived = archived,
    )

    private fun done(routineId: Long, on: String) = Completion(id = 0, routineId = routineId, doneOn = d(on))

    // --- 알림 시각 ---

    @Test
    fun `알림 시각 전이면 오늘 그 시각`() {
        assertEquals(dt("2026-11-03T08:00"), nextNotifyAt(dt("2026-11-03T07:59"), NotifyTime(8, 0)))
    }

    @Test
    fun `알림 시각이 지났거나 바로 그 시각이면 내일`() {
        assertEquals(dt("2026-11-04T08:00"), nextNotifyAt(dt("2026-11-03T08:00"), NotifyTime(8, 0)))
        assertEquals(dt("2026-11-04T08:00"), nextNotifyAt(dt("2026-11-03T08:01"), NotifyTime(8, 0)))
    }

    @Test
    fun `자정 근처 - 23시 50분이면 다음 날, 0시 30분 알림은 같은 날`() {
        assertEquals(dt("2026-11-04T08:00"), nextNotifyAt(dt("2026-11-03T23:50"), NotifyTime(8, 0)))
        assertEquals(dt("2026-11-04T00:30"), nextNotifyAt(dt("2026-11-04T00:00"), NotifyTime(0, 30)))
    }

    @Test
    fun `월말 연말도 다음 날로 넘어간다`() {
        assertEquals(dt("2027-01-01T08:00"), nextNotifyAt(dt("2026-12-31T21:00"), NotifyTime(8, 0)))
    }

    @Test
    fun `고를 수 있는 시각은 30분 간격 48개`() {
        assertEquals(48, NOTIFY_TIME_OPTIONS.size)
        assertEquals(NotifyTime(0, 0), NOTIFY_TIME_OPTIONS.first())
        assertEquals(NotifyTime(23, 30), NOTIFY_TIME_OPTIONS.last())
        assertTrue(DEFAULT_NOTIFY_TIME in NOTIFY_TIME_OPTIONS)
        assertEquals(NotifyTime(8, 0), DEFAULT_NOTIFY_TIME)
    }

    @Test
    fun `HH mm 형식으로 저장하고 읽는다`() {
        assertEquals("08:00", NotifyTime(8, 0).encode())
        assertEquals("21:30", NotifyTime(21, 30).encode())
        assertEquals(NotifyTime(21, 30), NotifyTime.decode("21:30"))
        assertNull(NotifyTime.decode("24:00"))
        assertNull(NotifyTime.decode("8시"))
        assertNull(NotifyTime.decode(""))
    }

    // --- 요약 대상 ---

    @Test
    fun `지난 것과 오늘만 넣고 다가오는 7일은 뺀다`() {
        val summary = buildDailySummary(
            listOf(
                routine(1, firstDue = "2026-11-01") to emptyList(),
                routine(2, firstDue = "2026-11-03") to emptyList(),
                routine(3, firstDue = "2026-11-05") to emptyList(),
            ),
            today,
        )
        assertEquals(listOf(1L), summary.overdue.map { it.id })
        assertEquals(listOf(2L), summary.dueToday.map { it.id })
        assertFalse(summary.isEmpty)
    }

    @Test
    fun `알림에 넣기를 끈 루틴과 보관한 루틴은 뺀다`() {
        val summary = buildDailySummary(
            listOf(
                routine(1, firstDue = "2026-11-03", notify = false) to emptyList(),
                routine(2, firstDue = "2026-11-03", archived = true) to emptyList(),
            ),
            today,
        )
        assertTrue(summary.isEmpty)
    }

    @Test
    fun `오늘 이미 체크한 루틴은 뺀다`() {
        val summary = buildDailySummary(
            listOf(
                routine(1, firstDue = "2026-11-03") to listOf(done(1, "2026-11-03")),
                routine(2, firstDue = "2026-11-01") to listOf(done(2, "2026-11-03")),
            ),
            today,
        )
        assertTrue(summary.isEmpty)
    }

    @Test
    fun `할 것이 없는 날은 비어 있다`() {
        assertTrue(buildDailySummary(emptyList(), today).isEmpty)
        assertTrue(buildDailySummary(listOf(routine(1, firstDue = "2026-11-20") to emptyList()), today).isEmpty)
    }
}
