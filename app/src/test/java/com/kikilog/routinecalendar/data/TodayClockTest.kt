package com.kikilog.routinecalendar.data

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

/** Android 가 필요 없는 일반 JUnit 시험. 가상 시간으로 자정을 흉내 낸다. */
@OptIn(ExperimentalCoroutinesApi::class)
class TodayClockTest {

    private var now = LocalDate.parse("2026-11-02")
    private val clock = TodayClock(readToday = { now })

    @Test
    fun `처음 값은 만들 때의 오늘`() {
        assertEquals(LocalDate.parse("2026-11-02"), clock.today.value)
    }

    @Test
    fun `refresh 하면 바뀐 날짜가 흐름에 반영된다`() {
        now = LocalDate.parse("2026-11-03")
        assertEquals(LocalDate.parse("2026-11-02"), clock.today.value)

        clock.refresh()
        assertEquals(LocalDate.parse("2026-11-03"), clock.today.value)
    }

    @Test
    fun `current 는 놓친 자정이 있어도 새 날짜를 돌려주고 흐름도 맞춘다`() {
        now = LocalDate.parse("2026-11-03")
        assertEquals(LocalDate.parse("2026-11-03"), clock.current())
        assertEquals(LocalDate.parse("2026-11-03"), clock.today.value)
    }

    @Test
    fun `자정이 지나면 저절로 다음 날이 된다`() = runTest {
        val job = launch { clock.runMidnightUpdates(millisUntilNextDay = { 60_000 }) }
        runCurrent()

        now = LocalDate.parse("2026-11-03")
        advanceTimeBy(60_000) // 아직 1초 여유 전
        assertEquals(LocalDate.parse("2026-11-02"), clock.today.value)

        advanceTimeBy(1_001)
        assertEquals(LocalDate.parse("2026-11-03"), clock.today.value)

        job.cancel()
    }
}
