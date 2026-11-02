package com.kikilog.routinecalendar.core.schedule

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus

/** 주 시작 요일 기본값. 설정에서 바꿀 수 있게 되면(4단계) 이 값은 처음 값으로만 쓴다. */
val DEFAULT_WEEK_START: DayOfWeek = DayOfWeek.SUNDAY

/** [today] 가 속한 주의 마지막 날. 일요일 시작이면 토요일, 월요일 시작이면 일요일. */
fun weekEndOf(today: LocalDate, weekStart: DayOfWeek): LocalDate {
    val daysSinceStart = (today.dayOfWeek.isoDayNumber - weekStart.isoDayNumber + 7) % 7
    return today.plus(DatePeriod(days = 6 - daysSinceStart))
}
