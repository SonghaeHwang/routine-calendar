package com.kikilog.routinecalendar.core.notify

import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Routine
import com.kikilog.routinecalendar.core.schedule.buildTodayBoard
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

/** 하루 1회 요약 알림 시각(기기 시간대 기준 벽시계 시각). 설정에서 30분 단위로 고른다. */
data class NotifyTime(val hour: Int, val minute: Int) {
    init {
        require(hour in 0..23 && minute in 0..59) { "알림 시각이 범위를 벗어남: $hour:$minute" }
    }

    val localTime: LocalTime get() = LocalTime(hour, minute)

    /** 저장용 "HH:mm" */
    fun encode(): String = "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

    companion object {
        /** "HH:mm" 을 읽는다. 형식이 틀리면 null */
        fun decode(text: String): NotifyTime? {
            val parts = text.split(':')
            if (parts.size != 2) return null
            val hour = parts[0].toIntOrNull() ?: return null
            val minute = parts[1].toIntOrNull() ?: return null
            if (hour !in 0..23 || minute !in 0..59) return null
            return NotifyTime(hour, minute)
        }
    }
}

/** 처음 알림을 켰을 때의 시각 */
val DEFAULT_NOTIFY_TIME = NotifyTime(8, 0)

/** 설정에서 고를 수 있는 시각: 00:00 ~ 23:30, 30분 간격 48개 */
val NOTIFY_TIME_OPTIONS: List<NotifyTime> = (0 until 48).map { NotifyTime(it / 2, (it % 2) * 30) }

/**
 * [now] 다음에 오는 알림 시각. 오늘 그 시각이 아직이면 오늘, 이미 지났거나 바로 그 시각이면 내일.
 * 시간대 · 서머타임은 다루지 않는다(벽시계 기준). 실제 시점으로 바꾸는 일은 기기 시간대를 아는 쪽(:app)이 한다.
 */
fun nextNotifyAt(now: LocalDateTime, time: NotifyTime): LocalDateTime {
    val todayAt = LocalDateTime(now.date, time.localTime)
    return if (now < todayAt) todayAt else LocalDateTime(now.date.plus(1, DateTimeUnit.DAY), time.localTime)
}

/** 요약 알림에 넣을 루틴. 순서는 오늘 화면과 같다. */
data class DailySummary(
    val overdue: List<Routine>,
    val dueToday: List<Routine>,
) {
    val isEmpty: Boolean get() = overdue.isEmpty() && dueToday.isEmpty()
}

/**
 * 오늘 화면의 "지난 것 · 오늘" 칸에서 알림에 넣을 것만 고른다.
 * - "알림에 넣기" 를 끈 루틴과 보관한 루틴은 뺀다
 * - 오늘 이미 체크한 루틴은 뺀다
 * - "다가오는 7일" 은 넣지 않는다
 */
fun buildDailySummary(
    items: List<Pair<Routine, List<Completion>>>,
    today: LocalDate,
): DailySummary {
    val board = buildTodayBoard(items.filter { it.first.notify }, today)
    return DailySummary(
        overdue = board.overdue.map { it.routine },
        dueToday = board.dueToday.filterNot { it.doneToday }.map { it.routine },
    )
}
