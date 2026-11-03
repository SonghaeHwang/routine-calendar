package com.kikilog.routinecalendar.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelTabTest {

    @Test
    fun `기본 첫 화면은 캘린더`() {
        assertEquals(TopLevelTab.CALENDAR, DEFAULT_START_TAB)
    }

    @Test
    fun `첫 화면 탭은 그 탭 하나만`() {
        assertEquals(listOf(CalendarKey), backStackFor(TopLevelTab.CALENDAR))
    }

    @Test
    fun `다른 탭은 첫 화면 위에 쌓아서 뒤로 가면 첫 화면으로 온다`() {
        assertEquals(listOf(CalendarKey, TodayKey), backStackFor(TopLevelTab.TODAY))
        assertEquals(listOf(CalendarKey, RoutinesKey), backStackFor(TopLevelTab.ROUTINES))
    }

    @Test
    fun `첫 화면을 바꾸면 그 탭이 기준이 된다 - 나중에 설정으로 고를 때`() {
        assertEquals(listOf(TodayKey), backStackFor(TopLevelTab.TODAY, startTab = TopLevelTab.TODAY))
        assertEquals(listOf(TodayKey, CalendarKey), backStackFor(TopLevelTab.CALENDAR, startTab = TopLevelTab.TODAY))
        assertEquals(listOf(RoutinesKey, TodayKey), backStackFor(TopLevelTab.TODAY, startTab = TopLevelTab.ROUTINES))
    }

    @Test
    fun `설정의 첫 화면을 탭으로`() {
        assertEquals(TopLevelTab.TODAY, com.kikilog.routinecalendar.data.StartScreen.TODAY.toTab())
        assertEquals(TopLevelTab.CALENDAR, com.kikilog.routinecalendar.data.StartScreen.CALENDAR.toTab())
        assertEquals(TopLevelTab.ROUTINES, com.kikilog.routinecalendar.data.StartScreen.ROUTINES.toTab())
        assertEquals(TopLevelTab.CALENDAR, com.kikilog.routinecalendar.data.DEFAULT_START_SCREEN.toTab())
    }

    @Test
    fun `탭은 오늘 · 캘린더 · 루틴 순서`() {
        assertEquals(listOf(TodayKey, CalendarKey, RoutinesKey), TopLevelTab.entries.map { it.key })
    }
}
