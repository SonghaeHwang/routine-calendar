package com.kikilog.routinecalendar.ui.info

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
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoutineInfoViewModelTest {

    private val today = MutableStateFlow(LocalDate.parse("2026-11-03"))
    private lateinit var db: AppDatabase
    private lateinit var repository: RoutineRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoutineRepository(db.routineDao(), today = { today.value }, now = { 1_000L })
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    @Test
    fun `분류 · 다음 예정일 · 마지막으로 한 날`() = runTest {
        val id = repository.add(
            RoutineDraft("젤팩", Category.BEAUTY, Interval(1, IntervalUnit.WEEK), repeats = true, notify = true),
            LocalDate.parse("2026-10-20"),
        )
        val state = RoutineInfoViewModel(repository, id, today).state.first { !it.isLoading }

        assertEquals("젤팩", state.routine?.title)
        assertEquals("뷰티", state.category?.name)
        assertEquals(LocalDate.parse("2026-10-20"), state.lastDoneOn)
        assertEquals(LocalDate.parse("2026-10-27"), state.nextDue)
        assertEquals(7, state.overdueDays)
    }

    @Test
    fun `정보 화면에서 알림을 끄면 루틴에 저장된다`() = runTest {
        val id = repository.add(
            RoutineDraft("젤팩", Category.BEAUTY, Interval(1, IntervalUnit.WEEK), repeats = true, notify = true),
            null,
        )
        val viewModel = RoutineInfoViewModel(repository, id, today)
        viewModel.state.first { it.routine != null }

        viewModel.setNotify(false)
        assertEquals(false, viewModel.state.first { it.routine?.notify == false }.routine?.notify)
    }

    @Test
    fun `지워진 루틴이면 notFound`() = runTest {
        val id = repository.add(
            RoutineDraft("젤팩", Category.BEAUTY, Interval(1, IntervalUnit.WEEK), repeats = true, notify = true),
            null,
        )
        val viewModel = RoutineInfoViewModel(repository, id, today)
        viewModel.state.first { it.routine != null }

        repository.delete(id)
        assertTrue(viewModel.state.first { !it.isLoading && it.routine == null }.notFound)
    }
}
