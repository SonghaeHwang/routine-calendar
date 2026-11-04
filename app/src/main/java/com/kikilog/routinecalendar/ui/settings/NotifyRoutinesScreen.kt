package com.kikilog.routinecalendar.ui.settings

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.common.CategoryDot
import com.kikilog.routinecalendar.ui.common.EmptyState
import com.kikilog.routinecalendar.ui.common.SubScaffold
import com.kikilog.routinecalendar.ui.common.color

@Composable
fun NotifyRoutinesRoute(onBack: () -> Unit) {
    val viewModel: NotifyRoutinesViewModel = viewModel(factory = NotifyRoutinesViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    NotifyRoutinesScreen(state = state, onBack = onBack, onToggle = viewModel::setNotify, onToggleAll = viewModel::setAll)
}

/** 설정 → 알림에 넣을 루틴. 맨 위 「모두」 스위치로 한꺼번에, 줄마다 하나씩 켜고 끈다 */
@Composable
fun NotifyRoutinesScreen(
    state: NotifyRoutinesUiState,
    onBack: () -> Unit,
    onToggle: (id: Long, notify: Boolean) -> Unit,
    onToggleAll: (Boolean) -> Unit,
) {
    SubScaffold(title = stringResource(R.string.notify_routines), onBack = onBack) { padding ->
        if (state.isLoading) return@SubScaffold
        if (state.rows.isEmpty()) {
            EmptyState(stringResource(R.string.notify_routines_empty), padding)
            return@SubScaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
        ) {
            item(key = "all") {
                ListItem(
                    modifier = Modifier.toggleable(value = state.allOn, role = Role.Switch) { onToggleAll(it) },
                    headlineContent = { Text(stringResource(R.string.notify_routines_all), style = MaterialTheme.typography.titleMedium) },
                    supportingContent = { Text(stringResource(R.string.notify_routines_hint)) },
                    trailingContent = { Switch(checked = state.allOn, onCheckedChange = null) },
                )
                HorizontalDivider()
            }
            items(state.rows, key = { it.routine.id }) { row ->
                val routine = row.routine
                ListItem(
                    modifier = Modifier.toggleable(value = routine.notify, role = Role.Switch) { onToggle(routine.id, it) },
                    leadingContent = row.category?.let { c -> { CategoryDot(c.color, size = 12.dp) } },
                    headlineContent = { Text(routine.title) },
                    trailingContent = { Switch(checked = routine.notify, onCheckedChange = null) },
                )
            }
        }
    }
}
