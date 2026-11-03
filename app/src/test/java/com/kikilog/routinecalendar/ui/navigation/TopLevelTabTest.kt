package com.kikilog.routinecalendar.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class TopLevelTabTest {

    @Test
    fun `기본 첫 화면은 달력`() {
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
        // 오늘 탭을 숨긴 뒤로 저장돼 있던 「오늘」은 달력으로 연다
        assertEquals(TopLevelTab.CALENDAR, com.kikilog.routinecalendar.data.StartScreen.TODAY.toTab())
        assertEquals(TopLevelTab.CALENDAR, com.kikilog.routinecalendar.data.StartScreen.CALENDAR.toTab())
        assertEquals(TopLevelTab.ROUTINES, com.kikilog.routinecalendar.data.StartScreen.ROUTINES.toTab())
        assertEquals(TopLevelTab.CALENDAR, com.kikilog.routinecalendar.data.DEFAULT_START_SCREEN.toTab())
    }

    @Test
    fun `하단 탭은 달력 · 루틴 - 오늘 탭은 숨김`() {
        assertEquals(listOf(CalendarKey, RoutinesKey), VISIBLE_TABS.map { it.key })
        // 다시 살릴 수 있도록 탭 자체는 남아 있다
        assertEquals(listOf(TodayKey, CalendarKey, RoutinesKey), TopLevelTab.entries.map { it.key })
    }

    @Test
    fun `알림을 누르면 달력 탭`() {
        assertEquals(TopLevelTab.CALENDAR.name, com.kikilog.routinecalendar.notify.OPEN_TAB_ON_TAP)
    }
}
