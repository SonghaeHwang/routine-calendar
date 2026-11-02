package com.kikilog.routinecalendar.ui.format

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DateFormatsTest {

    private fun d(s: String) = LocalDate.parse(s)
    private val today = d("2026-11-02")

    @Test
    fun `요일 이름`() {
        assertEquals("월", dayOfWeekShort(DayOfWeek.MONDAY))
        assertEquals("토", dayOfWeekShort(DayOfWeek.SATURDAY))
        assertEquals("일", dayOfWeekShort(DayOfWeek.SUNDAY))
    }

    @Test
    fun `같은 해는 월 일 요일`() {
        assertEquals("11월 2일 (월)", formatDate(d("2026-11-02"), today))
        assertEquals("1월 31일 (토)", formatDate(d("2026-01-31"), today))
    }

    @Test
    fun `다른 해는 연도를 붙인다`() {
        assertEquals("2027년 1월 2일 (토)", formatDate(d("2027-01-02"), today))
        assertEquals("2028년 2월 29일 (화)", formatDate(d("2028-02-29"), today))
    }

    @Test
    fun `짧은 날짜`() {
        assertEquals("11/6 (금)", formatShortDate(d("2026-11-06")))
        assertEquals("12/31 (목)", formatShortDate(d("2026-12-31")))
    }
}
