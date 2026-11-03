package com.kikilog.routinecalendar.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.data.StartScreen
import com.kikilog.routinecalendar.ui.common.ChoiceRow
import com.kikilog.routinecalendar.ui.common.SubScaffold
import kotlinx.datetime.DayOfWeek

/**
 * 설정 화면: 주 시작 요일 · 첫 화면 · 빠른 선택 편집 · 개인정보 안내 · 앱 버전.
 * 알림 시각 · 백업은 각 단계에서 추가한다(동작하지 않는 메뉴는 보이지 않는다).
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenPresets: () -> Unit,
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
                        options = StartScreen.entries,
                        selected = state.startScreen,
                        onSelect = viewModel::setStartScreen,
                        label = { startScreenLabel(it) },
                    )
                }
                HorizontalDivider()
            }
            ListItem(
                modifier = Modifier.clickable(onClick = onOpenPresets),
                headlineContent = { Text(stringResource(R.string.settings_presets)) },
                supportingContent = { Text(stringResource(R.string.settings_presets_hint)) },
            )
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
