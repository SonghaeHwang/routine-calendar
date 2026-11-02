package com.kikilog.routinecalendar.ui.routines

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.common.AddRoutineButton
import com.kikilog.routinecalendar.ui.common.EmptyState
import com.kikilog.routinecalendar.ui.common.TabScaffold

/** 루틴 목록 자리. 9번 작업에서 실제 목록으로 바꾼다. */
@Composable
fun RoutinesPlaceholder(onAdd: () -> Unit, onOpenSettings: () -> Unit) {
    TabScaffold(
        title = stringResource(R.string.tab_routines),
        onOpenSettings = onOpenSettings,
        floatingActionButton = { AddRoutineButton(onAdd) },
    ) { padding ->
        EmptyState(stringResource(R.string.empty_routines), padding)
    }
}
