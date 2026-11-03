package com.kikilog.routinecalendar.core.model

import kotlinx.datetime.LocalDate

/**
 * 등록한 루틴. 다음 예정일은 저장하지 않고 완료 기록으로 매번 계산한다 ([com.kikilog.routinecalendar.core.schedule]).
 *
 * @property repeats false 면 한 번만 한다("화장지 2달 후"). 완료 기록이 생기면 끝난다.
 * @property categoryId [Category.id]
 * @property firstDue 완료 기록이 없을 때의 예정일
 */
data class Routine(
    val id: Long,
    val title: String,
    val categoryId: Long,
    val interval: Interval,
    val repeats: Boolean,
    val firstDue: LocalDate,
    val notify: Boolean,
    val archived: Boolean,
)

/** 실제로 한 날의 기록. 한 루틴에 같은 날은 하나만 있다. */
data class Completion(
    val id: Long,
    val routineId: Long,
    val doneOn: LocalDate,
)
