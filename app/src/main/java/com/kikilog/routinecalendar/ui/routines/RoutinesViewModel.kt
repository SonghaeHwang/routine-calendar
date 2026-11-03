package com.kikilog.routinecalendar.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

/** 루틴 목록. 보관 · 삭제와 보관함 보기는 13번 작업에서 더한다. */
class RoutinesViewModel(
    repository: RoutineRepository,
    today: StateFlow<LocalDate>,
) : ViewModel() {

    val state: StateFlow<RoutinesUiState> =
        combine(repository.observeRoutines(), today) { items, date ->
            RoutinesUiState(isLoading = false, today = date, rows = buildRoutineRows(items))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinesUiState())

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                RoutinesViewModel(container.routineRepository, container.todayClock.today)
            }
        }
    }
}
