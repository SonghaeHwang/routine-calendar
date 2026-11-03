package com.kikilog.routinecalendar.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** 루틴 목록. 보관한 루틴 보기를 켜면 보관한 루틴도 함께 보인다. */
@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModel(
    private val repository: RoutineRepository,
    today: StateFlow<LocalDate>,
) : ViewModel() {

    private val showArchived = MutableStateFlow(false)

    val state: StateFlow<RoutinesUiState> =
        combine(
            showArchived.flatMapLatest { repository.observeRoutines(includeArchived = it) },
            today,
            showArchived,
        ) { items, date, archivedShown ->
            RoutinesUiState(isLoading = false, today = date, rows = buildRoutineRows(items), showArchived = archivedShown)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinesUiState())

    fun onToggleShowArchived() = showArchived.update { !it }

    fun onSetArchived(id: Long, archived: Boolean) {
        viewModelScope.launch { repository.setArchived(id, archived) }
    }

    fun onDelete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                RoutinesViewModel(container.routineRepository, container.todayClock.today)
            }
        }
    }
}
