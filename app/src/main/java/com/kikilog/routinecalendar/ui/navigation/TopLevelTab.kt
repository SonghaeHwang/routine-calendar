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

/**
 * 탭을 누르면 뒤로 가기 목록을 「오늘」 또는 「오늘, 그 탭」 으로 다시 만든다.
 * 다른 탭에서 뒤로 가면 오늘로, 오늘에서 뒤로 가면 앱이 닫힌다.
 */
fun backStackFor(tab: TopLevelTab): List<NavKey> =
    if (tab == TopLevelTab.TODAY) listOf(TodayKey) else listOf(TodayKey, tab.key)
