package com.kikilog.routinecalendar.ui.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.data.CategoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** 지우기 확인 창. [itemCount] 가 0 보다 크면 「기타로 이동돼요」 경고를 보인다 */
data class PendingDelete(val category: Category, val itemCount: Int)

data class CategoriesUiState(
    val isLoading: Boolean = true,
    val categories: List<Category> = emptyList(),
    val pendingDelete: PendingDelete? = null,
)

/** 분류 편집: 추가 · 이름 · 색 변경 · 삭제(든 항목은 기타로) */
class CategoriesViewModel(private val repository: CategoryRepository) : ViewModel() {

    private val pendingDelete = MutableStateFlow<PendingDelete?>(null)

    val state: StateFlow<CategoriesUiState> =
        combine(repository.observeCategories(), pendingDelete) { categories, pending ->
            CategoriesUiState(isLoading = false, categories = categories, pendingDelete = pending)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CategoriesUiState())

    /**
     * [id] 가 null 이면 새 분류(색은 [colorArgb], null 이면 자동), 아니면 이름 · 색을 고친다.
     * 이름이 비었거나 너무 길면 저장하지 않고 false.
     */
    fun save(id: Long?, name: String, colorArgb: Int?): Boolean {
        val trimmed = name.trim()
        if (trimmed.isEmpty() || trimmed.length > CategoryRepository.MAX_NAME_LENGTH) return false
        viewModelScope.launch {
            runCatching {
                if (id == null) repository.add(trimmed, colorArgb) else repository.update(id, trimmed, colorArgb ?: return@runCatching)
            }
        }
        return true
    }

    /** 삭제를 누르면 든 항목 수를 세어 확인 창을 띄운다. 「기타」는 무시한다 */
    fun requestDelete(category: Category) {
        if (!category.removable) return
        viewModelScope.launch {
            pendingDelete.value = PendingDelete(category, repository.itemCount(category.id))
        }
    }

    fun confirmDelete() {
        val target = pendingDelete.value ?: return
        pendingDelete.value = null
        viewModelScope.launch { repository.delete(target.category.id) }
    }

    fun dismissDelete() {
        pendingDelete.value = null
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                CategoriesViewModel(container.categoryRepository)
            }
        }
    }
}
