package com.kikilog.routinecalendar.ui.calendar

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
import kotlinx.datetime.DayOfWeek
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
class CalendarViewModelTest {

    /** 2026-11-03 은 화요일 */
    private val today = MutableStateFlow(LocalDate.parse("2026-11-03"))
    private val weekStart = MutableStateFlow(DayOfWeek.SUNDAY)
    private lateinit var db: AppDatabase
    private lateinit var repository: RoutineRepository
    private lateinit var viewModel: CalendarViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = RoutineRepository(db.routineDao(), today = { today.value }, now = { 1_000L })
        viewModel = CalendarViewModel(repository, today, weekStart)
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private fun d(s: String) = LocalDate.parse(s)

    private suspend fun stateWhen(predicate: (CalendarUiState) -> Boolean) = viewModel.state.first(predicate)

    private suspend fun add(title: String, amount: Int, lastDoneOn: String? = null): Long =
        repository.add(
            RoutineDraft(title, Category.ETC, Interval(amount, IntervalUnit.DAY), repeats = true, notify = true),
            lastDoneOn?.let(::d),
        )

    @Test
    fun `처음에는 오늘이 든 달과 오늘을 보여 준다`() = runTest {
        val s = stateWhen { !it.isLoading }
        assertEquals(d("2026-11-01"), s.month)
        assertEquals(d("2026-11-03"), s.selectedDate)
        assertEquals(d("2026-11-01"), s.gridDates.first())
    }

    @Test
    fun `다음 달 · 이전 달로 옮기고, 오늘 단추로 돌아온다`() = runTest {
        stateWhen { !it.isLoading }

        viewModel.onNextMonth()
        val dec = stateWhen { it.month == d("2026-12-01") }
        assertEquals(d("2026-12-01"), dec.selectedDate)

        viewModel.onPrevMonth()
        // 오늘이 든 달로 돌아오면 오늘을 고른다
        assertEquals(d("2026-11-03"), stateWhen { it.month == d("2026-11-01") }.selectedDate)

        viewModel.onPrevMonth()
        stateWhen { it.month == d("2026-10-01") }
        viewModel.onGoToday()
        val back = stateWhen { it.month == d("2026-11-01") }
        assertEquals(d("2026-11-03"), back.selectedDate)
    }

    @Test
    fun `다음 달 칸을 누르면 그 달로 넘어간다`() = runTest {
        stateWhen { !it.isLoading }
        viewModel.onSelect(d("2026-12-02"))
        val s = stateWhen { it.month == d("2026-12-01") }
        assertEquals(d("2026-12-02"), s.selectedDate)
    }

    @Test
    fun `주 시작 요일을 월요일로 바꾸면 달력 첫 칸이 바뀐다`() = runTest {
        stateWhen { !it.isLoading }
        weekStart.value = DayOfWeek.MONDAY
        val s = stateWhen { it.weekStart == DayOfWeek.MONDAY }
        assertEquals(d("2026-10-26"), s.gridDates.first())
    }

    @Test
    fun `지난 날짜를 골라 체크하면 그날로 기록된다`() = runTest {
        // 10-20 에 했고 10일마다 → 10-30 이 예정이었는데 안 함
        val id = add("각질제거", 10, lastDoneOn = "2026-10-20")
        stateWhen { it.days.isNotEmpty() }

        viewModel.onSelect(d("2026-10-30"))
        val before = stateWhen { it.selectedDate == d("2026-10-30") && it.entries.isNotEmpty() }
        val entry = before.entries.single()
        assertTrue(entry.due)

        viewModel.onToggle(entry)
        val after = stateWhen { s -> s.entries.any { it.done } }
        assertTrue(after.entries.single().done)
        assertEquals(listOf(d("2026-10-20"), d("2026-10-30")), db.routineDao().completionsOf(id).map { it.doneOn })
    }

    @Test
    fun `아직 오지 않은 날은 체크해도 기록되지 않는다`() = runTest {
        val id = add("청소기", 7, lastDoneOn = "2026-11-02") // 다음 11-09
        stateWhen { it.days.isNotEmpty() }

        viewModel.onSelect(d("2026-11-09"))
        val entry = stateWhen { it.selectedDate == d("2026-11-09") && it.entries.isNotEmpty() }.entries.single()
        viewModel.onToggle(entry)

        assertEquals(listOf(d("2026-11-02")), db.routineDao().completionsOf(id).map { it.doneOn })
    }

    @Test
    fun `칸의 분류는 분류당 하나씩 분류 순서대로`() = runTest {
        // 오늘(11-03) 예정: 기타 2개 + 집안일 1개
        add("여권", 7)
        add("우산", 7)
        repository.add(
            RoutineDraft("청소기", Category.HOUSE, Interval(7, IntervalUnit.DAY), repeats = true, notify = true),
            null,
        )
        val state = stateWhen { it.days[d("2026-11-03")]?.dueIds?.size == 3 }

        val categories = state.categoriesOf(state.days.getValue(d("2026-11-03")).dueIds)
        assertEquals(listOf(Category.HOUSE, Category.ETC), categories.map { it.id })
    }

    @Test
    fun `설정을 넘기지 않으면 알림 카드는 보이지 않는다`() = runTest {
        add("여권", 7)
        assertEquals(false, stateWhen { it.entries.isNotEmpty() }.showNotifyCard)
    }
}
