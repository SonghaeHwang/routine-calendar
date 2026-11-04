package com.kikilog.routinecalendar.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.DEFAULT_CATEGORIES
import com.kikilog.routinecalendar.core.model.Routine
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 알림에 넣을 루틴 한 줄 */
data class NotifyRoutineRow(val routine: Routine, val category: Category?)

data class NotifyRoutinesUiState(
    val isLoading: Boolean = true,
    /** 분류 순서 → 이름 순. 보관한 루틴은 알림에 나오지 않으므로 뺀다 */
    val rows: List<NotifyRoutineRow> = emptyList(),
) {
    val allOn: Boolean get() = rows.isNotEmpty() && rows.all { it.routine.notify }
}

/** 설정 → 알림에 넣을 루틴. 루틴마다 켜고 끄거나 한꺼번에 바꾼다 */
class NotifyRoutinesViewModel(
    private val repository: RoutineRepository,
    categories: Flow<List<Category>> = flowOf(DEFAULT_CATEGORIES),
) : ViewModel() {

    val state: StateFlow<NotifyRoutinesUiState> =
        combine(repository.observeRoutines(), categories) { list, cats ->
            val order = cats.withIndex().associate { (index, c) -> c.id to index }
            val rows = list.map { it.routine }
                .sortedWith(compareBy<Routine>({ order[it.categoryId] ?: Int.MAX_VALUE }, { it.title }))
                .map { r -> NotifyRoutineRow(r, cats.firstOrNull { it.id == r.categoryId }) }
            NotifyRoutinesUiState(isLoading = false, rows = rows)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NotifyRoutinesUiState())

    fun setNotify(id: Long, notify: Boolean) {
        viewModelScope.launch { repository.setNotify(listOf(id), notify) }
    }

    /** 보이는 루틴 전체를 켜거나 끈다 */
    fun setAll(notify: Boolean) {
        val ids = state.value.rows.map { it.routine.id }
        viewModelScope.launch { repository.setNotify(ids, notify) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                NotifyRoutinesViewModel(container.routineRepository, container.categoryRepository.observeCategories())
            }
        }
    }
}
