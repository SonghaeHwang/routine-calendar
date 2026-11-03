package com.kikilog.routinecalendar.ui.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kikilog.routinecalendar.core.notify.NotifyTime
import com.kikilog.routinecalendar.data.SettingsRepository
import com.kikilog.routinecalendar.data.StartScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/** Android 가 필요 없는 일반 JUnit 시험. 임시 파일에 DataStore 를 만든다. */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** 예약을 맞춘 횟수. 저장이 끝난 뒤에 불리므로 기다려서 확인한다 */
    private val syncCalls = Channel<Unit>(Channel.UNLIMITED)

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

    private fun viewModel(repository: SettingsRepository) = SettingsViewModel(repository, syncNotify = { syncCalls.send(Unit) })

    @Test
    fun `처음에는 일요일 시작 · 첫 화면 캘린더 · 알림 꺼짐 08시`() = runTest {
        val state = viewModel(newRepository()).state.first { !it.isLoading }
        assertEquals(DayOfWeek.SUNDAY, state.weekStart)
        assertEquals(StartScreen.CALENDAR, state.startScreen)
        assertFalse(state.notifyEnabled)
        assertEquals(NotifyTime(8, 0), state.notifyTime)
    }

    @Test
    fun `고른 값이 저장소와 화면 상태에 반영된다`() = runTest {
        val repository = newRepository()
        val viewModel = viewModel(repository)

        viewModel.setWeekStart(DayOfWeek.MONDAY)
        viewModel.setStartScreen(StartScreen.ROUTINES)

        val state = viewModel.state.first { it.weekStart == DayOfWeek.MONDAY && it.startScreen == StartScreen.ROUTINES }
        assertEquals(DayOfWeek.MONDAY, state.weekStart)
        assertEquals(DayOfWeek.MONDAY, repository.weekStart.first())
        assertEquals(StartScreen.ROUTINES, repository.startScreen.first())
    }

    @Test
    fun `알림을 켜면 예약을 맞추고 오늘 화면 카드는 닫힌다`() = runTest {
        val repository = newRepository()
        val viewModel = viewModel(repository)

        viewModel.setNotifyEnabled(true)

        syncCalls.receive()
        assertTrue(viewModel.state.first { it.notifyEnabled }.notifyEnabled)
        assertTrue(repository.notifyCardDismissed.first())
    }

    @Test
    fun `알림 시각을 바꾸면 저장하고 예약을 다시 맞춘다`() = runTest {
        val repository = newRepository()
        val viewModel = viewModel(repository)

        viewModel.setNotifyTime(NotifyTime(21, 30))

        syncCalls.receive()
        assertEquals(NotifyTime(21, 30), repository.notifyTime.first())
        assertTrue(syncCalls.tryReceive().isFailure)
    }

    @Test
    fun `주 시작 요일은 일요일과 월요일 두 가지`() {
        assertEquals(listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY), WEEK_START_OPTIONS)
    }
}
