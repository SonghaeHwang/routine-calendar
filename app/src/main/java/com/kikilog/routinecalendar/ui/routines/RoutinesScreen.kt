package com.kikilog.routinecalendar.ui.routines

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import com.kikilog.routinecalendar.ui.common.ConfirmDeleteDialog
import com.kikilog.routinecalendar.ui.common.EmptyState
import com.kikilog.routinecalendar.ui.common.RoutineActionsMenu
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
        onSetArchived = viewModel::onSetArchived,
        onDelete = viewModel::onDelete,
    )
}

@Composable
fun RoutinesScreen(
    state: RoutinesUiState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onToggleShowArchived: () -> Unit,
    onSetArchived: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
) {
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }
    var topMenuOpen by rememberSaveable { mutableStateOf(false) }

    TabScaffold(
        title = stringResource(R.string.tab_routines),
        onOpenSettings = onOpenSettings,
        floatingActionButton = { AddRoutineButton(onAdd) },
        actions = {
            Box {
                IconButton(onClick = { topMenuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = topMenuOpen, onDismissRequest = { topMenuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(if (state.showArchived) R.string.hide_archived else R.string.show_archived)) },
                        onClick = {
                            topMenuOpen = false
                            onToggleShowArchived()
                        },
                    )
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
                        onClick = { onOpen(row.id) },
                        onToggleArchive = { onSetArchived(row.id, !row.archived) },
                        onDelete = { pendingDeleteId = row.id },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    val pending = state.rows.firstOrNull { it.id == pendingDeleteId }
    if (pending != null) {
        ConfirmDeleteDialog(
            title = pending.title,
            onConfirm = {
                onDelete(pending.id)
                pendingDeleteId = null
            },
            onDismiss = { pendingDeleteId = null },
        )
    }
}

/** 누르면 수정, 길게 누르면 보관 · 삭제 메뉴. 보관한 루틴은 흐리게 보인다. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RoutineRowItem(
    row: RoutineRow,
    today: LocalDate,
    onClick: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }

    Box {
        ListItem(
            modifier = Modifier
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = { menuOpen = true },
                    onLongClickLabel = stringResource(R.string.routine_menu_label),
                )
                .alpha(if (row.archived) 0.6f else 1f),
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
        RoutineActionsMenu(
            expanded = menuOpen,
            archived = row.archived,
            onDismiss = { menuOpen = false },
            onToggleArchive = onToggleArchive,
            onDelete = onDelete,
        )
    }
}
