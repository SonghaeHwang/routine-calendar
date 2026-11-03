package com.kikilog.routinecalendar.ui.presets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.DEFAULT_CATEGORIES
import com.kikilog.routinecalendar.core.preset.Preset
import com.kikilog.routinecalendar.data.PresetRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PresetsUiState(
    val isLoading: Boolean = true,
    val presets: List<Preset> = emptyList(),
    /** 분류 순서 · 이름 · 색. 빈 분류도 머리글로 보여 끌어 놓을 자리가 된다 */
    val categories: List<Category> = emptyList(),
)

/** 설정 → 빠른 선택 편집 */
class PresetsViewModel(
    private val repository: PresetRepository,
    categories: Flow<List<Category>> = flowOf(DEFAULT_CATEGORIES),
) : ViewModel() {

    val state: StateFlow<PresetsUiState> =
        combine(repository.observePresets(), categories) { presets, cats ->
            PresetsUiState(isLoading = false, presets = presets, categories = cats)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PresetsUiState())

    /** 검사를 통과하면 저장하고 true. 새 항목이면 끝에 더하고, 아니면 고친다 */
    fun save(form: PresetForm): Boolean {
        val preset = form.toPresetOrNull() ?: return false
        viewModelScope.launch {
            runCatching { if (form.id == null) repository.add(preset) else repository.update(preset) }
        }
        return true
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** 꾹 눌러 끌어 다른 분류에 놓았을 때 */
    fun move(id: Long, categoryId: Long) {
        viewModelScope.launch { repository.move(id, categoryId) }
    }

    fun resetToDefaults() {
        viewModelScope.launch { repository.resetToDefaults() }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                PresetsViewModel(container.presetRepository, container.categoryRepository.observeCategories())
            }
        }
    }
}
