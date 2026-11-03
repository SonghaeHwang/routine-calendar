package com.kikilog.routinecalendar.ui.today

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.RoutineDraft
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class TodayViewModelTest {

    /** 2026-11-02 는 월요일 */
    private val today = MutableStateFlow(LocalDate.parse("2026-11-02"))
    private lateinit var db: AppDatabase
    private lateinit var repository: RoutineRepository
    private lateinit var viewModel: TodayViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoutineRepository(db.routineDao(), today = { today.value }, now = { 1_000L })
        viewModel = TodayViewModel(repository, today, flowOf(DayOfWeek.SUNDAY))
    }

    @After
    fun tearDown() {
        db.close()
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
    fun `지난 것 · 오늘 · 이번 주로 나뉜다`() = runTest {
        add("각질제거", amount = 10, lastDoneOn = "2026-10-20") // 예정 10-30 → 3일 지남
        add("모델링팩", amount = 7) // 마지막으로 한 날 없음 → 오늘
        add("이불 세탁", amount = 7, lastDoneOn = "2026-10-30") // 예정 11-06(금) → 이번 주

        val board = viewModel.state.first { it.hasAnyRoutine && !it.board.isEmpty && it.board.thisWeek.isNotEmpty() }.board
        assertEquals(listOf("각질제거"), board.overdue.map { it.routine.title })
        assertEquals(3, board.overdue.single().overdueDays)
        assertEquals(listOf("모델링팩"), board.dueToday.map { it.routine.title })
        assertEquals(listOf("이불 세탁"), board.thisWeek.map { it.routine.title })
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
    fun `이번 주 루틴을 일찍 체크하면 오늘 기록으로 저장된다`() = runTest {
        val id = add("이불 세탁", amount = 7, lastDoneOn = "2026-10-30")
        val entry = viewModel.state.first { it.board.thisWeek.isNotEmpty() }.board.thisWeek.single()

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
}
