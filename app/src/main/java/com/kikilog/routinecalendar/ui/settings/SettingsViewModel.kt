package com.kikilog.routinecalendar.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.schedule.DEFAULT_WEEK_START
import com.kikilog.routinecalendar.data.DEFAULT_START_SCREEN
import com.kikilog.routinecalendar.data.SettingsRepository
import com.kikilog.routinecalendar.data.StartScreen
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

/** 주 시작 요일로 고를 수 있는 값. 한국 달력에서 쓰는 두 가지만 둔다 */
val WEEK_START_OPTIONS: List<DayOfWeek> = listOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)

data class SettingsUiState(
    val isLoading: Boolean = true,
    val weekStart: DayOfWeek = DEFAULT_WEEK_START,
    val startScreen: StartScreen = DEFAULT_START_SCREEN,
)

/** 설정 화면. 주 시작 요일은 캘린더에 바로, 첫 화면은 다음에 앱을 켤 때 적용된다 */
class SettingsViewModel(private val repository: SettingsRepository) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(repository.weekStart, repository.startScreen) { weekStart, startScreen ->
            SettingsUiState(isLoading = false, weekStart = weekStart, startScreen = startScreen)
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setWeekStart(day: DayOfWeek) {
        viewModelScope.launch { repository.setWeekStart(day) }
    }

    fun setStartScreen(screen: StartScreen) {
        viewModelScope.launch { repository.setStartScreen(screen) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                SettingsViewModel(container.settingsRepository)
            }
        }
    }
}
