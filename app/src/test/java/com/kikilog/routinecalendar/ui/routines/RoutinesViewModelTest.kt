package com.kikilog.routinecalendar.ui.routines

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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class RoutinesViewModelTest {

    private val today = MutableStateFlow(LocalDate.parse("2026-11-02"))
    private lateinit var db: AppDatabase
    private lateinit var repository: RoutineRepository
    private lateinit var viewModel: RoutinesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoutineRepository(db.routineDao(), today = { today.value }, now = { 1_000L })
        viewModel = RoutinesViewModel(repository, today)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun add(title: String, lastDoneOn: String? = null): Long =
        repository.add(
            RoutineDraft(title, Category.ETC, Interval(7, IntervalUnit.DAY), repeats = true, notify = true),
            lastDoneOn?.let(LocalDate::parse),
        )

    private suspend fun stateWhen(predicate: (RoutinesUiState) -> Boolean) = viewModel.state.first(predicate)

    private suspend fun titles(predicate: (RoutinesUiState) -> Boolean) = stateWhen(predicate).rows.map { it.title }

    @Test
    fun `꾹 누르면 선택 모드가 되고, 누를 때마다 더하거나 빼며 모두 빼면 끝난다`() = runTest {
        val a = add("젤팩")
        val b = add("청소기")
        stateWhen { it.rows.size == 2 }

        viewModel.onStartSelection(a)
        assertEquals(setOf(a), stateWhen { it.isSelecting }.selectedIds)

        viewModel.onToggleSelection(b)
        assertEquals(setOf(a, b), stateWhen { it.selectedIds.size == 2 }.selectedIds)

        viewModel.onToggleSelection(a)
        viewModel.onToggleSelection(b)
        assertFalse(stateWhen { it.selectedIds.isEmpty() }.isSelecting)
    }

    @Test
    fun `선택 삭제는 고른 루틴과 그 기록만 지운다`() = runTest {
        val a = add("젤팩", lastDoneOn = "2026-11-01")
        val b = add("청소기")
        add("빨래")
        stateWhen { it.rows.size == 3 }

        viewModel.onStartSelection(a)
        viewModel.onToggleSelection(b)
        stateWhen { it.selectedIds.size == 2 }
        viewModel.onDeleteSelected()

        assertEquals(listOf("빨래"), titles { it.rows.size == 1 })
        assertEquals(emptyList<Any>(), db.routineDao().completionsOf(a))
        assertFalse(viewModel.state.value.isSelecting)
    }

    @Test
    fun `전체 삭제는 목록에 보이는 루틴만 지운다 - 보관함 보기가 꺼져 있으면 보관한 루틴은 남는다`() = runTest {
        add("젤팩")
        add("청소기")
        val archived = add("화장지")
        repository.setArchived(archived, true)
        stateWhen { it.rows.size == 2 }

        viewModel.onStartSelection(stateWhen { !it.isLoading }.rows.first().id)
        viewModel.onDeleteAll()
        stateWhen { it.rows.isEmpty() }

        viewModel.onToggleShowArchived()
        assertEquals(listOf("화장지"), titles { it.showArchived && it.rows.size == 1 })
    }

    @Test
    fun `선택 보관하면 목록에서 빠지고, 보관함에서 선택 보관 해제하면 돌아온다`() = runTest {
        val a = add("젤팩")
        add("청소기")
        stateWhen { it.rows.size == 2 }

        viewModel.onStartSelection(a)
        viewModel.onArchiveSelected(true)
        assertEquals(listOf("청소기"), titles { it.rows.size == 1 })

        viewModel.onToggleShowArchived()
        val shown = stateWhen { it.showArchived && it.rows.size == 2 }
        assertTrue(shown.rows.single { it.id == a }.archived)

        viewModel.onStartSelection(a)
        viewModel.onArchiveSelected(false)
        viewModel.onToggleShowArchived()
        assertEquals(2, titles { !it.showArchived && it.rows.size == 2 }.size)
    }

    @Test
    fun `보관함 보기를 바꾸면 선택은 풀린다`() = runTest {
        val a = add("젤팩")
        stateWhen { it.rows.size == 1 }
        viewModel.onStartSelection(a)
        stateWhen { it.isSelecting }

        viewModel.onToggleShowArchived()
        assertFalse(stateWhen { it.showArchived }.isSelecting)
    }
}
