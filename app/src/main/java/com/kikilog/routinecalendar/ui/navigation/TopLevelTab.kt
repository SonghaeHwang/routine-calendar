package com.kikilog.routinecalendar.ui.navigation

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.navigation3.runtime.NavKey
import com.kikilog.routinecalendar.R

/** 하단 탭 3개 */
enum class TopLevelTab(val key: NavKey, @param:StringRes val label: Int, @param:DrawableRes val icon: Int) {
    TODAY(TodayKey, R.string.tab_today, R.drawable.ic_today),
    CALENDAR(CalendarKey, R.string.tab_calendar, R.drawable.ic_calendar),
    ROUTINES(RoutinesKey, R.string.tab_routines, R.drawable.ic_list),
}

/** 앱을 켰을 때 처음 보이는 탭. 4단계에서 설정(DataStore)으로 고를 수 있게 한다. */
val DEFAULT_START_TAB: TopLevelTab = TopLevelTab.CALENDAR

/**
 * 탭을 누르면 뒤로 가기 목록을 「첫 화면」 또는 「첫 화면, 그 탭」 으로 다시 만든다.
 * 다른 탭에서 뒤로 가면 첫 화면으로, 첫 화면에서 뒤로 가면 앱이 닫힌다.
 */
fun backStackFor(tab: TopLevelTab, startTab: TopLevelTab = DEFAULT_START_TAB): List<NavKey> =
    if (tab == startTab) listOf(startTab.key) else listOf(startTab.key, tab.key)
