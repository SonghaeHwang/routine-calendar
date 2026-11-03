package com.kikilog.routinecalendar.ui.today

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.RoutineDraft
import com.kikilog.routinecalendar.data.RoutineRepository
import com.kikilog.routinecalendar.data.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TodayViewModelTest {

    @get:Rule
    val folder = TemporaryFolder()

    /** 2026-11-02 는 월요일 */
    private val today = MutableStateFlow(LocalDate.parse("2026-11-02"))
    private lateinit var db: AppDatabase
    private lateinit var repository: RoutineRepository
    private lateinit var viewModel: TodayViewModel
    private lateinit var settings: SettingsRepository
    private val storeScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    /** 예약을 맞춘 횟수. 저장이 끝난 뒤에 불리므로 기다려서 확인한다 */
    private val syncCalls = Channel<Unit>(Channel.UNLIMITED)

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoutineRepository(db.routineDao(), today = { today.value }, now = { 1_000L })
        settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = storeScope, produceFile = { folder.newFile("settings.preferences_pb") }),
        )
        viewModel = TodayViewModel(repository, today, settings, syncNotify = { syncCalls.send(Unit) })
    }

    @After
    fun tearDown() {
        db.close()
        storeScope.cancel()
        Dispatchers.resetMain()
    }

    private suspend fun add(title: String, amount: Int, lastDoneOn: String? = null): Long =
        repository.add(
            RoutineDraft(title, Category.ETC, Interval(amount, IntervalUnit.DAY), repeats = true, notify = true),
            lastDoneOn?.let(LocalDate::parse),
        )

    @Test
    fun `루틴이 없으면 빈 상태`() = runTest {
        val state = viewModel.state.first { !it.isLoading }
        assertFalse(state.hasAnyRoutine)
        assertTrue(state.board.isEmpty)
    }

    @Test
    fun `지난 것 · 오늘 · 다가오는 7일로 나뉜다`() = runTest {
        add("각질제거", amount = 10, lastDoneOn = "2026-10-20") // 예정 10-30 → 3일 지남
        add("모델링팩", amount = 7) // 마지막으로 한 날 없음 → 오늘
        add("이불 세탁", amount = 7, lastDoneOn = "2026-10-30") // 예정 11-06(금) → 다가오는 7일

        val board = viewModel.state.first { it.hasAnyRoutine && !it.board.isEmpty && it.board.upcoming.isNotEmpty() }.board
        assertEquals(listOf("각질제거"), board.overdue.map { it.routine.title })
        assertEquals(3, board.overdue.single().overdueDays)
        assertEquals(listOf("모델링팩"), board.dueToday.map { it.routine.title })
        assertEquals(listOf("이불 세탁"), board.upcoming.map { it.routine.title })
    }

    @Test
    fun `체크하면 오늘 칸에 체크된 채로 남고, 취소하면 원래 칸으로 돌아간다`() = runTest {
        add("각질제거", amount = 10, lastDoneOn = "2026-10-20")
        val overdue = viewModel.state.first { it.board.overdue.isNotEmpty() }.board.overdue.single()

        viewModel.onToggle(overdue)
        val checked = viewModel.state.first { it.board.dueToday.isNotEmpty() }.board
        assertTrue(checked.dueToday.single().doneToday)
        assertTrue(checked.overdue.isEmpty())

        viewModel.onToggle(checked.dueToday.single())
        val back = viewModel.state.first { it.board.overdue.isNotEmpty() }.board
        assertTrue(back.dueToday.isEmpty())
        assertEquals("각질제거", back.overdue.single().routine.title)
    }

    @Test
    fun `다가오는 루틴을 일찍 체크하면 오늘 기록으로 저장된다`() = runTest {
        val id = add("이불 세탁", amount = 7, lastDoneOn = "2026-10-30")
        val entry = viewModel.state.first { it.board.upcoming.isNotEmpty() }.board.upcoming.single()

        viewModel.onToggle(entry)
        viewModel.state.first { it.board.dueToday.isNotEmpty() }
        assertEquals(
            listOf(LocalDate.parse("2026-10-30"), LocalDate.parse("2026-11-02")),
            db.routineDao().completionsOf(id).map { it.doneOn },
        )
    }

    @Test
    fun `날짜가 바뀌면 칸이 다시 나뉜다`() = runTest {
        add("모델링팩", amount = 7) // 첫 예정일 = 11-02
        viewModel.state.first { it.board.dueToday.isNotEmpty() }

        today.value = LocalDate.parse("2026-11-03") // 자정이 지남
        val board = viewModel.state.first { it.today == LocalDate.parse("2026-11-03") }.board
        assertEquals(1, board.overdue.single().overdueDays)
        assertTrue(board.dueToday.isEmpty())
    }

    // --- 알림 카드 ---

    @Test
    fun `루틴이 없으면 알림 카드를 보이지 않고, 루틴이 생기면 보인다`() = runTest {
        assertFalse(viewModel.state.first { !it.isLoading }.showNotifyCard)
        add("모델링팩", amount = 7)
        assertTrue(viewModel.state.first { it.hasAnyRoutine }.showNotifyCard)
    }

    @Test
    fun `켜기를 허용하면 알림이 켜지고 예약하고 카드가 사라진다`() = runTest {
        add("모델링팩", amount = 7)
        viewModel.state.first { it.showNotifyCard }

        viewModel.onNotifyPermissionResult(granted = true)

        syncCalls.receive()
        viewModel.state.first { it.hasAnyRoutine && !it.showNotifyCard }
        assertTrue(settings.notifyEnabled.first())
    }

    @Test
    fun `권한을 거절하거나 나중에를 누르면 카드만 닫히고 알림은 꺼진 채`() = runTest {
        add("모델링팩", amount = 7)
        viewModel.state.first { it.showNotifyCard }

        viewModel.onNotifyPermissionResult(granted = false)

        viewModel.state.first { it.hasAnyRoutine && !it.showNotifyCard }
        assertFalse(settings.notifyEnabled.first())
        assertTrue(settings.notifyCardDismissed.first())
        assertTrue(syncCalls.tryReceive().isFailure)
    }
}
