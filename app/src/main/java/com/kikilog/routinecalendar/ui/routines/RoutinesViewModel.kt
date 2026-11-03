package com.kikilog.routinecalendar.ui.routines

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.DEFAULT_CATEGORIES
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * 루틴 목록. 보관한 루틴 보기를 켜면 보관한 루틴도 함께 보인다.
 * 꾹 누르면 선택 모드 — 선택 삭제 · 전체 삭제 · 선택 보관.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoutinesViewModel(
    private val repository: RoutineRepository,
    today: StateFlow<LocalDate>,
    categories: Flow<List<Category>> = flowOf(DEFAULT_CATEGORIES),
) : ViewModel() {

    private val showArchived = MutableStateFlow(false)
    private val selected = MutableStateFlow<Set<Long>>(emptySet())

    val state: StateFlow<RoutinesUiState> =
        combine(
            showArchived.flatMapLatest { repository.observeRoutines(includeArchived = it) },
            today,
            showArchived,
            selected,
            categories,
        ) { items, date, archivedShown, selectedIds, cats ->
            val rows = buildRoutineRows(items)
            RoutinesUiState(
                isLoading = false,
                today = date,
                rows = rows,
                showArchived = archivedShown,
                categories = cats,
                // 지워졌거나 숨겨진 루틴은 선택에서 뺀다
                selectedIds = selectedIds intersect rows.map { it.id }.toSet(),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutinesUiState())

    fun onToggleShowArchived() {
        showArchived.update { !it }
        selected.value = emptySet()
    }

    /** 꾹 누르면 그 루틴을 고른 채로 선택 모드 시작 */
    fun onStartSelection(id: Long) {
        selected.value = setOf(id)
    }

    /** 선택 모드에서 누르면 더하거나 뺀다. 모두 빼면 선택 모드가 끝난다 */
    fun onToggleSelection(id: Long) = selected.update { if (id in it) it - id else it + id }

    fun onClearSelection() {
        selected.value = emptySet()
    }

    fun onDeleteSelected() {
        val ids = state.value.selectedIds
        selected.value = emptySet()
        viewModelScope.launch { repository.delete(ids) }
    }

    /** 지금 목록에 보이는 루틴 전부. 보관함 보기가 꺼져 있으면 보관한 루틴은 남는다 */
    fun onDeleteAll() {
        val ids = state.value.rows.map { it.id }
        selected.value = emptySet()
        viewModelScope.launch { repository.delete(ids) }
    }

    fun onArchiveSelected(archived: Boolean) {
        val ids = state.value.selectedIds
        selected.value = emptySet()
        viewModelScope.launch { repository.setArchived(ids, archived) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                RoutinesViewModel(
                    container.routineRepository,
                    container.todayClock.today,
                    container.categoryRepository.observeCategories(),
                )
            }
        }
    }
}
