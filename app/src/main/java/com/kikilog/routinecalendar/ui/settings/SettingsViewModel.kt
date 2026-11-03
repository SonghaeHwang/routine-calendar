package com.kikilog.routinecalendar.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.notify.DEFAULT_NOTIFY_TIME
import com.kikilog.routinecalendar.core.notify.NotifyTime
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

/** 첫 화면으로 고를 수 있는 탭. 오늘 탭을 숨긴 동안은 달력 · 루틴만 */
val START_SCREEN_OPTIONS: List<StartScreen> = listOf(StartScreen.CALENDAR, StartScreen.ROUTINES)

data class SettingsUiState(
    val isLoading: Boolean = true,
    val weekStart: DayOfWeek = DEFAULT_WEEK_START,
    val startScreen: StartScreen = DEFAULT_START_SCREEN,
    val notifyEnabled: Boolean = false,
    val notifyTime: NotifyTime = DEFAULT_NOTIFY_TIME,
)

/**
 * 설정 화면. 주 시작 요일은 캘린더에 바로, 첫 화면은 다음에 앱을 켤 때 적용된다.
 * 알림을 켜고 끄거나 시각을 바꾸면 [syncNotify] 로 예약을 다시 맞춘다. 권한 요청은 화면(Compose)이 한다.
 */
class SettingsViewModel(
    private val repository: SettingsRepository,
    private val syncNotify: suspend () -> Unit,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> =
        combine(
            repository.weekStart,
            repository.startScreen,
            repository.notifyEnabled,
            repository.notifyTime,
        ) { weekStart, startScreen, notifyEnabled, notifyTime ->
            SettingsUiState(
                isLoading = false,
                weekStart = weekStart,
                startScreen = startScreen,
                notifyEnabled = notifyEnabled,
                notifyTime = notifyTime,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setWeekStart(day: DayOfWeek) {
        viewModelScope.launch { repository.setWeekStart(day) }
    }

    fun setStartScreen(screen: StartScreen) {
        viewModelScope.launch { repository.setStartScreen(screen) }
    }

    /** 켤 때는 화면이 권한을 받은 뒤에만 부른다 */
    fun setNotifyEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.setNotifyEnabled(enabled)
            // 설정에서 켰으면 오늘 화면 카드는 더 필요 없다
            if (enabled) repository.dismissNotifyCard()
            syncNotify()
        }
    }

    fun setNotifyTime(time: NotifyTime) {
        viewModelScope.launch {
            repository.setNotifyTime(time)
            syncNotify()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                SettingsViewModel(container.settingsRepository, container.notifyScheduler::sync)
            }
        }
    }
}
