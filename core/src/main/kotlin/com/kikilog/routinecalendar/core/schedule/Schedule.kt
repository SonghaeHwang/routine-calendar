package com.kikilog.routinecalendar.core.schedule

import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.Routine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

/**
 * 다음 예정일. 완료 기록이 있으면 마지막으로 한 날 + 주기(완료일 기준), 없으면 [Routine.firstDue].
 * 반복하지 않는 루틴에 완료 기록이 있으면 끝난 것이므로 null.
 *
 * [completions] 에 다른 루틴의 기록이 섞여 있어도 이 루틴 것만 본다.
 */
fun nextDue(routine: Routine, completions: Collection<Completion>): LocalDate? {
    val lastDone = lastDoneOn(routine, completions) ?: return routine.firstDue
    return if (routine.repeats) routine.interval.addTo(lastDone) else null
}

/** 마지막으로 한 날. 기록이 없으면 null. 다른 루틴의 기록은 무시한다. */
fun lastDoneOn(routine: Routine, completions: Collection<Completion>): LocalDate? =
    completions
        .filter { it.routineId == routine.id }
        .maxOfOrNull { it.doneOn }

/**
 * [range] 안에 들어오는 예정일(추정치). 다음 예정일부터 주기씩 더해 펼친다.
 * 매번 다음 예정일에 `주기 × k` 를 더하므로 월말이 밀리지 않는다(1/31 → 2/28 → 3/31).
 * 완료 기록 자체는 포함하지 않는다.
 */
fun occurrencesIn(
    routine: Routine,
    completions: Collection<Completion>,
    range: ClosedRange<LocalDate>,
): List<LocalDate> {
    val start = nextDue(routine, completions) ?: return emptyList()
    if (!routine.repeats) return if (start in range) listOf(start) else emptyList()

    return generateSequence(0) { it + 1 }
        .map { k -> routine.interval.addTo(start, k) }
        .takeWhile { it <= range.endInclusive }
        .filter { it >= range.start }
        .toList()
}

/** 예정일이 지난 날수. 오늘이거나 아직 안 왔으면 0. */
fun overdueDays(due: LocalDate, today: LocalDate): Int =
    due.daysUntil(today).coerceAtLeast(0)

/**
 * 등록할 때 정하는 첫 예정일과, 함께 저장할 완료 기록.
 * @property initialDoneOn null 이 아니면 이 날짜로 완료 기록을 하나 저장한다
 */
data class InitialSchedule(val firstDue: LocalDate, val initialDoneOn: LocalDate?)

/**
 * "마지막으로 한 날" 을 넣었으면 첫 예정일은 그날 + 주기.
 * 반복하는 루틴이면 그날을 완료 기록으로 남기고, 반복하지 않으면 남기지 않는다(남기면 바로 끝나 버린다).
 * 비웠으면 반복하는 루틴은 오늘, 반복하지 않으면 오늘 + 주기.
 */
fun initialSchedule(
    interval: Interval,
    repeats: Boolean,
    today: LocalDate,
    lastDoneOn: LocalDate?,
): InitialSchedule {
    if (lastDoneOn != null) {
        require(lastDoneOn <= today) { "마지막으로 한 날이 오늘보다 늦습니다: $lastDoneOn" }
        return InitialSchedule(
            firstDue = interval.addTo(lastDoneOn),
            initialDoneOn = lastDoneOn.takeIf { repeats },
        )
    }
    val firstDue = if (repeats) today else interval.addTo(today)
    return InitialSchedule(firstDue = firstDue, initialDoneOn = null)
}

/**
 * 등록 화면의 "루틴 시작일".
 * - 오늘이거나 앞날이면 그날이 첫 예정일(반복 여부와 상관없이). 완료 기록은 남기지 않는다
 * - 지난 날이면 그날 한 것으로 보고 [initialSchedule] 의 "마지막으로 한 날" 과 같게 계산한다
 */
fun startSchedule(
    interval: Interval,
    repeats: Boolean,
    today: LocalDate,
    start: LocalDate,
): InitialSchedule =
    if (start >= today) {
        InitialSchedule(firstDue = start, initialDoneOn = null)
    } else {
        initialSchedule(interval, repeats, today, lastDoneOn = start)
    }

/**
 * 다음 예정일부터 [count] 개(루틴 정보 화면의 미리보기). 반복하지 않으면 많아야 1개, 끝났으면 빈 목록.
 * [occurrencesIn] 과 같이 다음 예정일에 `주기 × k` 를 더해 월말이 밀리지 않는다.
 */
fun nextDueDates(routine: Routine, completions: Collection<Completion>, count: Int = 3): List<LocalDate> {
    val start = nextDue(routine, completions) ?: return emptyList()
    if (!routine.repeats) return listOf(start)
    return (0 until count).map { k -> routine.interval.addTo(start, k) }
}
