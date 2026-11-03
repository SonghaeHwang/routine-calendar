package com.kikilog.routinecalendar.ui.info

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.common.CategoryDot
import com.kikilog.routinecalendar.ui.common.SubScaffold
import com.kikilog.routinecalendar.ui.common.color
import com.kikilog.routinecalendar.ui.format.formatDate
import com.kikilog.routinecalendar.ui.format.intervalLabel
import com.kikilog.routinecalendar.ui.format.overdueLabel

@Composable
fun RoutineInfoRoute(routineId: Long, onBack: () -> Unit, onEdit: (Long) -> Unit) {
    val viewModel: RoutineInfoViewModel = viewModel(factory = RoutineInfoViewModel.factory(routineId))
    val state by viewModel.state.collectAsStateWithLifecycle()
    // 편집 화면에서 삭제하고 돌아오면 닫는다
    LaunchedEffect(state.notFound) { if (state.notFound) onBack() }
    RoutineInfoScreen(state = state, onBack = onBack, onEdit = { onEdit(routineId) })
}

/** 루틴 정보(읽기 전용). 맨 아래 「편집하기」로 수정 화면에 간다 */
@Composable
fun RoutineInfoScreen(state: RoutineInfoUiState, onBack: () -> Unit, onEdit: () -> Unit) {
    SubScaffold(title = stringResource(R.string.routine_info), onBack = onBack) { padding ->
        val routine = state.routine ?: return@SubScaffold
        val today = state.today ?: return@SubScaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            Text(
                text = routine.title,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
            InfoRow(stringResource(R.string.label_category)) {
                state.category?.let { category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryDot(category.color, size = 12.dp)
                        Text(category.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            InfoRow(stringResource(R.string.label_interval)) { Text(intervalLabel(routine.interval, routine.repeats)) }
            InfoRow(stringResource(R.string.label_repeats)) {
                Text(stringResource(if (routine.repeats) R.string.info_yes else R.string.info_no))
            }
            InfoRow(stringResource(R.string.label_notify)) {
                Text(stringResource(if (routine.notify) R.string.info_on else R.string.info_off))
            }
            HorizontalDivider()
            InfoRow(stringResource(R.string.info_next_due)) {
                val next = state.nextDue
                when {
                    next == null -> Text(stringResource(R.string.routine_ended))
                    state.overdueDays > 0 -> Text(
                        "${formatDate(next, today)} · ${overdueLabel(state.overdueDays)}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> Text(formatDate(next, today))
                }
            }
            InfoRow(stringResource(R.string.label_last_done)) {
                Text(state.lastDoneOn?.let { formatDate(it, today) } ?: stringResource(R.string.routine_never_done))
            }
            if (state.upcoming.size > 1) {
                InfoRow(stringResource(R.string.info_upcoming)) {
                    Column {
                        state.upcoming.forEach { Text(formatDate(it, today)) }
                    }
                }
            }
            state.createdOn?.let { createdOn ->
                InfoRow(stringResource(R.string.info_created_on)) { Text(formatDate(createdOn, today)) }
            }
            Button(
                onClick = onEdit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.info_edit))
            }
        }
    }
}

/** 왼쪽 이름, 오른쪽 값 한 줄 */
@Composable
private fun InfoRow(label: String, value: @Composable () -> Unit) {
    ListItem(
        headlineContent = { Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { value() },
        colors = androidx.compose.material3.ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
