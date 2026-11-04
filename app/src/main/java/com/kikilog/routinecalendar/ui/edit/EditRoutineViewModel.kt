package com.kikilog.routinecalendar.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.DEFAULT_CATEGORIES
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.DEFAULT_PRESETS
import com.kikilog.routinecalendar.core.preset.Preset
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
 * @param startDate 등록할 때 루틴 시작일의 처음 값(달력에서 고른 날). null 이면 오늘
 */
class EditRoutineViewModel(
    private val repository: RoutineRepository,
    today: () -> LocalDate,
    routineId: Long? = null,
    presets: Flow<List<Preset>> = flowOf(DEFAULT_PRESETS),
    categories: Flow<List<Category>> = flowOf(DEFAULT_CATEGORIES),
    startDate: LocalDate? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(
        today().let { date ->
            EditUiState(today = date, routineId = routineId, isLoading = routineId != null, startDate = startDate ?: date)
        },
    )
    val state: StateFlow<EditUiState> = _state.asStateFlow()

    init {
        if (routineId != null) load(routineId)
        // 빠른 선택은 등록할 때만 쓴다. 설정에서 바꾸면 바로 반영된다
        if (routineId == null) {
            viewModelScope.launch { presets.collect { list -> _state.update { it.copy(presets = list) } } }
        }
        // 분류를 지우면 고른 분류가 사라질 수 있다 → 저장소가 항목을 기타로 옮기므로 화면도 기타로
        viewModelScope.launch {
            categories.collect { list ->
                _state.update { s ->
                    val id = if (list.any { it.id == s.categoryId }) s.categoryId else Category.ETC
                    s.copy(categories = list, categoryId = id)
                }
            }
        }
        // 같은 이름 안내용: 보관한 것까지 포함한 다른 루틴 이름
        viewModelScope.launch {
            repository.observeRoutines(includeArchived = true).collect { list ->
                val others = list.map { it.routine }
                    .filter { it.id != routineId }
                    .map { NamedRoutine(it.id, it.title, it.archived) }
                _state.update { it.copy(others = others) }
            }
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
                    categoryId = routine.categoryId,
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

    fun onTitleChange(title: String) = _state.update {
        it.copy(title = title.take(MAX_TITLE_LENGTH), duplicateConfirmed = false)
    }

    fun onCategoryChange(categoryId: Long) = _state.update { it.copy(categoryId = categoryId) }

    /** 숫자만 받고, 세 자리까지 */
    fun onAmountChange(text: String) = _state.update { it.copy(amountText = text.filter(Char::isDigit).take(3)) }

    fun onUnitChange(unit: IntervalUnit) = _state.update { it.copy(unit = unit) }

    /** 잠긴 경우(기록이 있는 반복 루틴)는 무시한다 */
    fun onRepeatsChange(repeats: Boolean) = _state.update { if (it.repeatsLocked) it else it.copy(repeats = repeats) }

    fun onNotifyChange(notify: Boolean) = _state.update { it.copy(notify = notify) }

    /** 등록 화면의 루틴 시작일. 지난 날 · 오늘 · 앞날 모두 고를 수 있다 */
    fun onStartDateChange(date: LocalDate) = _state.update { it.copy(startDate = date) }

    /** 수정 모드에서 기록이 없을 때만. 과거 · 미래 모두 고를 수 있다 */
    fun onFirstDueChange(date: LocalDate) = _state.update { if (it.hasCompletions) it else it.copy(firstDue = date) }

    /** 프리셋을 고르면 칸을 채운다. 주기가 없는 프리셋(리들샷)은 숫자 칸을 비워 직접 입력하게 한다. */
    fun applyPreset(preset: Preset) = _state.update {
        it.copy(
            title = preset.title,
            categoryId = preset.categoryId,
            amountText = preset.interval?.amount?.toString() ?: "",
            unit = preset.interval?.unit ?: it.unit,
            repeats = preset.repeats,
        )
    }

    fun onSaveFailedShown() = _state.update { it.copy(saveFailed = false) }

    /** 같은 이름 확인 창: 그래도 추가 */
    fun onAddAnyway() {
        _state.update { it.copy(askDuplicate = false, duplicateConfirmed = true) }
        save()
    }

    /** 같은 이름 확인 창: 기존 루틴 열기(입력하던 내용은 버린다) */
    fun onOpenExisting() = _state.update {
        it.copy(askDuplicate = false, openExistingId = it.duplicate?.id)
    }

    fun onDismissDuplicate() = _state.update { it.copy(askDuplicate = false) }

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
        // 등록할 때만 같은 이름을 묻는다. 수정 중에는 입력한 내용을 잃지 않도록 안내만 한다
        if (!current.isEdit && current.duplicate != null && !current.duplicateConfirmed) {
            _state.update { it.copy(showErrors = true, askDuplicate = true) }
            return
        }
        _state.update { it.copy(showErrors = true, isSaving = true) }
        viewModelScope.launch {
            val draft = RoutineDraft(
                title = current.title,
                categoryId = current.categoryId,
                interval = interval,
                repeats = current.repeats,
                notify = current.notify,
            )
            val id = current.routineId
            runCatching {
                if (id == null) {
                    // 등록 직후에는 다음 예정일 = 첫 예정일
                    repository.addStartingOn(draft, current.startDate).second
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

        fun factory(routineId: Long?, startDate: LocalDate? = null): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                EditRoutineViewModel(
                    repository = container.routineRepository,
                    today = container.todayClock::current,
                    routineId = routineId,
                    presets = container.presetRepository.observePresets(),
                    categories = container.categoryRepository.observeCategories(),
                    startDate = startDate,
                )
            }
        }
    }
}
