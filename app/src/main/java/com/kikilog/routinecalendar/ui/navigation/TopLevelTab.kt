package com.kikilog.routinecalendar.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.data.StartScreen

/** 탭. 하단에 보이는 것은 [VISIBLE_TABS] 뿐이다 */
enum class TopLevelTab(val key: NavKey, @param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    TODAY(TodayKey, R.string.tab_today, R.drawable.ic_today),
    CALENDAR(CalendarKey, R.string.tab_calendar, R.drawable.ic_calendar),
    ROUTINES(RoutinesKey, R.string.tab_routines, R.drawable.ic_list),
}

/**
 * 하단 탭에 보이는 탭. 오늘 탭은 2026-11-03 사용자 요청으로 숨겼다(화면 코드는 남겨 두었고,
 * 다시 살릴 때는 이 목록에 TODAY 를 넣고 [StartScreen.toTab] · 첫 화면 선택지를 되돌린다).
 */
val VISIBLE_TABS: List<TopLevelTab> = listOf(TopLevelTab.CALENDAR, TopLevelTab.ROUTINES)

/** 앱을 켰을 때 처음 보이는 탭의 기본값. 설정(첫 화면)에서 바꿀 수 있다. */
val DEFAULT_START_TAB: TopLevelTab = TopLevelTab.CALENDAR

/** 설정에 저장된 첫 화면 → 탭. 오늘 탭을 숨기기 전에 저장한 「오늘」은 달력으로 연다 */
fun StartScreen.toTab(): TopLevelTab = when (this) {
    StartScreen.TODAY -> TopLevelTab.CALENDAR
    StartScreen.CALENDAR -> TopLevelTab.CALENDAR
    StartScreen.ROUTINES -> TopLevelTab.ROUTINES
}

/**
 * 탭을 누르면 뒤로 가기 목록을 「첫 화면」 또는 「첫 화면, 그 탭」 으로 다시 만든다.
 * 다른 탭에서 뒤로 가면 첫 화면으로, 첫 화면에서 뒤로 가면 앱이 닫힌다.
 */
fun backStackFor(tab: TopLevelTab, startTab: TopLevelTab = DEFAULT_START_TAB): List<NavKey> =
    if (tab == startTab) listOf(startTab.key) else listOf(startTab.key, tab.key)
