package com.kikilog.routinecalendar.core.schedule

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import org.junit.Assert.assertEquals
import org.junit.Test

class WeekTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun `기본 주 시작은 일요일`() {
        assertEquals(DayOfWeek.SUNDAY, DEFAULT_WEEK_START)
    }

    @Test
    fun `일요일 시작 - 일요일부터 토요일까지 모두 그 주 토요일`() {
        val sunday = d("2026-11-01")
        for (i in 0..6) {
            assertEquals(d("2026-11-07"), weekEndOf(sunday.plus(DatePeriod(days = i)), DayOfWeek.SUNDAY))
        }
        // 다음 일요일부터는 다음 주
        assertEquals(d("2026-11-14"), weekEndOf(d("2026-11-08"), DayOfWeek.SUNDAY))
    }

    @Test
    fun `월요일 시작 - 월요일부터 일요일까지 모두 그 주 일요일`() {
        val monday = d("2026-11-02")
        for (i in 0..6) {
            assertEquals(d("2026-11-08"), weekEndOf(monday.plus(DatePeriod(days = i)), DayOfWeek.MONDAY))
        }
        assertEquals(d("2026-11-15"), weekEndOf(d("2026-11-09"), DayOfWeek.MONDAY))
    }

    @Test
    fun `해와 달을 넘는 주`() {
        // 2026-12-31 은 목요일
        assertEquals(d("2027-01-02"), weekEndOf(d("2026-12-31"), DayOfWeek.SUNDAY))
        assertEquals(d("2027-01-03"), weekEndOf(d("2026-12-31"), DayOfWeek.MONDAY))
    }
}
