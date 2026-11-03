package com.kikilog.routinecalendar.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.schedule.CalendarDay
import com.kikilog.routinecalendar.core.schedule.CalendarEntry
import com.kikilog.routinecalendar.core.schedule.DEFAULT_WEEK_START
import com.kikilog.routinecalendar.core.schedule.buildCalendarDays
import com.kikilog.routinecalendar.core.schedule.calendarEntriesOn
import com.kikilog.routinecalendar.core.schedule.firstOfMonth
import com.kikilog.routinecalendar.core.schedule.monthGridDates
import com.kikilog.routinecalendar.core.schedule.shiftMonth
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

data class CalendarUiState(
    val isLoading: Boolean = true,
    val today: LocalDate? = null,
    /** 보고 있는 달의 1일 */
    val month: LocalDate? = null,
    val weekStart: DayOfWeek = DEFAULT_WEEK_START,
    /** 달력 칸 날짜(앞뒤 달 포함, 7의 배수) */
    val gridDates: List<LocalDate> = emptyList(),
    val days: Map<LocalDate, CalendarDay> = emptyMap(),
    val selectedDate: LocalDate? = null,
    /** 고른 날짜의 목록 */
    val entries: List<CalendarEntry> = emptyList(),
)

/**
 * 월 캘린더. 처음에는 오늘이 든 달과 오늘을 보여 준다.
 * 지난 날짜(오늘 포함)는 목록에서 그날 한 것을 기록 · 취소할 수 있다.
 */
class CalendarViewModel(
    private val repository: RoutineRepository,
    today: StateFlow<LocalDate>,
    weekStart: Flow<DayOfWeek>,
) : ViewModel() {

    // null 이면 오늘 기준(날짜가 바뀌면 따라간다)
    private val month = MutableStateFlow<LocalDate?>(null)
    private val selected = MutableStateFlow<LocalDate?>(null)

    val state: StateFlow<CalendarUiState> =
        combine(repository.observeRoutines(), today, weekStart, month, selected) { list, date, start, m, sel ->
            val items = list.map { it.routine to it.completions }
            val shownMonth = m ?: firstOfMonth(date)
            val grid = monthGridDates(shownMonth, start)
            val selectedDate = sel ?: date
            CalendarUiState(
                isLoading = false,
                today = date,
                month = shownMonth,
                weekStart = start,
                gridDates = grid,
                days = buildCalendarDays(items, grid.first()..grid.last()),
                selectedDate = selectedDate,
                entries = calendarEntriesOn(items, selectedDate),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CalendarUiState())

    fun onPrevMonth() = moveMonth(-1)

    fun onNextMonth() = moveMonth(1)

    private fun moveMonth(by: Int) {
        val current = state.value.month ?: return
        val target = shiftMonth(current, by)
        month.value = target
        // 오늘이 든 달이면 오늘, 아니면 그 달 1일을 고른다
        val today = state.value.today
        selected.value = if (today != null && firstOfMonth(today) == target) today else target
    }

    /** 오늘로 돌아가기 */
    fun onGoToday() {
        month.value = null
        selected.value = null
    }

    /** 앞뒤 달 칸을 누르면 그 달로 넘어간다 */
    fun onSelect(date: LocalDate) {
        selected.value = date
        if (firstOfMonth(date) != state.value.month) month.value = firstOfMonth(date)
    }

    /** 고른 날짜에 한 것 기록 · 취소. 아직 오지 않은 날은 무시한다 */
    fun onToggle(entry: CalendarEntry) {
        val date = state.value.selectedDate ?: return
        val today = state.value.today ?: return
        if (date > today) return
        viewModelScope.launch {
            runCatching {
                if (entry.done) repository.uncheck(entry.routine.id, date) else repository.check(entry.routine.id, date)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                CalendarViewModel(container.routineRepository, container.todayClock.today, container.weekStart)
            }
        }
    }
}
