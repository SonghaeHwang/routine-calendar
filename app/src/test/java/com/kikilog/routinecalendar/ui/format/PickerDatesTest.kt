package com.kikilog.routinecalendar.ui.format

import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class PickerDatesTest {

    @Test
    fun `날짜를 UTC 자정 밀리초로`() {
        assertEquals(0L, LocalDate.parse("1970-01-01").toPickerMillis())
        // 2026-11-02 00:00 UTC
        assertEquals(1_793_577_600_000L, LocalDate.parse("2026-11-02").toPickerMillis())
    }

    @Test
    fun `밀리초를 날짜로 - 같은 날 안의 시각은 모두 그날`() {
        val midnight = LocalDate.parse("2026-11-02").toPickerMillis()
        assertEquals(LocalDate.parse("2026-11-02"), pickerMillisToDate(midnight))
        assertEquals(LocalDate.parse("2026-11-02"), pickerMillisToDate(midnight + 86_399_999))
        assertEquals(LocalDate.parse("2026-11-03"), pickerMillisToDate(midnight + 86_400_000))
    }

    @Test
    fun `등록 시각은 기기 시간대의 날짜로 - 서울 자정 직후는 UTC 전날`() {
        // 2026-11-02 00:30 (서울, UTC+9) = 2026-11-01 15:30 UTC
        val millis = java.time.ZonedDateTime.of(2026, 11, 2, 0, 30, 0, 0, java.time.ZoneId.of("Asia/Seoul")).toInstant().toEpochMilli()
        assertEquals(LocalDate.parse("2026-11-02"), epochMillisToLocalDate(millis, java.time.ZoneId.of("Asia/Seoul")))
        assertEquals(LocalDate.parse("2026-11-01"), epochMillisToLocalDate(millis, java.time.ZoneOffset.UTC))
    }

    @Test
    fun `왕복해도 같은 날 - 월말 · 윤일 · 1970 이전`() {
        for (s in listOf("2026-01-31", "2028-02-29", "2026-12-31", "1969-12-31")) {
            val date = LocalDate.parse(s)
            assertEquals(date, pickerMillisToDate(date.toPickerMillis()))
        }
    }
}
