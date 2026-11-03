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
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
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
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.schedule.CalendarDay
import com.kikilog.routinecalendar.core.schedule.CalendarEntry
import com.kikilog.routinecalendar.core.schedule.weekDays
import com.kikilog.routinecalendar.ui.common.AddRoutineButton
import com.kikilog.routinecalendar.ui.common.CategoryDot
import com.kikilog.routinecalendar.ui.common.color
import com.kikilog.routinecalendar.ui.common.rememberNotificationEnabler
import com.kikilog.routinecalendar.ui.format.notifyTimeLabel
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
    val requestNotify = rememberNotificationEnabler(viewModel::onNotifyPermissionResult)
    CalendarScreen(
        state = state,
        onEnableNotify = requestNotify,
        onDismissNotifyCard = viewModel::onDismissNotifyCard,
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
    onEnableNotify: () -> Unit = {},
    onDismissNotifyCard: () -> Unit = {},
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
                    state = state,
                    month = month,
                    today = today,
                    selected = selected,
                    onSelect = onSelect,
                    onSwipePrev = onPrevMonth,
                    onSwipeNext = onNextMonth,
                )
            }
            if (state.showNotifyCard) {
                item(key = "notify-card") {
                    NotifyCard(notifyTimeLabel(state.notifyTime), onEnable = onEnableNotify, onLater = onDismissNotifyCard)
                }
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
                    category = state.categories.firstOrNull { it.id == entry.routine.categoryId },
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

/** 루틴을 등록한 뒤 한 번만 보이는 알림 안내. 처음 켜자마자 권한을 묻지 않기 위해 여기서 묻는다 */
@Composable
private fun NotifyCard(timeLabel: String, onEnable: () -> Unit, onLater: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(modifier = Modifier.padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 4.dp)) {
            Text(
                stringResource(R.string.notify_card_body, timeLabel),
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

/**
 * 요일 머리글 + 날짜 칸. 칸 수가 42개 이하로 정해져 있어 LazyVerticalGrid 대신 줄(Row)로 그린다.
 * 좌우로 밀면 이전 달 · 다음 달로 간다(끝까지 민 거리가 [SWIPE_THRESHOLD] 를 넘을 때).
 */
@Composable
private fun MonthGrid(
    state: CalendarUiState,
    month: LocalDate,
    today: LocalDate,
    selected: LocalDate,
    onSelect: (LocalDate) -> Unit,
    onSwipePrev: () -> Unit,
    onSwipeNext: () -> Unit,
) {
    val gridDates = state.gridDates
    val weekStart = state.weekStart
    val days = state.days
    val threshold = with(LocalDensity.current) { SWIPE_THRESHOLD.toPx() }
    Column(
        modifier = Modifier
            .padding(horizontal = 8.dp)
            .pointerInput(month) {
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { total = 0f },
                    onHorizontalDrag = { change, dx ->
                        change.consume()
                        total += dx
                    },
                    onDragEnd = {
                        when {
                            total > threshold -> onSwipePrev()
                            total < -threshold -> onSwipeNext()
                        }
                    },
                )
            },
    ) {
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
                    val day = days[date]
                    DayCell(
                        date = date,
                        inMonth = date.month == month.month && date.year == month.year,
                        isToday = date == today,
                        isSelected = date == selected,
                        isPast = date < today,
                        day = day,
                        dueCategories = day?.let { state.categoriesOf(it.dueIds) }.orEmpty(),
                        doneCategories = day?.let { state.categoriesOf(it.doneIds) }.orEmpty(),
                        onClick = { onSelect(date) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/**
 * 날짜 한 칸: 숫자 + 아래 표시. 분류마다 그 색 점 하나(할 예정), 지났는데 안 한 것은 빨간 테두리 점,
 * 그날 한 것은 그 분류 색 ✓. 칸이 작아 최대 [MAX_MARKS] 개까지, 넘치면 「+」.
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
    dueCategories: List<Category>,
    doneCategories: List<Category>,
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
        val marks = dueCategories.map { it to false } + doneCategories.map { it to true }
        Row(
            horizontalArrangement = Arrangement.spacedBy(2.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.heightIn(min = 14.dp),
        ) {
            marks.take(MAX_MARKS).forEach { (category, done) ->
                if (done) {
                    Text(text = "✓", style = MaterialTheme.typography.labelSmall, color = category.color)
                } else {
                    CategoryDot(
                        color = category.color,
                        size = MARK_SIZE,
                        // 지났는데 안 한 예정은 빨간 테두리
                        modifier = if (isPast) {
                            Modifier.border(1.5.dp, MaterialTheme.colorScheme.error, CircleShape)
                        } else {
                            Modifier
                        },
                    )
                }
            }
            if (marks.size > MAX_MARKS) {
                Text(text = "+", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private const val MAX_MARKS = 4
private val MARK_SIZE: Dp = 7.dp
private val SWIPE_THRESHOLD: Dp = 56.dp

/**
 * 고른 날짜의 한 줄. 오늘이거나 지난 날이면 눌러서 그날 한 것을 기록 · 취소, 꾹 누르면 루틴 정보.
 * 아직 오지 않은 날은 예정만 보여 준다(누르면 루틴 정보).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun EntryRow(
    entry: CalendarEntry,
    category: Category?,
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
                onLongClickLabel = stringResource(R.string.routine_info),
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
        category?.let { CategoryDot(it.color, modifier = Modifier.padding(start = 4.dp)) }
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
