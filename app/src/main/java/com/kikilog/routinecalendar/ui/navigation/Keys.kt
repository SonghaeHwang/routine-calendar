package com.kikilog.routinecalendar.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

// 화면 키. @Serializable 이라 화면 회전 · 앱이 메모리에서 정리된 뒤에도 뒤로 가기 목록이 되살아난다.

@Serializable
data object TodayKey : NavKey

@Serializable
data object CalendarKey : NavKey

@Serializable
data object RoutinesKey : NavKey

/** 루틴 등록(routineId = null) 또는 수정 */
@Serializable
data class EditKey(val routineId: Long? = null) : NavKey

@Serializable
data object SettingsKey : NavKey
