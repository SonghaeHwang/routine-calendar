package com.kikilog.routinecalendar.core.schedule

import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Routine
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** 오늘 화면 아래 칸에 보여 줄 날수. 요일과 상관없이 늘 앞으로 이만큼 보인다. */
const val UPCOMING_DAYS = 7

/**
 * 오늘 화면의 한 줄.
 * @property due 이 줄의 예정일. 오늘 체크한 줄은 오늘
 * @property overdueDays 지난 날수. 지난 것 칸이 아니면 0
 */
data class TodayEntry(
    val routine: Routine,
    val due: LocalDate,
    val overdueDays: Int,
    val doneToday: Boolean,
)

/** 오늘 화면의 세 칸. 루틴 하나는 한 칸에만 나온다. */
data class TodayBoard(
    val overdue: List<TodayEntry>,
    val dueToday: List<TodayEntry>,
    val upcoming: List<TodayEntry>,
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && dueToday.isEmpty() && upcoming.isEmpty()

    companion object {
        val EMPTY = TodayBoard(emptyList(), emptyList(), emptyList())
    }
}

/**
 * 루틴을 "지난 것 · 오늘 · 다가오는 7일" 로 나눈다. 위에서부터 차례로 적용한다.
 * 1. 보관한 루틴은 뺀다
 * 2. 오늘 완료 기록이 있으면 "오늘" 칸에 체크된 채로 둔다 — 체크하자마자 줄이 사라지지 않게
 * 3. 다음 예정일이 없으면(반복 없음 + 완료) 뺀다
 * 4. 예정일 < 오늘 → 지난 것, = 오늘 → 오늘, ≤ 오늘 + [UPCOMING_DAYS] → 다가오는 7일. 그 뒤는 뺀다
 *
 * 처음에는 "이번 주(일~토)" 였지만, 주 후반에 등록한 1주 루틴이 화면에 안 보여 등록이 안 된 것처럼 보여서 바꿨다.
 */
fun buildTodayBoard(
    items: List<Pair<Routine, List<Completion>>>,
    today: LocalDate,
): TodayBoard {
    val upcomingEnd = today.plus(DatePeriod(days = UPCOMING_DAYS))
    val overdue = mutableListOf<TodayEntry>()
    val dueToday = mutableListOf<TodayEntry>()
    val upcoming = mutableListOf<TodayEntry>()

    for ((routine, completions) in items) {
        if (routine.archived) continue
        val own = completions.filter { it.routineId == routine.id }
        if (own.any { it.doneOn == today }) {
            dueToday += TodayEntry(routine, due = today, overdueDays = 0, doneToday = true)
            continue
        }
        val due = nextDue(routine, own) ?: continue
        when {
            due < today -> overdue += TodayEntry(routine, due, overdueDays(due, today), doneToday = false)
            due == today -> dueToday += TodayEntry(routine, due, overdueDays = 0, doneToday = false)
            due <= upcomingEnd -> upcoming += TodayEntry(routine, due, overdueDays = 0, doneToday = false)
        }
    }

    return TodayBoard(
        overdue = overdue.sortedWith(
            compareByDescending<TodayEntry> { it.overdueDays }.thenBy { it.routine.title }.thenBy { it.routine.id },
        ),
        dueToday = dueToday.sortedWith(
            compareBy<TodayEntry> { it.doneToday }.thenBy { it.routine.title }.thenBy { it.routine.id },
        ),
        upcoming = upcoming.sortedWith(
            compareBy<TodayEntry> { it.due }.thenBy { it.routine.title }.thenBy { it.routine.id },
        ),
    )
}
