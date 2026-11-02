package com.kikilog.routinecalendar.ui.calendar

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.common.EmptyState
import com.kikilog.routinecalendar.ui.common.TabScaffold

/** 캘린더 탭 자리. 4단계에서 월 그리드로 바꾼다. */
@Composable
fun CalendarPlaceholder(onOpenSettings: () -> Unit) {
    TabScaffold(title = stringResource(R.string.tab_calendar), onOpenSettings = onOpenSettings) { padding ->
        EmptyState(stringResource(R.string.calendar_placeholder), padding)
    }
}
