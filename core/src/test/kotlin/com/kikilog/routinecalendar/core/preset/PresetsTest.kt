package com.kikilog.routinecalendar.core.preset

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PresetsTest {

    private fun named(title: String) = DEFAULT_PRESETS.single { it.title == title }

    @Test
    fun `PLAN 표와 같은 개수 - 집안일 7 · 뷰티 4 · 구매 3`() {
        val counts = DEFAULT_PRESETS.groupingBy { it.category }.eachCount()
        assertEquals(mapOf(Category.HOUSE to 7, Category.BEAUTY to 4, Category.SUPPLY to 3), counts)
    }

    @Test
    fun `이름이 겹치지 않는다`() {
        assertEquals(DEFAULT_PRESETS.size, DEFAULT_PRESETS.map { it.title }.toSet().size)
    }

    @Test
    fun `분류 순서대로 놓인다`() {
        val order = DEFAULT_PRESETS.map { it.category }.distinct()
        assertEquals(listOf(Category.HOUSE, Category.BEAUTY, Category.SUPPLY), order)
        assertEquals(DEFAULT_PRESETS.map { it.category }, DEFAULT_PRESETS.map { it.category }.sortedBy { order.indexOf(it) })
    }

    @Test
    fun `대표 프리셋의 주기`() {
        assertEquals(Interval(2, IntervalUnit.MONTH), named("이불 세탁").interval)
        assertEquals(Interval(1, IntervalUnit.WEEK), named("모델링팩").interval)
        assertEquals(Interval(10, IntervalUnit.DAY), named("각질제거").interval)
        assertEquals(Interval(2, IntervalUnit.WEEK), named("화장실 청소").interval)
        assertEquals(Interval(3, IntervalUnit.MONTH), named("칫솔 교체").interval)
    }

    @Test
    fun `리들샷만 주기를 직접 입력한다`() {
        assertNull(named("리들샷").interval)
        assertEquals(listOf("리들샷"), DEFAULT_PRESETS.filter { it.interval == null }.map { it.title })
    }

    @Test
    fun `기본값은 모두 반복`() {
        assertTrue(DEFAULT_PRESETS.all { it.repeats })
    }
}
