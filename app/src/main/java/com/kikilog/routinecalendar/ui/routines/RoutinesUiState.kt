package com.kikilog.routinecalendar.ui.routines

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.schedule.lastDoneOn
import com.kikilog.routinecalendar.core.schedule.nextDue
import com.kikilog.routinecalendar.data.RoutineWithCompletions
import kotlinx.datetime.LocalDate

/** 루틴 목록의 한 줄 */
data class RoutineRow(
    val id: Long,
    val title: String,
    val categoryId: Long,
    val interval: Interval,
    val repeats: Boolean,
    val lastDoneOn: LocalDate?,
    /** null 이면 끝남(반복 없음 + 완료) */
    val nextDue: LocalDate?,
    val archived: Boolean,
)

data class RoutinesUiState(
    val isLoading: Boolean = true,
    val today: LocalDate? = null,
    val rows: List<RoutineRow> = emptyList(),
    val showArchived: Boolean = false,
    /** 줄의 분류 이름 · 색 */
    val categories: List<Category> = emptyList(),
    /** 선택 모드에서 고른 루틴. 비어 있으면 선택 모드가 아니다 */
    val selectedIds: Set<Long> = emptySet(),
) {
    val isSelecting: Boolean get() = selectedIds.isNotEmpty()
}

/** 다음 예정일 순, 끝난 것은 맨 아래. 같으면 이름 · id 순. */
fun buildRoutineRows(items: List<RoutineWithCompletions>): List<RoutineRow> =
    items
        .map { (routine, completions) ->
            RoutineRow(
                id = routine.id,
                title = routine.title,
                categoryId = routine.categoryId,
                interval = routine.interval,
                repeats = routine.repeats,
                lastDoneOn = lastDoneOn(routine, completions),
                nextDue = nextDue(routine, completions),
                archived = routine.archived,
            )
        }
        .sortedWith(
            compareBy<RoutineRow> { it.nextDue == null }
                .thenBy { it.nextDue }
                .thenBy { it.title }
                .thenBy { it.id },
        )
