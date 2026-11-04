package com.kikilog.routinecalendar.core.schedule

import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Routine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** 그 달의 1일 */
fun firstOfMonth(date: LocalDate): LocalDate = LocalDate(date.year, date.month, 1)

/** 한 달 앞뒤로 옮긴 달의 1일 */
fun shiftMonth(firstOfMonth: LocalDate, months: Int): LocalDate = firstOfMonth.plus(DatePeriod(months = months))

/**
 * 월 달력에 그릴 날짜들. 1일이 들어간 주의 첫날부터 마지막 날이 들어간 주의 끝날까지(4~6주, 7의 배수).
 * 앞뒤 달 날짜도 포함한다.
 */
fun monthGridDates(firstOfMonth: LocalDate, weekStart: DayOfWeek): List<LocalDate> {
    val lastOfMonth = firstOfMonth.plus(DatePeriod(months = 1)).minus(DatePeriod(days = 1))
    val leading = (firstOfMonth.dayOfWeek.isoDayNumber - weekStart.isoDayNumber + 7) % 7
    val start = firstOfMonth.minus(DatePeriod(days = leading))
    val end = weekEndOf(lastOfMonth, weekStart)
    return generateSequence(start) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= end }.toList()
}

/** 요일 머리글 순서. 주 시작 요일부터 7개 */
fun weekDays(weekStart: DayOfWeek): List<DayOfWeek> =
    (0 until 7).map { DayOfWeek((weekStart.isoDayNumber - 1 + it) % 7 + 1) }

/**
 * 달력 한 칸의 표시.
 * @property dueIds 그날 할 예정인 루틴(●). 다음 예정일부터 주기씩 펼친 추정치이고, 지난 예정이면 아직 안 한 것
 * @property doneIds 그날 실제로 한 루틴(✓)
 */
data class CalendarDay(val dueIds: Set<Long>, val doneIds: Set<Long>) {
    val isEmpty: Boolean get() = dueIds.isEmpty() && doneIds.isEmpty()

    /** 그날 한 것이 있고 남은 예정이 없는 날. 달력에 발바닥 도장을 찍는다 */
    val allDone: Boolean get() = doneIds.isNotEmpty() && doneIds.containsAll(dueIds)
}

/**
 * [range] 안의 날짜별 예정 · 완료. 보관한 루틴은 뺀다. 표시할 것이 없는 날은 맵에 없다.
 * 예정은 [occurrencesIn] 으로 다음 예정일부터 펼치므로, 지난 날짜에는 아직 안 한(지난) 예정만 남는다.
 */
fun buildCalendarDays(
    items: List<Pair<Routine, List<Completion>>>,
    range: ClosedRange<LocalDate>,
): Map<LocalDate, CalendarDay> {
    val due = mutableMapOf<LocalDate, MutableSet<Long>>()
    val done = mutableMapOf<LocalDate, MutableSet<Long>>()
    for ((routine, completions) in items) {
        if (routine.archived) continue
        val own = completions.filter { it.routineId == routine.id }
        occurrencesIn(routine, own, range).forEach { due.getOrPut(it) { mutableSetOf() } += routine.id }
        own.filter { it.doneOn in range }.forEach { done.getOrPut(it.doneOn) { mutableSetOf() } += routine.id }
    }
    return (due.keys + done.keys).associateWith { date ->
        CalendarDay(dueIds = due[date].orEmpty(), doneIds = done[date].orEmpty())
    }
}

/** 날짜를 눌렀을 때 아래 목록의 한 줄. 예정이면서 그날 했으면 둘 다 true */
data class CalendarEntry(val routine: Routine, val due: Boolean, val done: Boolean)

/** 그날 예정이거나 그날 한 루틴. 이름 · id 순 */
fun calendarEntriesOn(items: List<Pair<Routine, List<Completion>>>, date: LocalDate): List<CalendarEntry> {
    val day = buildCalendarDays(items, date..date)[date] ?: return emptyList()
    return items
        .map { it.first }
        .filter { it.id in day.dueIds || it.id in day.doneIds }
        .map { CalendarEntry(it, due = it.id in day.dueIds, done = it.id in day.doneIds) }
        .sortedWith(compareBy<CalendarEntry> { it.routine.title }.thenBy { it.routine.id })
}
