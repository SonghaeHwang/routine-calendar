package com.kikilog.routinecalendar.ui.today

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.common.AddRoutineButton
import com.kikilog.routinecalendar.ui.common.EmptyState
import com.kikilog.routinecalendar.ui.common.TabScaffold

/** 오늘 화면 자리. 10번 작업에서 실제 목록으로 바꾼다. */
@Composable
fun TodayPlaceholder(onAdd: () -> Unit, onOpenSettings: () -> Unit) {
    TabScaffold(
        title = stringResource(R.string.tab_today),
        onOpenSettings = onOpenSettings,
        floatingActionButton = { AddRoutineButton(onAdd) },
    ) { padding ->
        EmptyState(stringResource(R.string.empty_today), padding)
    }
}
