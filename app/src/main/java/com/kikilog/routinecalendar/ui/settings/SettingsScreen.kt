package com.kikilog.routinecalendar.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.core.notify.NOTIFY_TIME_OPTIONS
import com.kikilog.routinecalendar.core.notify.NotifyTime
import com.kikilog.routinecalendar.data.StartScreen
import com.kikilog.routinecalendar.ui.common.ChoiceRow
import com.kikilog.routinecalendar.ui.common.SubScaffold
import com.kikilog.routinecalendar.ui.common.openAppNotificationSettings
import com.kikilog.routinecalendar.ui.common.openBatteryOptimizationSettings
import com.kikilog.routinecalendar.ui.common.rememberCanPostNotifications
import com.kikilog.routinecalendar.ui.common.rememberNotificationEnabler
import com.kikilog.routinecalendar.ui.format.notifyTimeLabel
import kotlinx.datetime.DayOfWeek

/**
 * 설정 화면: 알림 · 주 시작 요일 · 첫 화면 · 분류 · 빠른 선택 편집 · 백업 · 개인정보 안내 · 앱 버전.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPresets: () -> Unit,
    onOpenCategories: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // AGP 9 는 BuildConfig 를 기본으로 만들지 않아 패키지 정보에서 읽는다
    val versionName = remember(context) {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }

    SubScaffold(title = stringResource(R.string.settings), onBack = onBack) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            if (!state.isLoading) {
                NotifySettings(
                    enabled = state.notifyEnabled,
                    time = state.notifyTime,
                    onEnabledChange = viewModel::setNotifyEnabled,
                    onTimeChange = viewModel::setNotifyTime,
                )
                HorizontalDivider()
                ChoiceSetting(
                    title = stringResource(R.string.settings_week_start),
                    hint = stringResource(R.string.settings_week_start_hint),
                ) {
                    ChoiceRow(
                        options = WEEK_START_OPTIONS,
                        selected = state.weekStart,
                        onSelect = viewModel::setWeekStart,
                        label = { weekStartLabel(it) },
                    )
                }
                HorizontalDivider()
                ChoiceSetting(
                    title = stringResource(R.string.settings_start_screen),
                    hint = stringResource(R.string.settings_start_screen_hint),
                ) {
                    ChoiceRow(
                        options = START_SCREEN_OPTIONS,
                        // 오늘 탭을 숨기기 전에 고른 「오늘」은 달력으로 열리므로 달력으로 보인다
                        selected = state.startScreen.takeIf { it in START_SCREEN_OPTIONS } ?: StartScreen.CALENDAR,
                        onSelect = viewModel::setStartScreen,
                        label = { startScreenLabel(it) },
                    )
                }
                HorizontalDivider()
            }
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenCategories),
                headlineContent = { Text(stringResource(R.string.categories_edit)) },
                supportingContent = { Text(stringResource(R.string.categories_edit_hint)) },
            )
            HorizontalDivider()
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenPresets),
                headlineContent = { Text(stringResource(R.string.settings_presets)) },
                supportingContent = { Text(stringResource(R.string.settings_presets_hint)) },
            )
            HorizontalDivider()
            BackupSection()
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy_title)) },
                supportingContent = { Text(stringResource(R.string.settings_privacy_body)) },
            )
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_version)) },
                supportingContent = { Text(versionName) },
            )
        }
    }
}

/**
 * 알림 켜기 스위치 · 시각 · (꺼져 있을 때) 휴대폰 설정 안내 · (켜져 있을 때) 배터리 안내.
 * 켤 때만 권한을 묻는다. 거절하면 꺼진 채로 두고, 휴대폰 설정으로 보내는 것은 사용자가 단추를 누를 때만.
 */
@Composable
private fun NotifySettings(
    enabled: Boolean,
    time: NotifyTime,
    onEnabledChange: (Boolean) -> Unit,
    onTimeChange: (NotifyTime) -> Unit,
) {
    val context = LocalContext.current
    val canPost = rememberCanPostNotifications()
    var denied by rememberSaveable { mutableStateOf(false) }
    var showTimePicker by rememberSaveable { mutableStateOf(false) }
    val requestEnable = rememberNotificationEnabler { granted ->
        denied = !granted
        if (granted) onEnabledChange(true)
    }
    // 켜 두었어도 휴대폰 설정에서 알림을 끄면 실제로는 오지 않으므로 꺼진 것으로 보인다
    val effectivelyOn = enabled && canPost

    ListItem(
        modifier = Modifier.clickable(role = Role.Switch) {
            if (effectivelyOn) onEnabledChange(false) else requestEnable()
        },
        headlineContent = { Text(stringResource(R.string.settings_notify)) },
        supportingContent = { Text(stringResource(R.string.settings_notify_hint)) },
        trailingContent = { Switch(checked = effectivelyOn, onCheckedChange = null) },
    )
    if (!canPost && (denied || enabled)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                stringResource(R.string.settings_notify_blocked),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { openAppNotificationSettings(context) }) {
                Text(stringResource(R.string.settings_open_notification_settings))
            }
        }
    }
    ListItem(
        modifier = Modifier.clickable { showTimePicker = true },
        headlineContent = { Text(stringResource(R.string.settings_notify_time)) },
        supportingContent = { Text(stringResource(R.string.settings_notify_time_hint)) },
        trailingContent = { Text(notifyTimeLabel(time), style = MaterialTheme.typography.bodyLarge) },
    )
    if (effectivelyOn) {
        Column(modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp)) {
            Text(
                stringResource(R.string.settings_battery_hint),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            TextButton(onClick = { openBatteryOptimizationSettings(context) }) {
                Text(stringResource(R.string.settings_open_battery_settings))
            }
        }
    }

    if (showTimePicker) {
        NotifyTimeDialog(
            selected = time,
            onSelect = {
                onTimeChange(it)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false },
        )
    }
}

/** 30분 간격 48개 중에서 고른다. 지금 시각이 보이도록 그 근처부터 보인다 */
@Composable
private fun NotifyTimeDialog(selected: NotifyTime, onSelect: (NotifyTime) -> Unit, onDismiss: () -> Unit) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (NOTIFY_TIME_OPTIONS.indexOf(selected) - 2).coerceAtLeast(0),
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_notify_time)) },
        text = {
            LazyColumn(state = listState, modifier = Modifier.heightIn(max = 360.dp)) {
                items(NOTIFY_TIME_OPTIONS) { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 48.dp)
                            .selectable(selected = option == selected, role = Role.RadioButton) { onSelect(option) },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = option == selected, onClick = null)
                        Text(notifyTimeLabel(option), modifier = Modifier.padding(start = 16.dp))
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } },
    )
}

/** 제목 · 설명 아래에 고르는 단추 줄을 둔 설정 한 칸 */
@Composable
private fun ChoiceSetting(title: String, hint: String, choices: @Composable () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        choices()
    }
}

@Composable
private fun weekStartLabel(day: DayOfWeek): String = stringResource(
    if (day == DayOfWeek.MONDAY) R.string.settings_week_monday else R.string.settings_week_sunday,
)

@Composable
private fun startScreenLabel(screen: StartScreen): String = stringResource(
    when (screen) {
        StartScreen.TODAY -> R.string.tab_today
        StartScreen.CALENDAR -> R.string.tab_calendar
        StartScreen.ROUTINES -> R.string.tab_routines
    },
)
