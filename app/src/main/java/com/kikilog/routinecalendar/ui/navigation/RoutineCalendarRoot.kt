package com.kikilog.routinecalendar.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.kikilog.routinecalendar.ui.calendar.CalendarPlaceholder
import com.kikilog.routinecalendar.ui.edit.EditRoutinePlaceholder
import com.kikilog.routinecalendar.ui.edit.EditRoutineRoute
import com.kikilog.routinecalendar.ui.routines.RoutinesRoute
import com.kikilog.routinecalendar.ui.settings.SettingsScreen
import com.kikilog.routinecalendar.ui.today.TodayPlaceholder

/** 앱의 뼈대: 하단 탭 + 화면 이동. 등록/수정 · 설정 화면에서는 하단 탭을 숨긴다. */
@Composable
fun RoutineCalendarRoot() {
    val backStack = rememberNavBackStack(TodayKey)
    val currentTab = TopLevelTab.entries.firstOrNull { it.key == backStack.lastOrNull() }

    val openSettings: () -> Unit = { backStack.add(SettingsKey) }
    val openEdit: (Long?) -> Unit = { routineId -> backStack.add(EditKey(routineId)) }
    val goBack: () -> Unit = { backStack.removeLastOrNull() }

    Scaffold(
        // 시스템 바 여백은 하단 탭과 각 화면의 상단 바가 처리한다
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null) {
                NavigationBar {
                    TopLevelTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { backStack.selectTab(tab) },
                            icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                            label = { Text(stringResource(tab.label)) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavDisplay(
            backStack = backStack,
            onBack = { goBack() },
            modifier = Modifier
                .padding(padding)
                .consumeWindowInsets(padding),
            entryDecorators = listOf(
                rememberSaveableStateHolderNavEntryDecorator(),
                // 화면이 닫히면 그 화면의 ViewModel 도 정리된다
                rememberViewModelStoreNavEntryDecorator(),
            ),
            entryProvider = entryProvider {
                entry<TodayKey> { TodayPlaceholder(onAdd = { openEdit(null) }, onOpenSettings = openSettings) }
                entry<CalendarKey> { CalendarPlaceholder(onOpenSettings = openSettings) }
                entry<RoutinesKey> {
                    RoutinesRoute(onAdd = { openEdit(null) }, onOpen = { id -> openEdit(id) }, onOpenSettings = openSettings)
                }
                entry<EditKey> { key ->
                    if (key.routineId == null) {
                        EditRoutineRoute(onDone = goBack)
                    } else {
                        // 수정 화면은 12번 작업에서
                        EditRoutinePlaceholder(onBack = goBack)
                    }
                }
                entry<SettingsKey> { SettingsScreen(onBack = { goBack() }) }
            },
        )
    }
}

private fun NavBackStack<NavKey>.selectTab(tab: TopLevelTab) {
    val target = backStackFor(tab)
    if (toList() == target) return
    clear()
    addAll(target)
}
