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

/** 설정 → 빠른 선택 편집 */
@Serializable
data object PresetsKey : NavKey

/** 설정 · 루틴 편집창 → 분류 편집 */
@Serializable
data object CategoriesKey : NavKey

/** 루틴 정보(읽기 전용). 아래 「편집하기」로 수정 화면으로 간다 */
@Serializable
data class InfoKey(val routineId: Long) : NavKey
