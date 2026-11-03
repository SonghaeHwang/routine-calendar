package com.kikilog.routinecalendar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kikilog.routinecalendar.core.schedule.DEFAULT_WEEK_START
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek

/** 앱을 켰을 때 처음 보이는 화면 */
enum class StartScreen { TODAY, CALENDAR, ROUTINES }

val DEFAULT_START_SCREEN = StartScreen.CALENDAR

/** 설정 파일. 앱 전체에서 하나만 연다 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 앱 설정(DataStore). 이 휴대폰 안에만 저장한다.
 * 값이 없거나 알 수 없는 값이면 기본값(주 시작 일요일 · 첫 화면 캘린더).
 */
class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    /** 일요일 또는 월요일만 고를 수 있다 */
    val weekStart: Flow<DayOfWeek> =
        dataStore.data
            .map { prefs -> prefs[WEEK_START]?.let { name -> DayOfWeek.entries.firstOrNull { it.name == name } } ?: DEFAULT_WEEK_START }
            .distinctUntilChanged()

    val startScreen: Flow<StartScreen> =
        dataStore.data
            .map { prefs -> prefs[START_SCREEN]?.let { name -> StartScreen.entries.firstOrNull { it.name == name } } ?: DEFAULT_START_SCREEN }
            .distinctUntilChanged()

    suspend fun setWeekStart(day: DayOfWeek) {
        dataStore.edit { it[WEEK_START] = day.name }
    }

    suspend fun setStartScreen(screen: StartScreen) {
        dataStore.edit { it[START_SCREEN] = screen.name }
    }

    private companion object {
        val WEEK_START = stringPreferencesKey("week_start")
        val START_SCREEN = stringPreferencesKey("start_screen")
    }
}
