package com.kikilog.routinecalendar.data

import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Routine

/**
 * 리포지토리가 내보내는 루틴 하나와 그 완료 기록. [completions] 는 doneOn 오름차순.
 * @property createdAt 등록한 시각(epoch millis). 계산에는 쓰지 않고 상세(수정) 화면의 등록일로만 보인다
 */
data class RoutineWithCompletions(
    val routine: Routine,
    val completions: List<Completion>,
    val createdAt: Long = 0,
)
