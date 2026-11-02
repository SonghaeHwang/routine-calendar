package com.kikilog.routinecalendar.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.Duration
import java.time.LocalDateTime

/**
 * 앱 전체가 함께 쓰는 "오늘". 화면은 [today] 를 구독하고, 날짜가 바뀌면 다시 계산된다.
 *
 * 자정 대기([runMidnightUpdates])만으로는 휴대폰이 깊이 잠든 동안 늦게 깨어날 수 있어,
 * 앱 화면이 다시 보일 때 [refresh] 도 부른다.
 */
class TodayClock(
    private val readToday: () -> LocalDate = { java.time.LocalDate.now().toKotlinLocalDate() },
) {
    private val _today = MutableStateFlow(readToday())
    val today: StateFlow<LocalDate> = _today.asStateFlow()

    /** 지금 날짜를 다시 읽어 흐름에 반영한다. */
    fun refresh() {
        _today.value = readToday()
    }

    /** 저장소용. 놓친 자정이 있어도 정확하도록 읽을 때마다 새로 확인한다. */
    fun current(): LocalDate {
        refresh()
        return _today.value
    }

    /** 자정마다 [refresh] 한다. 앱 범위 코루틴에서 한 번 실행한다. */
    suspend fun runMidnightUpdates(millisUntilNextDay: () -> Long = ::millisUntilNextDay) {
        while (true) {
            // 자정 직후에 읽도록 1초 여유
            delay(millisUntilNextDay() + 1_000)
            refresh()
        }
    }
}

private fun millisUntilNextDay(): Long {
    val now = LocalDateTime.now()
    val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
    return Duration.between(now, nextMidnight).toMillis()
}
