package com.kikilog.routinecalendar.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.edit.EditRoutineRoute
import com.kikilog.routinecalendar.ui.format.formatDate
import kotlinx.coroutines.launch
import com.kikilog.routinecalendar.ui.routines.RoutinesRoute
import com.kikilog.routinecalendar.ui.settings.SettingsScreen
import com.kikilog.routinecalendar.ui.today.TodayRoute

/** 앱의 뼈대: 하단 탭 + 화면 이동. 등록/수정 · 설정 화면에서는 하단 탭을 숨긴다. */
@Composable
fun RoutineCalendarRoot() {
    val backStack = rememberNavBackStack(TodayKey)
    val currentTab = TopLevelTab.entries.firstOrNull { it.key == backStack.lastOrNull() }

    val openSettings: () -> Unit = { backStack.add(SettingsKey) }
    val openEdit: (Long?) -> Unit = { routineId -> backStack.add(EditKey(routineId)) }
    val goBack: () -> Unit = { backStack.removeLastOrNull() }

    // 등록 후 안내 문구. 등록 화면이 닫힌 뒤 아래 화면 위에 띄운다
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val savedTemplate = stringResource(R.string.saved_message)
    val editedMessage = stringResource(R.string.edited_message)

    Scaffold(
        // 시스템 바 여백은 하단 탭과 각 화면의 상단 바가 처리한다
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                entry<TodayKey> { TodayRoute(onAdd = { openEdit(null) }, onOpenSettings = openSettings) }
                entry<CalendarKey> { CalendarPlaceholder(onOpenSettings = openSettings) }
                entry<RoutinesKey> {
                    RoutinesRoute(onAdd = { openEdit(null) }, onOpen = { id -> openEdit(id) }, onOpenSettings = openSettings)
                }
                entry<EditKey> { key ->
                    EditRoutineRoute(
                        routineId = key.routineId,
                        onBack = goBack,
                        onAdded = { title, nextDue, today ->
                            goBack()
                            val message = savedTemplate.format(title, formatDate(nextDue, today))
                            scope.launch { snackbarHostState.showSnackbar(message) }
                        },
                        onEdited = {
                            goBack()
                            scope.launch { snackbarHostState.showSnackbar(editedMessage) }
                        },
                    )
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
