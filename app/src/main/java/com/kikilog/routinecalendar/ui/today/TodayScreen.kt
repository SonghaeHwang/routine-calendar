package com.kikilog.routinecalendar.ui.today

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.core.schedule.TodayEntry
import com.kikilog.routinecalendar.ui.common.AddRoutineButton
import com.kikilog.routinecalendar.ui.common.EmptyState
import com.kikilog.routinecalendar.ui.common.TabScaffold
import com.kikilog.routinecalendar.ui.common.rememberNotificationEnabler
import com.kikilog.routinecalendar.ui.format.notifyTimeLabel
import com.kikilog.routinecalendar.ui.format.formatDate
import com.kikilog.routinecalendar.ui.format.formatShortDate
import com.kikilog.routinecalendar.ui.format.overdueLabel
import kotlinx.datetime.LocalDate

@Composable
fun TodayRoute(onAdd: () -> Unit, onOpen: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    val requestNotify = rememberNotificationEnabler(viewModel::onNotifyPermissionResult)
    TodayScreen(
        state = state,
        onToggle = viewModel::onToggle,
        onOpen = onOpen,
        onAdd = onAdd,
        onOpenSettings = onOpenSettings,
        onEnableNotify = requestNotify,
        onDismissNotifyCard = viewModel::onDismissNotifyCard,
    )
}

@Composable
fun TodayScreen(
    state: TodayUiState,
    onToggle: (TodayEntry) -> Unit,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    onOpenSettings: () -> Unit,
    onEnableNotify: () -> Unit = {},
    onDismissNotifyCard: () -> Unit = {},
) {
    val today = state.today
    TabScaffold(
        // 제목 자리에 오늘 날짜 「11월 3일 (화)」
        title = today?.let { formatDate(it, it) } ?: stringResource(R.string.tab_today),
        onOpenSettings = onOpenSettings,
        floatingActionButton = { AddRoutineButton(onAdd) },
    ) { padding ->
        when {
            state.isLoading || today == null -> Unit
            !state.hasAnyRoutine -> EmptyState(stringResource(R.string.today_no_routines), padding)
            state.board.isEmpty -> EmptyState(stringResource(R.string.empty_today), padding)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    // [+] 단추에 마지막 줄이 가리지 않게
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                if (state.showNotifyCard) {
                    item(key = "notify-card") {
                        NotifyCard(state, onEnable = onEnableNotify, onLater = onDismissNotifyCard)
                    }
                }
                section(R.string.section_overdue, state.board.overdue, today, onToggle, onOpen)
                section(R.string.section_today, state.board.dueToday, today, onToggle, onOpen)
                section(R.string.section_upcoming, state.board.upcoming, today, onToggle, onOpen)
            }
        }
    }
}

/** 루틴을 등록한 뒤 한 번만 보이는 알림 안내. 처음 켜자마자 권한을 묻지 않기 위해 여기서 묻는다 */
@Composable
private fun NotifyCard(state: TodayUiState, onEnable: () -> Unit, onLater: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)) {
            Text(
                stringResource(R.string.notify_card_body, notifyTimeLabel(state.notifyTime)),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(end = 8.dp),
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onLater) { Text(stringResource(R.string.notify_card_later)) }
                TextButton(onClick = onEnable) { Text(stringResource(R.string.notify_card_enable)) }
            }
        }
    }
}

private fun LazyListScope.section(
    titleRes: Int,
    entries: List<TodayEntry>,
    today: LocalDate,
    onToggle: (TodayEntry) -> Unit,
    onOpen: (Long) -> Unit,
) {
    if (entries.isEmpty()) return
    item(key = "header-$titleRes") {
        Text(
            text = stringResource(titleRes),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                .semantics { heading() },
        )
    }
    items(entries, key = { "$titleRes-${it.routine.id}" }) { entry ->
        TodayRow(entry = entry, today = today, onToggle = { onToggle(entry) }, onOpen = { onOpen(entry.routine.id) })
    }
}

/**
 * 줄 전체를 눌러 체크한다(체크 칸만이 아니라). 꼭 누르면 수정 화면. 최소 높이 56dp.
 * 화면 읽기 기능에는 체크박스로 읽히고, 길게 누르기 설명이 붙는다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TodayRow(entry: TodayEntry, today: LocalDate, onToggle: () -> Unit, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .combinedClickable(
                role = Role.Checkbox,
                onClick = onToggle,
                onLongClick = onOpen,
                onLongClickLabel = stringResource(R.string.edit_routine),
            )
            .semantics { toggleableState = ToggleableState(entry.doneToday) }
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Checkbox(checked = entry.doneToday, onCheckedChange = null, modifier = Modifier.padding(8.dp))
        Text(
            text = entry.routine.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (entry.doneToday) TextDecoration.LineThrough else null,
            color = if (entry.doneToday) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        val trailing = when {
            entry.overdueDays > 0 -> overdueLabel(entry.overdueDays)
            entry.due > today -> formatShortDate(entry.due)
            else -> null
        }
        if (trailing != null) {
            Text(
                text = trailing,
                style = MaterialTheme.typography.labelLarge,
                color = if (entry.overdueDays > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(end = 8.dp),
            )
        }
    }
}
