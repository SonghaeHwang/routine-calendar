package com.kikilog.routinecalendar.ui.info

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.DEFAULT_CATEGORIES
import com.kikilog.routinecalendar.core.model.Routine
import com.kikilog.routinecalendar.core.schedule.lastDoneOn
import com.kikilog.routinecalendar.core.schedule.nextDue
import com.kikilog.routinecalendar.core.schedule.nextDueDates
import com.kikilog.routinecalendar.core.schedule.overdueDays
import com.kikilog.routinecalendar.data.RoutineRepository
import com.kikilog.routinecalendar.ui.format.epochMillisToLocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.datetime.LocalDate

data class RoutineInfoUiState(
    val isLoading: Boolean = true,
    /** 지워졌으면 true — 화면을 닫는다 */
    val notFound: Boolean = false,
    val today: LocalDate? = null,
    val routine: Routine? = null,
    val category: Category? = null,
    val createdOn: LocalDate? = null,
    /** null 이면 끝남(반복 없음 + 완료) */
    val nextDue: LocalDate? = null,
    val lastDoneOn: LocalDate? = null,
    /** 다음 예정일이 지났으면 그 날수, 아니면 0 */
    val overdueDays: Int = 0,
    /** 앞으로의 예정일(다음 예정일 포함 최대 3개) */
    val upcoming: List<LocalDate> = emptyList(),
)

/** 루틴 정보(읽기 전용). 고치려면 아래 「편집하기」 */
class RoutineInfoViewModel(
    repository: RoutineRepository,
    routineId: Long,
    today: StateFlow<LocalDate>,
    categories: Flow<List<Category>> = flowOf(DEFAULT_CATEGORIES),
) : ViewModel() {

    val state: StateFlow<RoutineInfoUiState> =
        combine(repository.observeRoutine(routineId), today, categories) { loaded, date, cats ->
            if (loaded == null) return@combine RoutineInfoUiState(isLoading = false, notFound = true)
            val (routine, completions, createdAt) = loaded
            val next = nextDue(routine, completions)
            RoutineInfoUiState(
                isLoading = false,
                today = date,
                routine = routine,
                category = cats.firstOrNull { it.id == routine.categoryId },
                createdOn = epochMillisToLocalDate(createdAt),
                nextDue = next,
                lastDoneOn = lastDoneOn(routine, completions),
                overdueDays = next?.let { overdueDays(it, date) } ?: 0,
                upcoming = nextDueDates(routine, completions, count = 3),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RoutineInfoUiState())

    companion object {
        fun factory(routineId: Long): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                RoutineInfoViewModel(
                    container.routineRepository,
                    routineId,
                    container.todayClock.today,
                    container.categoryRepository.observeCategories(),
                )
            }
        }
    }
}
