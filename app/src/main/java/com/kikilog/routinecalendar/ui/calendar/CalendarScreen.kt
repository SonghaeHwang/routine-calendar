package com.kikilog.routinecalendar.ui.calendar

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.core.schedule.CalendarDay
import com.kikilog.routinecalendar.core.schedule.CalendarEntry
import com.kikilog.routinecalendar.core.schedule.weekDays
import com.kikilog.routinecalendar.ui.common.AddRoutineButton
import com.kikilog.routinecalendar.ui.common.TabScaffold
import com.kikilog.routinecalendar.ui.format.dayOfWeekShort
import com.kikilog.routinecalendar.ui.format.formatDate
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

@Composable
fun CalendarRoute(onAdd: () -> Unit, onOpen: (Long) -> Unit, onOpenSettings: () -> Unit) {
    val viewModel: CalendarViewModel = viewModel(factory = CalendarViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    CalendarScreen(
        state = state,
        onAdd = onAdd,
        onOpen = onOpen,
        onOpenSettings = onOpenSettings,
        onPrevMonth = viewModel::onPrevMonth,
        onNextMonth = viewModel::onNextMonth,
        onGoToday = viewModel::onGoToday,
        onSelect = viewModel::onSelect,
        onToggle = viewModel::onToggle,
    )
}

@Composable
fun CalendarScreen(
    state: CalendarUiState,
    onAdd: () -> Unit,
    onOpen: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onPrevMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onGoToday: () -> Unit,
    onSelect: (LocalDate) -> Unit,
    onToggle: (CalendarEntry) -> Unit,
) {
    TabScaffold(
        title = stringResource(R.string.tab_calendar),
        onOpenSettings = onOpenSettings,
        floatingActionButton = { AddRoutineButton(onAdd) },
    ) { padding ->
        val today = state.today
        val month = state.month
        val selected = state.selectedDate
        if (state.isLoading || today == null || month == null || selected == null) return@TabScaffold

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding(),
                // [+] 단추에 마지막 줄이 가리지 않게
                bottom = padding.calculateBottomPadding() + 88.dp,
            ),
        ) {
            item(key = "month") {
                MonthHeader(month = month, onPrev = onPrevMonth, onNext = onNextMonth, onGoToday = onGoToday)
            }
            item(key = "grid") {
                MonthGrid(
                    gridDates = state.gridDates,
                    month = month,
                    today = today,
                    selected = selected,
                    weekStart = state.weekStart,
                    days = state.days,
                    onSelect = onSelect,
                )
            }
            item(key = "selected-title") {
                HorizontalDivider(modifier = Modifier.padding(top = 8.dp))
                Text(
                    text = formatDate(selected, today),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
                        .semantics { heading() },
                )
            }
            if (state.entries.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.calendar_day_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.entries, key = { "entry-${it.routine.id}" }) { entry ->
                EntryRow(
                    entry = entry,
                    checkable = selected <= today,
                    overdue = entry.due && !entry.done && selected < today,
                    onToggle = { onToggle(entry) },
                    onOpen = { onOpen(entry.routine.id) },
                )
            }
        }
    }
}

@Composable
private fun MonthHeader(month: LocalDate, onPrev: () -> Unit, onNext: () -> Unit, onGoToday: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(painterResource(R.drawable.ic_chevron_left), contentDescription = stringResource(R.string.calendar_prev))
        }
        Text(
            text = stringResource(R.string.calendar_month_title, month.year, month.month.number),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() },
        )
        IconButton(onClick = onNext) {
            Icon(painterResource(R.drawable.ic_chevron_right), contentDescription = stringResource(R.string.calendar_next))
        }
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onGoToday) { Text(stringResource(R.string.calendar_today)) }
    }
}

/** 요일 머리글 + 날짜 칸. 칸 수가 42개 이하로 정해져 있어 LazyVerticalGrid 대신 줄(Row)로 그린다 */
@Composable
private fun MonthGrid(
    gridDates: List<LocalDate>,
    month: LocalDate,
    today: LocalDate,
    selected: LocalDate,
    weekStart: DayOfWeek,
    days: Map<LocalDate, CalendarDay>,
    onSelect: (LocalDate) -> Unit,
) {
    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays(weekStart).forEach { day ->
                Text(
                    text = dayOfWeekShort(day),
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                    color = when (day) {
                        DayOfWeek.SUNDAY -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = 8.dp),
                )
            }
        }
        gridDates.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    DayCell(
                        date = date,
                        inMonth = date.month == month.month && date.year == month.year,
                        isToday = date == today,
                        isSelected = date == selected,
                        isPast = date < today,
                        day = days[date],
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 날짜 한 칸: 숫자 + 아래 표시. ● 는 할 예정(지났는데 안 했으면 빨강), ✓ 는 그날 한 것.
 * 오늘은 동그라미 배경, 고른 날은 테두리, 앞뒤 달은 흐리게.
 */
@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    isPast: Boolean,
    day: CalendarDay?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dueCount = day?.dueIds?.size ?: 0
    val doneCount = day?.doneIds?.size ?: 0
    val description = stringResource(R.string.calendar_cell_desc, formatDate(date, date), dueCount, doneCount)
    Column(
        modifier = modifier
            .heightIn(min = 56.dp)
            .padding(2.dp)
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick, role = Role.Button)
            .semantics {
                contentDescription = description
                selected = isSelected
            }
            .alpha(if (inMonth) 1f else 0.4f)
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .then(if (isToday) Modifier.background(MaterialTheme.colorScheme.primaryContainer, CircleShape) else Modifier),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.day.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            if (dueCount > 0) {
                Text(
                    text = "●",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPast) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                )
            }
            if (doneCount > 0) {
                Text(text = "✓", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
            }
        }
    }
}

/**
 * 고른 날짜의 한 줄. 오늘이거나 지난 날이면 눌러서 그날 한 것을 기록 · 취소, 꾹 누르면 수정.
 * 아직 오지 않은 날은 예정만 보여 준다.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    entry: CalendarEntry,
    checkable: Boolean,
    overdue: Boolean,
    onToggle: () -> Unit,
    onOpen: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .combinedClickable(
                role = if (checkable) Role.Checkbox else Role.Button,
                onClick = { if (checkable) onToggle() else onOpen() },
                onLongClick = onOpen,
                onLongClickLabel = stringResource(R.string.edit_routine),
            )
            .then(if (checkable) Modifier.semantics { toggleableState = ToggleableState(entry.done) } else Modifier)
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (checkable) {
            Checkbox(checked = entry.done, onCheckedChange = null, modifier = Modifier.padding(8.dp))
        } else {
            Spacer(modifier = Modifier.size(48.dp))
        }
        Text(
            text = entry.routine.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (entry.done) TextDecoration.LineThrough else null,
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        )
        Text(
            text = stringResource(
                when {
                    entry.done -> R.string.calendar_done
                    overdue -> R.string.calendar_missed
                    else -> R.string.calendar_due
                },
            ),
            style = MaterialTheme.typography.labelLarge,
            color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 8.dp),
        )
    }
}
