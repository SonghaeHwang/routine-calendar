package com.kikilog.routinecalendar.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.schedule.TodayBoard
import com.kikilog.routinecalendar.core.schedule.TodayEntry
import com.kikilog.routinecalendar.core.schedule.buildTodayBoard
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class TodayUiState(
    val isLoading: Boolean = true,
    val today: LocalDate? = null,
    val board: TodayBoard = TodayBoard.EMPTY,
    val hasAnyRoutine: Boolean = false,
)

/** 오늘 화면. 날짜가 바뀌면([today]) 칸이 저절로 다시 나뉜다. */
class TodayViewModel(
    private val repository: RoutineRepository,
    today: StateFlow<LocalDate>,
) : ViewModel() {

    val state: StateFlow<TodayUiState> =
        combine(repository.observeRoutines(), today) { items, date ->
            TodayUiState(
                isLoading = false,
                today = date,
                board = buildTodayBoard(items.map { it.routine to it.completions }, date),
                hasAnyRoutine = items.isNotEmpty(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /**
     * 체크 · 취소. 날짜는 화면에 보이는 오늘을 쓴다 — 자정 직전에 누른 체크가 다음 날로 저장되지 않게.
     */
    fun onToggle(entry: TodayEntry) {
        val date = state.value.today ?: return
        viewModelScope.launch {
            runCatching {
                if (entry.doneToday) {
                    repository.uncheck(entry.routine.id, date)
                } else {
                    repository.check(entry.routine.id, date)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                TodayViewModel(container.routineRepository, container.todayClock.today)
            }
        }
    }
}
