package com.kikilog.routinecalendar.data

import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Routine

/** 리포지토리가 내보내는 루틴 하나와 그 완료 기록. [completions] 는 doneOn 오름차순. */
data class RoutineWithCompletions(
    val routine: Routine,
    val completions: List<Completion>,
)
