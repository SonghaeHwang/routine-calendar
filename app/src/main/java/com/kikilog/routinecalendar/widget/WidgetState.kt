package com.kikilog.routinecalendar.widget

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.schedule.buildTodayBoard
import com.kikilog.routinecalendar.core.schedule.nextDue
import com.kikilog.routinecalendar.data.RoutineWithCompletions
import kotlinx.datetime.LocalDate

/**
 * 위젯 한 줄.
 * @property colorArgb 분류 색. 분류가 지워져 찾지 못하면 null
 * @property overdueDays 지난 날수. 오늘 할 것이면 0
 */
data class WidgetRow(
    val routineId: Long,
    val title: String,
    val colorArgb: Int?,
    val overdueDays: Int,
    val doneToday: Boolean,
)

/** 오늘 할 것이 없을 때 보여 줄 가장 가까운 다음 예정 */
data class WidgetNext(val title: String, val due: LocalDate)

/**
 * 위젯이 그리는 내용. 줄 나누기는 오늘 화면과 같은 [buildTodayBoard] 를 쓰고, 위젯에는 지난 것 + 오늘만 보인다.
 * 오늘 체크한 줄은 앱처럼 체크된 채로 남는다.
 */
data class WidgetState(
    val today: LocalDate,
    val rows: List<WidgetRow>,
    val next: WidgetNext?,
    val hasAnyRoutine: Boolean,
)

fun buildWidgetState(
    items: List<RoutineWithCompletions>,
    categories: List<Category>,
    today: LocalDate,
): WidgetState {
    val board = buildTodayBoard(items.map { it.routine to it.completions }, today)
    val colors = categories.associate { it.id to it.colorArgb }
    val rows = (board.overdue + board.dueToday).map { entry ->
        WidgetRow(
            routineId = entry.routine.id,
            title = entry.routine.title,
            colorArgb = colors[entry.routine.categoryId],
            overdueDays = entry.overdueDays,
            doneToday = entry.doneToday,
        )
    }
    val next = if (rows.isNotEmpty()) {
        null
    } else {
        items.asSequence()
            .filterNot { it.routine.archived }
            .mapNotNull { item -> nextDue(item.routine, item.completions)?.let { WidgetNext(item.routine.title, it) } }
            .filter { it.due > today }
            .minWithOrNull(compareBy<WidgetNext> { it.due }.thenBy { it.title })
    }
    return WidgetState(today, rows, next, hasAnyRoutine = items.any { !it.routine.archived })
}
