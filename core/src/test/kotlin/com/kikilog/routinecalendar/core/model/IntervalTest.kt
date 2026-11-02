package com.kikilog.routinecalendar.core.model

import com.kikilog.routinecalendar.core.model.IntervalUnit.DAY
import com.kikilog.routinecalendar.core.model.IntervalUnit.MONTH
import com.kikilog.routinecalendar.core.model.IntervalUnit.WEEK
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class IntervalTest {

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun `일 단위`() {
        assertEquals(d("2026-11-12"), Interval(10, DAY).addTo(d("2026-11-02")))
        assertEquals(d("2027-01-01"), Interval(1, DAY).addTo(d("2026-12-31")))
    }

    @Test
    fun `주 단위`() {
        assertEquals(d("2026-11-09"), Interval(1, WEEK).addTo(d("2026-11-02")))
        assertEquals(d("2026-11-16"), Interval(2, WEEK).addTo(d("2026-11-02")))
    }

    @Test
    fun `개월 단위`() {
        assertEquals(d("2027-01-02"), Interval(2, MONTH).addTo(d("2026-11-02")))
    }

    @Test
    fun `월말에 개월을 더하면 그 달 마지막 날`() {
        assertEquals(d("2026-02-28"), Interval(1, MONTH).addTo(d("2026-01-31")))
        assertEquals(d("2026-04-30"), Interval(1, MONTH).addTo(d("2026-03-31")))
    }

    @Test
    fun `윤년`() {
        assertEquals(d("2028-02-29"), Interval(1, MONTH).addTo(d("2028-01-31")))
        assertEquals(d("2028-03-01"), Interval(1, DAY).addTo(d("2028-02-29")))
        assertEquals(d("2028-02-29"), Interval(1, DAY).addTo(d("2028-02-28")))
        // 윤일 + 12개월 → 평년 2월 28일
        assertEquals(d("2029-02-28"), Interval(12, MONTH).addTo(d("2028-02-29")))
    }

    @Test
    fun `배수만큼 더한다`() {
        assertEquals(d("2026-03-31"), Interval(1, MONTH).addTo(d("2026-01-31"), times = 2))
        assertEquals(d("2026-11-02"), Interval(3, DAY).addTo(d("2026-11-02"), times = 0))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `주기는 1 이상`() {
        Interval(0, DAY)
    }
}
