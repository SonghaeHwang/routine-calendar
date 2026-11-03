package com.kikilog.routinecalendar.core.schedule

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Completion
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.model.IntervalUnit.DAY
import com.kikilog.routinecalendar.core.model.IntervalUnit.MONTH
import com.kikilog.routinecalendar.core.model.IntervalUnit.WEEK
import com.kikilog.routinecalendar.core.model.Routine
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ScheduleTest {

    private fun d(s: String) = LocalDate.parse(s)

    private fun routine(
        amount: Int,
        unit: IntervalUnit,
        firstDue: String,
        repeats: Boolean = true,
        id: Long = 1,
    ) = Routine(
        id = id,
        title = "루틴",
        categoryId = Category.ETC,
        interval = Interval(amount, unit),
        repeats = repeats,
        firstDue = d(firstDue),
        notify = true,
        archived = false,
    )

    private fun done(on: String, routineId: Long = 1) =
        Completion(id = 0, routineId = routineId, doneOn = d(on))

    // ── nextDue ──

    @Test
    fun `기록이 없으면 첫 예정일`() {
        assertEquals(d("2026-11-02"), nextDue(routine(10, DAY, "2026-11-02"), emptyList()))
    }

    @Test
    fun `일 주 개월 - 마지막으로 한 날 + 주기`() {
        assertEquals(d("2026-11-12"), nextDue(routine(10, DAY, "2026-11-01"), listOf(done("2026-11-02"))))
        assertEquals(d("2026-11-09"), nextDue(routine(1, WEEK, "2026-11-01"), listOf(done("2026-11-02"))))
        assertEquals(d("2027-01-02"), nextDue(routine(2, MONTH, "2026-11-01"), listOf(done("2026-11-02"))))
    }

    @Test
    fun `여러 기록 중 가장 늦은 날 기준 - 순서와 무관`() {
        val completions = listOf(done("2026-11-02"), done("2026-10-01"), done("2026-10-20"))
        assertEquals(d("2026-11-12"), nextDue(routine(10, DAY, "2026-09-01"), completions))
    }

    @Test
    fun `늦게 하면 다음 예정도 그날부터`() {
        // 예정 11/2, 실제로 11/4 에 함 → 11/14
        val r = routine(10, DAY, "2026-11-02")
        assertEquals(d("2026-11-14"), nextDue(r, listOf(done("2026-11-04"))))
    }

    @Test
    fun `일찍 하면 다음 예정도 그날부터`() {
        // 예정 11/12, 실제로 11/10 에 함 → 11/20
        val r = routine(10, DAY, "2026-11-12")
        assertEquals(d("2026-11-20"), nextDue(r, listOf(done("2026-11-10"))))
    }

    @Test
    fun `월말에 한 경우`() {
        assertEquals(d("2026-02-28"), nextDue(routine(1, MONTH, "2026-01-01"), listOf(done("2026-01-31"))))
        assertEquals(d("2028-02-29"), nextDue(routine(1, MONTH, "2028-01-01"), listOf(done("2028-01-31"))))
    }

    @Test
    fun `반복 없음 - 기록이 없으면 예정, 있으면 끝`() {
        val r = routine(2, MONTH, "2027-01-02", repeats = false)
        assertEquals(d("2027-01-02"), nextDue(r, emptyList()))
        assertNull(nextDue(r, listOf(done("2026-12-30"))))
    }

    @Test
    fun `체크를 취소하면 다시 계산된다`() {
        val r = routine(1, WEEK, "2026-10-01")
        val first = done("2026-10-20")
        val second = done("2026-10-27")
        assertEquals(d("2026-11-03"), nextDue(r, listOf(first, second)))
        // 마지막 체크 취소 → 이전 기록 기준
        assertEquals(d("2026-10-27"), nextDue(r, listOf(first)))
        // 모두 취소 → 첫 예정일
        assertEquals(d("2026-10-01"), nextDue(r, emptyList()))
    }

    @Test
    fun `반복 없음 - 체크 취소하면 다시 예정이 생긴다`() {
        val r = routine(2, MONTH, "2027-01-02", repeats = false)
        assertNull(nextDue(r, listOf(done("2027-01-02"))))
        assertEquals(d("2027-01-02"), nextDue(r, emptyList()))
    }

    @Test
    fun `다른 루틴의 기록은 무시`() {
        val r = routine(10, DAY, "2026-11-02", id = 1)
        assertEquals(d("2026-11-02"), nextDue(r, listOf(done("2026-11-05", routineId = 2))))
    }

    // ── lastDoneOn ──

    @Test
    fun `마지막으로 한 날 - 가장 늦은 기록, 없으면 null, 다른 루틴 무시`() {
        val r = routine(10, DAY, "2026-11-02")
        assertNull(lastDoneOn(r, emptyList()))
        assertEquals(
            d("2026-11-02"),
            lastDoneOn(r, listOf(done("2026-10-20"), done("2026-11-02"), done("2026-11-05", routineId = 2))),
        )
    }

    // ── occurrencesIn ──

    private fun range(from: String, to: String) = d(from)..d(to)

    @Test
    fun `주 단위 - 그 달 안의 예정일을 펼친다`() {
        val r = routine(1, WEEK, "2026-11-02")
        assertEquals(
            listOf(d("2026-11-02"), d("2026-11-09"), d("2026-11-16"), d("2026-11-23"), d("2026-11-30")),
            occurrencesIn(r, emptyList(), range("2026-11-01", "2026-11-30")),
        )
    }

    @Test
    fun `완료 기록 기준으로 펼친다`() {
        val r = routine(10, DAY, "2026-10-01")
        assertEquals(
            listOf(d("2026-11-07"), d("2026-11-17"), d("2026-11-27")),
            occurrencesIn(r, listOf(done("2026-10-28")), range("2026-11-01", "2026-11-30")),
        )
    }

    @Test
    fun `범위 시작보다 이른 예정일은 건너뛴다`() {
        val r = routine(1, WEEK, "2026-10-19")
        assertEquals(
            listOf(d("2026-11-02"), d("2026-11-09")),
            occurrencesIn(r, emptyList(), range("2026-11-01", "2026-11-10")),
        )
    }

    @Test
    fun `개월 단위로 펼쳐도 월말이 밀리지 않는다`() {
        val r = routine(1, MONTH, "2026-01-31")
        assertEquals(
            listOf(d("2026-01-31"), d("2026-02-28"), d("2026-03-31"), d("2026-04-30")),
            occurrencesIn(r, emptyList(), range("2026-01-01", "2026-04-30")),
        )
    }

    @Test
    fun `범위 끝 날도 포함`() {
        val r = routine(2, MONTH, "2026-11-30")
        assertEquals(listOf(d("2026-11-30")), occurrencesIn(r, emptyList(), range("2026-11-01", "2026-11-30")))
    }

    @Test
    fun `예정이 범위 뒤면 비어 있다`() {
        val r = routine(2, MONTH, "2027-01-02")
        assertEquals(emptyList<LocalDate>(), occurrencesIn(r, emptyList(), range("2026-11-01", "2026-11-30")))
    }

    @Test
    fun `반복 없음 - 한 번만`() {
        val r = routine(1, WEEK, "2026-11-09", repeats = false)
        assertEquals(listOf(d("2026-11-09")), occurrencesIn(r, emptyList(), range("2026-11-01", "2026-12-31")))
        assertEquals(emptyList<LocalDate>(), occurrencesIn(r, listOf(done("2026-11-09")), range("2026-11-01", "2026-12-31")))
    }

    // ── overdueDays ──

    @Test
    fun `지난 날수`() {
        assertEquals(3, overdueDays(due = d("2026-10-30"), today = d("2026-11-02")))
        assertEquals(0, overdueDays(due = d("2026-11-02"), today = d("2026-11-02")))
        assertEquals(0, overdueDays(due = d("2026-11-05"), today = d("2026-11-02")))
        // 윤년 2월 넘김
        assertEquals(2, overdueDays(due = d("2028-02-28"), today = d("2028-03-01")))
    }

    // ── initialSchedule ──

    @Test
    fun `등록 - 마지막으로 한 날을 넣으면 그날 + 주기, 기록도 남긴다`() {
        val s = initialSchedule(Interval(10, DAY), repeats = true, today = d("2026-11-02"), lastDoneOn = d("2026-10-28"))
        assertEquals(InitialSchedule(firstDue = d("2026-11-07"), initialDoneOn = d("2026-10-28")), s)
    }

    @Test
    fun `등록 - 비우면 오늘`() {
        val s = initialSchedule(Interval(10, DAY), repeats = true, today = d("2026-11-02"), lastDoneOn = null)
        assertEquals(InitialSchedule(firstDue = d("2026-11-02"), initialDoneOn = null), s)
    }

    @Test
    fun `등록 - 반복 없음, 비우면 오늘 + 주기`() {
        val s = initialSchedule(Interval(2, MONTH), repeats = false, today = d("2026-11-02"), lastDoneOn = null)
        assertEquals(InitialSchedule(firstDue = d("2027-01-02"), initialDoneOn = null), s)
    }

    @Test
    fun `등록 - 반복 없음, 마지막으로 한 날을 넣어도 기록은 남기지 않는다`() {
        val s = initialSchedule(Interval(2, MONTH), repeats = false, today = d("2026-11-02"), lastDoneOn = d("2026-10-15"))
        assertEquals(InitialSchedule(firstDue = d("2026-12-15"), initialDoneOn = null), s)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `등록 - 마지막으로 한 날이 미래면 거부`() {
        initialSchedule(Interval(1, WEEK), repeats = true, today = d("2026-11-02"), lastDoneOn = d("2026-11-03"))
    }

    // ── startSchedule (루틴 시작일) ──

    @Test
    fun `시작일 - 오늘이면 오늘이 첫 예정일, 기록 없음`() {
        val s = startSchedule(Interval(1, WEEK), repeats = true, today = d("2026-11-03"), start = d("2026-11-03"))
        assertEquals(InitialSchedule(firstDue = d("2026-11-03"), initialDoneOn = null), s)
    }

    @Test
    fun `시작일 - 앞날이면 그날이 첫 예정일, 반복 없음도 같다`() {
        assertEquals(
            InitialSchedule(firstDue = d("2026-11-10"), initialDoneOn = null),
            startSchedule(Interval(1, WEEK), repeats = true, today = d("2026-11-03"), start = d("2026-11-10")),
        )
        assertEquals(
            InitialSchedule(firstDue = d("2027-01-01"), initialDoneOn = null),
            startSchedule(Interval(2, MONTH), repeats = false, today = d("2026-11-03"), start = d("2027-01-01")),
        )
    }

    @Test
    fun `시작일 - 지난 날이면 그날 한 것으로 보고 주기를 더한다`() {
        val s = startSchedule(Interval(10, DAY), repeats = true, today = d("2026-11-03"), start = d("2026-10-28"))
        assertEquals(InitialSchedule(firstDue = d("2026-11-07"), initialDoneOn = d("2026-10-28")), s)
    }

    // ── nextDueDates (예정일 미리보기) ──

    @Test
    fun `미리보기 - 다음 예정일부터 3개, 월말이 밀리지 않는다`() {
        assertEquals(
            listOf(d("2027-01-31"), d("2027-02-28"), d("2027-03-31")),
            nextDueDates(routine(1, MONTH, "2027-01-31"), emptyList()),
        )
        assertEquals(
            listOf(d("2026-11-09"), d("2026-11-16"), d("2026-11-23")),
            nextDueDates(routine(1, WEEK, "2026-10-01"), listOf(done("2026-11-02"))),
        )
    }

    @Test
    fun `미리보기 - 반복 없음은 한 번, 끝났으면 없음`() {
        assertEquals(listOf(d("2026-12-01")), nextDueDates(routine(2, MONTH, "2026-12-01", repeats = false), emptyList()))
        assertEquals(
            emptyList<LocalDate>(),
            nextDueDates(routine(2, MONTH, "2026-12-01", repeats = false), listOf(done("2026-11-02"))),
        )
    }
}
