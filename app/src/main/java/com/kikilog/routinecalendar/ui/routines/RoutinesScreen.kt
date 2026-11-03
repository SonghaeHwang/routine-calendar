package com.kikilog.routinecalendar.ui.routines

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
    RoutinesScreen(state = state, onAdd = onAdd, onOpen = onOpen, onOpenSettings = onOpenSettings)
}

@Composable
fun RoutinesScreen(
    state: RoutinesUiState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    TabScaffold(
        title = stringResource(R.string.tab_routines),
        onOpenSettings = onOpenSettings,
        floatingActionButton = { AddRoutineButton(onAdd) },
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
                    RoutineRowItem(row = row, today = today, onClick = { onOpen(row.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun RoutineRowItem(row: RoutineRow, today: LocalDate, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(row.title) },
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
                color = if (row.nextDue != null && row.nextDue < today) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        },
    )
}
