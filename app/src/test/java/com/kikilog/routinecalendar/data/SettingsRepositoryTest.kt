package com.kikilog.routinecalendar.data

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Android 가 필요 없는 일반 JUnit 시험. 임시 파일에 DataStore 를 만든다. */
class SettingsRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private fun TestScope.newStore() = PreferenceDataStoreFactory.create(
        scope = backgroundScope,
        produceFile = { folder.newFile("settings.preferences_pb") },
    )

    @Test
    fun `처음에는 일요일 시작 · 첫 화면 캘린더`() = runTest {
        val repo = SettingsRepository(newStore())
        assertEquals(DayOfWeek.SUNDAY, repo.weekStart.first())
        assertEquals(StartScreen.CALENDAR, repo.startScreen.first())
    }

    @Test
    fun `바꾼 값이 저장된다`() = runTest {
        val repo = SettingsRepository(newStore())
        repo.setWeekStart(DayOfWeek.MONDAY)
        repo.setStartScreen(StartScreen.TODAY)

        assertEquals(DayOfWeek.MONDAY, repo.weekStart.first())
        assertEquals(StartScreen.TODAY, repo.startScreen.first())
    }

    @Test
    fun `알 수 없는 값이 저장돼 있으면 기본값`() = runTest {
        val store = newStore()
        store.edit {
            it[stringPreferencesKey("week_start")] = "SOMEDAY"
            it[stringPreferencesKey("start_screen")] = "WIDGET"
        }
        val repo = SettingsRepository(store)
        assertEquals(DayOfWeek.SUNDAY, repo.weekStart.first())
        assertEquals(StartScreen.CALENDAR, repo.startScreen.first())
    }
}
