package com.kikilog.routinecalendar.core.schedule

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.model.Routine
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarTest {

    private fun d(s: String) = LocalDate.parse(s)

    private fun routine(
        id: Long,
        firstDue: String,
        amount: Int = 7,
        repeats: Boolean = true,
        archived: Boolean = false,
        title: String = "루틴$id",
    ) = Routine(id, title, Category.ETC, Interval(amount, IntervalUnit.DAY), repeats, d(firstDue), notify = true, archived = archived)

    private fun done(routineId: Long, on: String) = Completion(0, routineId, d(on))

    // ── 달력 칸 ──

    @Test
    fun `2026년 11월 - 일요일 시작이면 11월 1일(일)부터 12월 5일(토)까지 5주`() {
        val dates = monthGridDates(d("2026-11-01"), DayOfWeek.SUNDAY)
        assertEquals(d("2026-11-01"), dates.first())
        assertEquals(d("2026-12-05"), dates.last())
        assertEquals(35, dates.size)
    }

    @Test
    fun `월요일 시작이면 앞에 10월 26일(월)부터 채운다`() {
        val dates = monthGridDates(d("2026-11-01"), DayOfWeek.MONDAY)
        assertEquals(d("2026-10-26"), dates.first())
        assertEquals(d("2026-12-06"), dates.last())
        assertEquals(0, dates.size % 7)
    }

    @Test
    fun `2월이 일요일에 시작해 토요일에 끝나면 딱 4주`() {
        // 2026-02-01 은 일요일, 02-28 은 토요일
        val dates = monthGridDates(d("2026-02-01"), DayOfWeek.SUNDAY)
        assertEquals(28, dates.size)
        assertEquals(d("2026-02-28"), dates.last())
    }

    @Test
    fun `요일 머리글은 주 시작 요일부터`() {
        assertEquals(DayOfWeek.SUNDAY, weekDays(DayOfWeek.SUNDAY).first())
        assertEquals(DayOfWeek.SATURDAY, weekDays(DayOfWeek.SUNDAY).last())
        assertEquals(listOf(DayOfWeek.MONDAY, DayOfWeek.TUESDAY), weekDays(DayOfWeek.MONDAY).take(2))
        assertEquals(DayOfWeek.SUNDAY, weekDays(DayOfWeek.MONDAY).last())
    }

    @Test
    fun `달 옮기기 - 해를 넘어도 1일`() {
        assertEquals(d("2027-01-01"), shiftMonth(d("2026-12-01"), 1))
        assertEquals(d("2026-10-01"), shiftMonth(d("2026-11-01"), -1))
        assertEquals(d("2026-11-01"), firstOfMonth(d("2026-11-17")))
    }

    // ── 예정 · 완료 표시 ──

    private val november = d("2026-11-01")..d("2026-11-30")

    @Test
    fun `예정은 다음 예정일부터 주기씩, 완료는 한 날에`() {
        val r = routine(1, firstDue = "2026-10-01")
        val days = buildCalendarDays(listOf(r to listOf(done(1, "2026-10-28"), done(1, "2026-11-01"))), november)

        assertEquals(setOf(1L), days[d("2026-11-01")]!!.doneIds)
        assertTrue(days[d("2026-11-01")]!!.dueIds.isEmpty())
        // 11-01 에 했으므로 다음은 11-08, 15, 22, 29
        listOf("2026-11-08", "2026-11-15", "2026-11-22", "2026-11-29").forEach {
            assertEquals(setOf(1L), days[d(it)]!!.dueIds)
        }
        assertNull(days[d("2026-11-04")])
    }

    @Test
    fun `안 하고 지난 예정은 그 지난 날짜에 남는다`() {
        val r = routine(1, firstDue = "2026-11-03")
        val days = buildCalendarDays(listOf(r to emptyList()), november)
        assertEquals(setOf(1L), days[d("2026-11-03")]!!.dueIds)
    }

    @Test
    fun `반복 없는 루틴은 한 번만, 하고 나면 완료만 남는다`() {
        val r = routine(1, firstDue = "2026-11-10", repeats = false)
        assertEquals(listOf(d("2026-11-10")), buildCalendarDays(listOf(r to emptyList()), november).keys.toList())

        val afterDone = buildCalendarDays(listOf(r to listOf(done(1, "2026-11-09"))), november)
        assertEquals(setOf(1L), afterDone[d("2026-11-09")]!!.doneIds)
        assertTrue(afterDone.values.all { it.dueIds.isEmpty() })
    }

    @Test
    fun `보관한 루틴은 예정도 완료도 보이지 않는다`() {
        val r = routine(1, firstDue = "2026-11-03", archived = true)
        assertTrue(buildCalendarDays(listOf(r to listOf(done(1, "2026-11-01"))), november).isEmpty())
    }

    @Test
    fun `범위 밖 완료 기록은 빠진다`() {
        val r = routine(1, firstDue = "2026-12-20")
        val days = buildCalendarDays(listOf(r to listOf(done(1, "2026-10-31"))), november)
        assertTrue(days.values.all { it.doneIds.isEmpty() })
    }

    // ── 날짜 목록 ──

    @Test
    fun `그날 목록은 예정과 완료를 합치고 이름 순`() {
        val a = routine(1, firstDue = "2026-11-05", title = "청소기")
        val b = routine(2, firstDue = "2026-10-01", title = "빨래")
        val c = routine(3, firstDue = "2026-11-20", title = "각질제거")
        val items = listOf(a to emptyList(), b to listOf(done(2, "2026-11-05")), c to emptyList())

        val entries = calendarEntriesOn(items, d("2026-11-05"))
        assertEquals(listOf("빨래", "청소기"), entries.map { it.routine.title })
        assertTrue(entries.single { it.routine.id == 2L }.done)
        assertFalse(entries.single { it.routine.id == 2L }.due)
        assertTrue(entries.single { it.routine.id == 1L }.due)
    }

    @Test
    fun `예정일에 한 루틴은 그 뒤 예정이 다음 주기로 밀려 그날은 완료만 남는다`() {
        val r = routine(1, firstDue = "2026-11-05")
        val entries = calendarEntriesOn(listOf(r to listOf(done(1, "2026-11-05"))), d("2026-11-05"))
        assertEquals(1, entries.size)
        assertTrue(entries.single().done)
        assertFalse(entries.single().due)
    }

    @Test
    fun `아무것도 없는 날은 빈 목록`() {
        assertTrue(calendarEntriesOn(listOf(routine(1, firstDue = "2026-11-05") to emptyList()), d("2026-11-06")).isEmpty())
    }
}
