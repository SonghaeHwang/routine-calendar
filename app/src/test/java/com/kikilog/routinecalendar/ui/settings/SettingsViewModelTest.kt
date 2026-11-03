package com.kikilog.routinecalendar.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kikilog.routinecalendar.data.SettingsRepository
import com.kikilog.routinecalendar.data.StartScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Android 가 필요 없는 일반 JUnit 시험. 임시 파일에 DataStore 를 만든다. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun TestScope.newRepository() = SettingsRepository(
        PreferenceDataStoreFactory.create(
            scope = backgroundScope,
            produceFile = { folder.newFile("settings.preferences_pb") },
        ),
    )

    @Test
    fun `처음에는 일요일 시작 · 첫 화면 캘린더`() = runTest {
        val viewModel = SettingsViewModel(newRepository())
        val state = viewModel.state.first { !it.isLoading }
        assertEquals(DayOfWeek.SUNDAY, state.weekStart)
        assertEquals(StartScreen.CALENDAR, state.startScreen)
    }

    @Test
    fun `고른 값이 저장소와 화면 상태에 반영된다`() = runTest {
        val repository = newRepository()
        val viewModel = SettingsViewModel(repository)

        viewModel.setWeekStart(DayOfWeek.MONDAY)
        viewModel.setStartScreen(StartScreen.ROUTINES)

        val state = viewModel.state.first { it.weekStart == DayOfWeek.MONDAY && it.startScreen == StartScreen.ROUTINES }
        assertEquals(DayOfWeek.MONDAY, state.weekStart)
        assertEquals(DayOfWeek.MONDAY, repository.weekStart.first())
        assertEquals(StartScreen.ROUTINES, repository.startScreen.first())
    }

    @Test
    fun `주 시작 요일은 일요일과 월요일 두 가지`() {
        assertEquals(listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY), WEEK_START_OPTIONS)
    }
}
