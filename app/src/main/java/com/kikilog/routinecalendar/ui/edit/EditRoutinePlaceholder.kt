package com.kikilog.routinecalendar.ui.edit

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.common.EmptyState
import com.kikilog.routinecalendar.ui.common.SubScaffold

/** 등록 화면 자리. 8번 작업에서 실제 입력 화면으로 바꾼다. */
@Composable
fun EditRoutinePlaceholder(onBack: () -> Unit) {
    SubScaffold(title = stringResource(R.string.add_routine), onBack = onBack) { padding ->
        EmptyState(stringResource(R.string.edit_placeholder), padding)
    }
}
