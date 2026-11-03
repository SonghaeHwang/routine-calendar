package com.kikilog.routinecalendar.ui.routines

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.common.AddRoutineButton
import com.kikilog.routinecalendar.ui.common.EmptyState
import com.kikilog.routinecalendar.ui.common.TabScaffold
import com.kikilog.routinecalendar.ui.format.categoryLabel
import com.kikilog.routinecalendar.ui.format.formatDate
import com.kikilog.routinecalendar.ui.format.intervalLabel
import kotlinx.datetime.LocalDate

@Composable
fun RoutinesRoute(onAdd: () -> Unit, onOpen: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val viewModel: RoutinesViewModel = viewModel(factory = RoutinesViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    RoutinesScreen(
        state = state,
        onAdd = onAdd,
        onOpen = onOpen,
        onOpenSettings = onOpenSettings,
        onToggleShowArchived = viewModel::onToggleShowArchived,
        onStartSelection = viewModel::onStartSelection,
        onToggleSelection = viewModel::onToggleSelection,
        onClearSelection = viewModel::onClearSelection,
        onDeleteSelected = viewModel::onDeleteSelected,
        onDeleteAll = viewModel::onDeleteAll,
        onArchiveSelected = viewModel::onArchiveSelected,
    )
}

/** 삭제 확인 창 종류 */
private enum class PendingDelete { SELECTED, ALL }

@Composable
fun RoutinesScreen(
    state: RoutinesUiState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleShowArchived: () -> Unit,
    onStartSelection: (Long) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onDeleteAll: () -> Unit,
    onArchiveSelected: (Boolean) -> Unit,
) {
    var pendingDelete by rememberSaveable { mutableStateOf<PendingDelete?>(null) }
    var topMenuOpen by rememberSaveable { mutableStateOf(false) }

    // 선택 모드에서 뒤로 가기는 선택만 끝낸다
    BackHandler(enabled = state.isSelecting) { onClearSelection() }

    TabScaffold(
        title = if (state.isSelecting) {
            stringResource(R.string.selection_title, state.selectedIds.size)
        } else {
            stringResource(R.string.tab_routines)
        },
        onOpenSettings = onOpenSettings,
        showSettings = !state.isSelecting,
        floatingActionButton = { if (!state.isSelecting) AddRoutineButton(onAdd) },
        navigationIcon = {
            if (state.isSelecting) {
                IconButton(onClick = onClearSelection) {
                    Icon(painterResource(R.drawable.ic_back), contentDescription = stringResource(R.string.close_selection))
                }
            }
        },
        actions = {
            if (state.isSelecting) {
                TextButton(onClick = { pendingDelete = PendingDelete.SELECTED }) {
                    Text(stringResource(R.string.action_delete_selected), color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { pendingDelete = PendingDelete.ALL }) {
                    Text(stringResource(R.string.action_delete_all), color = MaterialTheme.colorScheme.error)
                }
            }
            Box {
                IconButton(onClick = { topMenuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = topMenuOpen, onDismissRequest = { topMenuOpen = false }) {
                    if (state.isSelecting) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_archive)) },
                            onClick = {
                                topMenuOpen = false
                                onArchiveSelected(true)
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.action_unarchive)) },
                            onClick = {
                                topMenuOpen = false
                                onArchiveSelected(false)
                            },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text(stringResource(if (state.showArchived) R.string.hide_archived else R.string.show_archived)) },
                            onClick = {
                                topMenuOpen = false
                                onToggleShowArchived()
                            },
                        )
                    }
                }
            }
        },
    ) { padding ->
        val today = state.today
        when {
            state.isLoading || today == null -> Unit
            state.rows.isEmpty() -> EmptyState(stringResource(R.string.empty_routines), padding)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = padding.calculateTopPadding(),
                    // [+] 단추에 마지막 줄이 가리지 않게
                    bottom = padding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                items(state.rows, key = { it.id }) { row ->
                    RoutineRowItem(
                        row = row,
                        today = today,
                        selecting = state.isSelecting,
                        selected = row.id in state.selectedIds,
                        onClick = { if (state.isSelecting) onToggleSelection(row.id) else onOpen(row.id) },
                        onLongClick = { if (state.isSelecting) onToggleSelection(row.id) else onStartSelection(row.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    pendingDelete?.let { kind ->
        val count = if (kind == PendingDelete.SELECTED) state.selectedIds.size else state.rows.size
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = {
                Text(
                    stringResource(
                        if (kind == PendingDelete.SELECTED) R.string.delete_selected_title else R.string.delete_all_title,
                        count,
                    ),
                )
            },
            text = { Text(stringResource(R.string.delete_many_body)) },
            confirmButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    if (kind == PendingDelete.SELECTED) onDeleteSelected() else onDeleteAll()
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/**
 * 누르면 수정, 꾹 누르면 선택 모드. 선택 모드에서는 누를 때마다 고르거나 뺀다.
 * 보관한 루틴은 흐리게 보인다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineRowItem(
    row: RoutineRow,
    today: LocalDate,
    selecting: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    ListItem(
        modifier = Modifier
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
                onLongClickLabel = stringResource(R.string.routine_select_label),
            )
            .alpha(if (row.archived) 0.6f else 1f),
        leadingContent = if (selecting) {
            { Checkbox(checked = selected, onCheckedChange = null) }
        } else {
            null
        },
        headlineContent = {
            Text(
                if (row.archived) "${row.title} · ${stringResource(R.string.routine_archived)}" else row.title,
            )
        },
        supportingContent = {
            Column(modifier = Modifier.padding(top = 2.dp)) {
                Text("${categoryLabel(row.category)} · ${intervalLabel(row.interval, row.repeats)}")
                Text(
                    text = row.lastDoneOn?.let { stringResource(R.string.routine_last_done, formatDate(it, today)) }
                        ?: stringResource(R.string.routine_never_done),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        trailingContent = {
            Text(
                text = row.nextDue?.let { stringResource(R.string.routine_next_due, formatDate(it, today)) }
                    ?: stringResource(R.string.routine_ended),
                style = MaterialTheme.typography.labelLarge,
                color = if (row.nextDue != null && row.nextDue < today && !row.archived) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
    )
}
