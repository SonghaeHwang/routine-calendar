package com.kikilog.routinecalendar.data

import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.Routine
import kotlinx.datetime.LocalDate

fun RoutineEntity.toModel(): Routine = Routine(
    id = id,
    title = title,
    category = category,
    interval = Interval(intervalAmount, intervalUnit),
    repeats = repeats,
    firstDue = firstDue,
    notify = notify,
    archived = archived,
)

fun CompletionEntity.toModel(): Completion = Completion(
    id = id,
    routineId = routineId,
    doneOn = doneOn,
)

/** @Relation 은 순서를 보장하지 않으므로 여기서 doneOn 순으로 정렬한다. */
fun RoutineWithCompletionsRow.toModel(): RoutineWithCompletions = RoutineWithCompletions(
    routine = routine.toModel(),
    completions = completions.map { it.toModel() }.sortedBy { it.doneOn },
    createdAt = routine.createdAt,
)

fun RoutineDraft.toEntity(firstDue: LocalDate, createdAt: Long): RoutineEntity = RoutineEntity(
    title = title,
    category = category,
    intervalAmount = interval.amount,
    intervalUnit = interval.unit,
    repeats = repeats,
    firstDue = firstDue,
    notify = notify,
    archived = false,
    createdAt = createdAt,
)

fun RoutineDraft.toEditColumns(id: Long): RoutineEditColumns = RoutineEditColumns(
    id = id,
    title = title,
    category = category,
    intervalAmount = interval.amount,
    intervalUnit = interval.unit,
    repeats = repeats,
    notify = notify,
)
