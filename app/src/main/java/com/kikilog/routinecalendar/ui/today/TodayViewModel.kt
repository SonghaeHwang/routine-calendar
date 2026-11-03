package com.kikilog.routinecalendar.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.core.notify.DEFAULT_NOTIFY_TIME
import com.kikilog.routinecalendar.core.notify.NotifyTime
import com.kikilog.routinecalendar.core.schedule.TodayBoard
import com.kikilog.routinecalendar.core.schedule.TodayEntry
import com.kikilog.routinecalendar.core.schedule.buildTodayBoard
import com.kikilog.routinecalendar.data.RoutineRepository
import com.kikilog.routinecalendar.data.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class TodayUiState(
    val isLoading: Boolean = true,
    val today: LocalDate? = null,
    val board: TodayBoard = TodayBoard.EMPTY,
    val hasAnyRoutine: Boolean = false,
    /** 「알림 켤까요?」 카드: 루틴이 있고, 알림이 꺼져 있고, 카드를 닫은 적이 없을 때만 */
    val showNotifyCard: Boolean = false,
    val notifyTime: NotifyTime = DEFAULT_NOTIFY_TIME,
)

/**
 * 오늘 화면. 날짜가 바뀌면([today]) 칸이 저절로 다시 나뉜다.
 * 알림 카드의 권한 요청은 화면(Compose)이 하고, 결과만 여기로 받는다.
 */
class TodayViewModel(
    private val repository: RoutineRepository,
    today: StateFlow<LocalDate>,
    private val settings: SettingsRepository,
    private val syncNotify: suspend () -> Unit,
) : ViewModel() {

    val state: StateFlow<TodayUiState> =
        combine(
            repository.observeRoutines(),
            today,
            settings.notifyEnabled,
            settings.notifyCardDismissed,
            settings.notifyTime,
        ) { items, date, notifyEnabled, cardDismissed, notifyTime ->
            TodayUiState(
                isLoading = false,
                today = date,
                board = buildTodayBoard(items.map { it.routine to it.completions }, date),
                hasAnyRoutine = items.isNotEmpty(),
                showNotifyCard = items.isNotEmpty() && !notifyEnabled && !cardDismissed,
                notifyTime = notifyTime,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    /** 카드의 「켜기」 결과. 허용하면 켜고 예약, 거절해도 카드는 닫는다(계속 조르지 않는다) */
    fun onNotifyPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            if (granted) {
                settings.setNotifyEnabled(true)
                syncNotify()
            }
            settings.dismissNotifyCard()
        }
    }

    /** 카드의 「나중에」. 다시 보이지 않는다 — 설정에서 켤 수 있다 */
    fun onDismissNotifyCard() {
        viewModelScope.launch { settings.dismissNotifyCard() }
    }

    /**
     * 체크 · 취소. 날짜는 화면에 보이는 오늘을 쓴다 — 자정 직전에 누른 체크가 다음 날로 저장되지 않게.
     */
    fun onToggle(entry: TodayEntry) {
        val date = state.value.today ?: return
        viewModelScope.launch {
            runCatching {
                if (entry.doneToday) {
                    repository.uncheck(entry.routine.id, date)
                } else {
                    repository.check(entry.routine.id, date)
                }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp).container
                TodayViewModel(
                    container.routineRepository,
                    container.todayClock.today,
                    container.settingsRepository,
                    container.notifyScheduler::sync,
                )
            }
        }
    }
}
