package com.kikilog.routinecalendar.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelTabTest {

    @Test
    fun `오늘 탭은 오늘 하나만`() {
        assertEquals(listOf(TodayKey), backStackFor(TopLevelTab.TODAY))
    }

    @Test
    fun `다른 탭은 오늘 위에 쌓아서 뒤로 가면 오늘로 온다`() {
        assertEquals(listOf(TodayKey, CalendarKey), backStackFor(TopLevelTab.CALENDAR))
        assertEquals(listOf(TodayKey, RoutinesKey), backStackFor(TopLevelTab.ROUTINES))
    }

    @Test
    fun `탭은 오늘 · 캘린더 · 루틴 순서`() {
        assertEquals(listOf(TodayKey, CalendarKey, RoutinesKey), TopLevelTab.entries.map { it.key })
    }
}
