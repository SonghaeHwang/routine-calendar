package com.kikilog.routinecalendar.ui.info

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    RoutineInfoScreen(state = state, onBack = onBack, onEdit = { onEdit(routineId) }, onNotifyChange = viewModel::setNotify)
}

/**
 * 루틴 정보. 갤럭시 캘린더처럼 이름 · 분류 · 주기 · 날짜 줄을 누르면 편집 화면으로 가고,
 * 알림은 스위치로 바로 켜고 끈다. 맨 아래 「편집하기」도 같은 편집 화면이다.
 */
@Composable
fun RoutineInfoScreen(
    state: RoutineInfoUiState,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onNotifyChange: (Boolean) -> Unit = {},
) {
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
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClickLabel = stringResource(R.string.info_edit), onClick = onEdit)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
            )
            InfoRow(stringResource(R.string.label_category), onClick = onEdit) {
                state.category?.let { category ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CategoryDot(category.color, size = 14.dp)
                        Text(category.name, style = InfoValueStyle, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
            InfoRow(stringResource(R.string.label_interval), onClick = onEdit) {
                Text(intervalLabel(routine.interval, routine.repeats), style = InfoValueStyle)
            }
            InfoRow(stringResource(R.string.info_next_due), onClick = onEdit) {
                val next = state.nextDue
                when {
                    next == null -> Text(stringResource(R.string.routine_ended), style = InfoValueStyle)
                    state.overdueDays > 0 -> Text(
                        "${formatDate(next, today)} · ${overdueLabel(state.overdueDays)}",
                        style = InfoValueStyle,
                        color = MaterialTheme.colorScheme.error,
                    )
                    else -> Text(formatDate(next, today), style = InfoValueStyle)
                }
            }
            InfoRow(stringResource(R.string.label_last_done), onClick = onEdit) {
                Text(
                    state.lastDoneOn?.let { formatDate(it, today) } ?: stringResource(R.string.routine_never_done),
                    style = InfoValueStyle,
                )
            }
            HorizontalDivider()
            ListItem(
                modifier = Modifier.toggleable(value = routine.notify, role = Role.Switch, onValueChange = onNotifyChange),
                headlineContent = { Text(stringResource(R.string.label_notify), style = InfoLabelStyle) },
                supportingContent = { Text(stringResource(R.string.notify_hint)) },
                trailingContent = { Switch(checked = routine.notify, onCheckedChange = null) },
                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            )
            Text(
                text = stringResource(R.string.info_tap_to_edit),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
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

/** 정보 화면은 한눈에 읽도록 목록보다 글자를 키운다 */
private val InfoLabelStyle = TextStyle(fontSize = 18.sp, lineHeight = 26.sp)
private val InfoValueStyle = TextStyle(fontSize = 18.sp, lineHeight = 26.sp)

/** 왼쪽 이름, 오른쪽 값 한 줄. 누르면 편집 화면 */
@Composable
private fun InfoRow(label: String, onClick: () -> Unit, value: @Composable () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.info_edit), onClick = onClick),
        headlineContent = { Text(label, style = InfoLabelStyle, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        trailingContent = { value() },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}
