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

    private suspend fun titles(predicate: (RoutinesUiState) -> Boolean = { !it.isLoading }) =
        viewModel.state.first(predicate).rows.map { it.title }

    @Test
    fun `보관하면 목록에서 빠지고, 보관한 루틴 보기를 켜면 다시 보인다`() = runTest {
        val id = add("젤팩")
        add("청소기")
        assertEquals(2, titles { it.rows.size == 2 }.size)

        viewModel.onSetArchived(id, true)
        assertEquals(listOf("청소기"), titles { it.rows.size == 1 })

        viewModel.onToggleShowArchived()
        val shown = viewModel.state.first { it.showArchived && it.rows.size == 2 }
        assertTrue(shown.rows.single { it.id == id }.archived)

        viewModel.onToggleShowArchived()
        assertFalse(viewModel.state.first { !it.showArchived && it.rows.size == 1 }.showArchived)
    }

    @Test
    fun `보관을 해제하면 다시 기본 목록에 보인다`() = runTest {
        val id = add("젤팩")
        viewModel.onSetArchived(id, true)
        viewModel.state.first { !it.isLoading && it.rows.isEmpty() }

        viewModel.onSetArchived(id, false)
        assertEquals(listOf("젤팩"), titles { it.rows.size == 1 })
    }

    @Test
    fun `삭제하면 목록과 완료 기록에서 모두 사라진다`() = runTest {
        val id = add("젤팩", lastDoneOn = "2026-11-01")
        add("청소기")
        titles { it.rows.size == 2 }

        viewModel.onDelete(id)
        assertEquals(listOf("청소기"), titles { it.rows.size == 1 })
        assertEquals(emptyList<Any>(), db.routineDao().completionsOf(id))
    }
}
