package com.kikilog.routinecalendar.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kikilog.routinecalendar.core.notify.DEFAULT_NOTIFY_TIME
import com.kikilog.routinecalendar.core.notify.NotifyTime
import com.kikilog.routinecalendar.core.schedule.DEFAULT_WEEK_START
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** 앱을 켰을 때 처음 보이는 화면 */
enum class StartScreen { TODAY, CALENDAR, ROUTINES }

val DEFAULT_START_SCREEN = StartScreen.CALENDAR

/** 설정 파일. 앱 전체에서 하나만 연다 */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 앱 설정(DataStore). 이 휴대폰 안에만 저장한다.
 * 값이 없거나 알 수 없는 값이면 기본값(주 시작 일요일 · 첫 화면 캘린더 · 알림 꺼짐 · 알림 08:00).
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

    /** 하루 1회 요약 알림. 새로 설치하면 꺼져 있고, 사용자가 켜면서 권한을 허용해야 켜진다 */
    val notifyEnabled: Flow<Boolean> =
        dataStore.data.map { it[NOTIFY_ENABLED] ?: false }.distinctUntilChanged()

    val notifyTime: Flow<NotifyTime> =
        dataStore.data
            .map { prefs -> prefs[NOTIFY_TIME]?.let(NotifyTime::decode) ?: DEFAULT_NOTIFY_TIME }
            .distinctUntilChanged()

    /** 요약 알림을 마지막으로 보낸 날. 같은 날 두 번 보내지 않으려고 쓴다 */
    val lastNotifiedOn: Flow<LocalDate?> =
        dataStore.data
            .map { prefs -> prefs[LAST_NOTIFIED_ON]?.let { runCatching { LocalDate.parse(it) }.getOrNull() } }
            .distinctUntilChanged()

    /** 오늘 화면의 "알림 켤까요?" 카드를 닫았는지(켜기 · 나중에 모두) */
    val notifyCardDismissed: Flow<Boolean> =
        dataStore.data.map { it[NOTIFY_CARD_DISMISSED] ?: false }.distinctUntilChanged()

    suspend fun setWeekStart(day: DayOfWeek) {
        dataStore.edit { it[WEEK_START] = day.name }
    }

    suspend fun setStartScreen(screen: StartScreen) {
        dataStore.edit { it[START_SCREEN] = screen.name }
    }

    suspend fun setNotifyEnabled(enabled: Boolean) {
        dataStore.edit { it[NOTIFY_ENABLED] = enabled }
    }

    suspend fun setNotifyTime(time: NotifyTime) {
        dataStore.edit { it[NOTIFY_TIME] = time.encode() }
    }

    suspend fun setLastNotifiedOn(date: LocalDate) {
        dataStore.edit { it[LAST_NOTIFIED_ON] = date.toString() }
    }

    suspend fun dismissNotifyCard() {
        dataStore.edit { it[NOTIFY_CARD_DISMISSED] = true }
    }

    private companion object {
        val WEEK_START = stringPreferencesKey("week_start")
        val START_SCREEN = stringPreferencesKey("start_screen")
        val NOTIFY_ENABLED = booleanPreferencesKey("notify_enabled")
        val NOTIFY_TIME = stringPreferencesKey("notify_time")
        val LAST_NOTIFIED_ON = stringPreferencesKey("last_notified_on")
        val NOTIFY_CARD_DISMISSED = booleanPreferencesKey("notify_card_dismissed")
    }
}
