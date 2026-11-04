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
import androidx.compose.runtime.LaunchedEffect
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
import com.kikilog.routinecalendar.ui.calendar.CalendarRoute
import com.kikilog.routinecalendar.ui.categories.CategoriesRoute
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.edit.EditRoutineRoute
import com.kikilog.routinecalendar.ui.info.RoutineInfoRoute
import com.kikilog.routinecalendar.ui.format.formatDate
import com.kikilog.routinecalendar.ui.presets.PresetsRoute
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import com.kikilog.routinecalendar.ui.routines.RoutinesRoute
import com.kikilog.routinecalendar.ui.settings.NotifyRoutinesRoute
import com.kikilog.routinecalendar.ui.settings.SettingsScreen
import com.kikilog.routinecalendar.ui.today.TodayRoute

/**
 * 앱의 뼈대: 하단 탭 + 화면 이동. 등록/수정 · 설정 화면에서는 하단 탭을 숨긴다.
 * [openTab] 이 오면(알림을 눌러 열었을 때) 그 탭으로 바꾼 뒤 [onOpenTabHandled] 를 부른다.
 */
@Composable
fun RoutineCalendarRoot(
    startTab: TopLevelTab = DEFAULT_START_TAB,
    openTab: TopLevelTab? = null,
    onOpenTabHandled: () -> Unit = {},
) {
    val backStack = rememberNavBackStack(startTab.key)
    LaunchedEffect(openTab) {
        if (openTab != null) {
            backStack.selectTab(openTab, startTab)
            onOpenTabHandled()
        }
    }
    val currentTab = TopLevelTab.entries.firstOrNull { it.key == backStack.lastOrNull() }

    val openSettings: () -> Unit = { backStack.add(SettingsKey) }
    val openEdit: (Long?) -> Unit = { routineId -> backStack.add(EditKey(routineId)) }
    // 달력에서 고른 날에 추가하면 그날이 루틴 시작일
    val openAddOn: (LocalDate?) -> Unit = { date -> backStack.add(EditKey(startDate = date?.toString())) }
    // 목록에서 루틴을 열면 정보 화면부터. 고치려면 정보 화면의 「편집하기」
    val openInfo: (Long) -> Unit = { routineId -> backStack.add(InfoKey(routineId)) }
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
                    VISIBLE_TABS.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == currentTab,
                            onClick = { backStack.selectTab(tab, startTab) },
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
                entry<TodayKey> {
                    TodayRoute(onAdd = { openEdit(null) }, onOpen = openInfo, onOpenSettings = openSettings)
                }
                entry<CalendarKey> {
                    CalendarRoute(onAdd = openAddOn, onOpen = openInfo, onOpenSettings = openSettings)
                }
                entry<RoutinesKey> {
                    RoutinesRoute(onAdd = { openEdit(null) }, onOpen = openInfo, onOpenSettings = openSettings)
                }
                entry<InfoKey> { key ->
                    RoutineInfoRoute(routineId = key.routineId, onBack = goBack, onEdit = { id -> openEdit(id) })
                }
                entry<EditKey> { key ->
                    EditRoutineRoute(
                        routineId = key.routineId,
                        startDate = key.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() },
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
                        // 등록 화면을 닫고 같은 이름의 기존 루틴 수정 화면으로 바꾼다
                        onOpenExisting = { id ->
                            goBack()
                            openEdit(id)
                        },
                        onOpenCategories = { backStack.add(CategoriesKey) },
                    )
                }
                entry<SettingsKey> {
                    SettingsScreen(
                        onBack = goBack,
                        onOpenPresets = { backStack.add(PresetsKey) },
                        onOpenCategories = { backStack.add(CategoriesKey) },
                        onOpenNotifyRoutines = { backStack.add(NotifyRoutinesKey) },
                    )
                }
                entry<NotifyRoutinesKey> { NotifyRoutinesRoute(onBack = goBack) }
                entry<PresetsKey> { PresetsRoute(onBack = goBack) }
                entry<CategoriesKey> { CategoriesRoute(onBack = goBack) }
            },
        )
    }
}

private fun NavBackStack<NavKey>.selectTab(tab: TopLevelTab, startTab: TopLevelTab) {
    val target = backStackFor(tab, startTab)
    if (toList() == target) return
    clear()
    addAll(target)
}
