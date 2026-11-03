package com.kikilog.routinecalendar.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.DEFAULT_PRESETS
import com.kikilog.routinecalendar.core.preset.Preset
import com.kikilog.routinecalendar.core.schedule.initialSchedule
import com.kikilog.routinecalendar.data.RoutineDraft
import com.kikilog.routinecalendar.data.RoutineRepository
import com.kikilog.routinecalendar.ui.format.epochMillisToLocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * 루틴 등록 · 수정 화면.
 * @param routineId null 이면 등록, 아니면 그 루틴을 불러와 수정한다
 */
class EditRoutineViewModel(
    private val repository: RoutineRepository,
    today: () -> LocalDate,
    routineId: Long? = null,
    presets: Flow<List<Preset>> = flowOf(DEFAULT_PRESETS),
) : ViewModel() {

    private val _state = MutableStateFlow(
        EditUiState(today = today(), routineId = routineId, isLoading = routineId != null),
    )
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        if (routineId != null) load(routineId)
        // 빠른 선택은 등록할 때만 쓴다. 설정에서 바꾸면 바로 반영된다
        if (routineId == null) {
            viewModelScope.launch { presets.collect { list -> _state.update { it.copy(presets = list) } } }
        }
    }

    private fun load(id: Long) {
        viewModelScope.launch {
            val loaded = repository.observeRoutine(id).first()
            if (loaded == null) {
                _state.update { it.copy(isLoading = false, notFound = true) }
                return@launch
            }
            val (routine, completions, createdAt) = loaded
            val hasCompletions = completions.isNotEmpty()
            _state.update {
                it.copy(
                    isLoading = false,
                    title = routine.title,
                    category = routine.category,
                    amountText = routine.interval.amount.toString(),
                    unit = routine.interval.unit,
                    repeats = routine.repeats,
                    notify = routine.notify,
                    hasCompletions = hasCompletions,
                    repeatsLocked = hasCompletions && routine.repeats,
                    firstDue = routine.firstDue,
                    archived = routine.archived,
                    createdOn = epochMillisToLocalDate(createdAt),
                )
            }
        }
    }

    fun onTitleChange(title: String) = _state.update { it.copy(title = title.take(MAX_TITLE_LENGTH)) }

    fun onCategoryChange(category: Category) = _state.update { it.copy(category = category) }

    /** 숫자만 받고, 세 자리까지 */
    fun onAmountChange(text: String) = _state.update { it.copy(amountText = text.filter(Char::isDigit).take(3)) }

    fun onUnitChange(unit: IntervalUnit) = _state.update { it.copy(unit = unit) }

    /** 잠긴 경우(기록이 있는 반복 루틴)는 무시한다 */
    fun onRepeatsChange(repeats: Boolean) = _state.update { if (it.repeatsLocked) it else it.copy(repeats = repeats) }

    fun onNotifyChange(notify: Boolean) = _state.update { it.copy(notify = notify) }

    /** 오늘 이후 날짜는 무시한다(날짜 선택기에서도 막지만 한 번 더) */
    fun onLastDoneChange(date: LocalDate?) = _state.update {
        if (date != null && date > it.today) it else it.copy(lastDoneOn = date)
    }

    /** 수정 모드에서 기록이 없을 때만. 과거 · 미래 모두 고를 수 있다 */
    fun onFirstDueChange(date: LocalDate) = _state.update { if (it.hasCompletions) it else it.copy(firstDue = date) }

    /** 프리셋을 고르면 칸을 채운다. 주기가 없는 프리셋(리들샷)은 숫자 칸을 비워 직접 입력하게 한다. */
    fun applyPreset(preset: Preset) = _state.update {
        it.copy(
            title = preset.title,
            category = preset.category,
            amountText = preset.interval?.amount?.toString() ?: "",
            unit = preset.interval?.unit ?: it.unit,
            repeats = preset.repeats,
        )
    }

    fun onSaveFailedShown() = _state.update { it.copy(saveFailed = false) }

    /** 수정 모드 더보기: 보관 · 보관 해제. 끝나면 화면을 닫는다 */
    fun onToggleArchive() {
        val id = _state.value.routineId ?: return
        val archive = !_state.value.archived
        viewModelScope.launch {
            repository.setArchived(id, archive)
            _state.update { it.copy(archived = archive, closed = true) }
        }
    }

    /** 수정 모드 더보기: 삭제(확인 대화상자를 거친 뒤). 끝나면 화면을 닫는다 */
    fun onDelete() {
        val id = _state.value.routineId ?: return
        viewModelScope.launch {
            repository.delete(id)
            _state.update { it.copy(closed = true) }
        }
    }

    fun save() {
        val current = _state.value
        if (current.isSaving || current.saved || current.isLoading) return
        val interval = current.interval
        if (!current.isValid || interval == null) {
            _state.update { it.copy(showErrors = true) }
            return
        }
        _state.update { it.copy(showErrors = true, isSaving = true) }
        viewModelScope.launch {
            val draft = RoutineDraft(
                title = current.title,
                category = current.category,
                interval = interval,
                repeats = current.repeats,
                notify = current.notify,
            )
            val id = current.routineId
            runCatching {
                if (id == null) {
                    repository.add(draft, current.lastDoneOn)
                    // 등록 직후에는 다음 예정일 = 첫 예정일
                    initialSchedule(interval, current.repeats, current.today, current.lastDoneOn).firstDue
                } else {
                    repository.update(id, draft, firstDue = current.firstDue.takeIf { !current.hasCompletions })
                    null
                }
            }
                .onSuccess { nextDue -> _state.update { it.copy(isSaving = false, saved = true, savedNextDue = nextDue) } }
                .onFailure { _state.update { it.copy(isSaving = false, saveFailed = true) } }
        }
    }

    companion object {
        const val MAX_TITLE_LENGTH = 30

        fun factory(routineId: Long?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                EditRoutineViewModel(
                    repository = container.routineRepository,
                    today = container.todayClock::current,
                    routineId = routineId,
                    presets = container.presetRepository.observePresets(),
                )
            }
        }
    }
}
