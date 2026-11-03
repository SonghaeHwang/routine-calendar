package com.kikilog.routinecalendar.ui.common

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kikilog.routinecalendar.R

/** 루틴 하나에 대한 보관 · 삭제 메뉴. 루틴 목록(길게 누르기)과 수정 화면(더보기)이 함께 쓴다. */
@Composable
fun RoutineActionsMenu(
    expanded: Boolean,
    archived: Boolean,
    onDismiss: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text(stringResource(if (archived) R.string.action_unarchive else R.string.action_archive)) },
            onClick = {
                onDismiss()
                onToggleArchive()
            },
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) },
            onClick = {
                onDismiss()
                onDelete()
            },
        )
    }
}

/** 삭제는 되돌릴 수 없으므로 한 번 더 묻는다. 보관을 함께 권한다. */
@Composable
fun ConfirmDeleteDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_title, title)) },
        text = { Text(stringResource(R.string.delete_body)) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
