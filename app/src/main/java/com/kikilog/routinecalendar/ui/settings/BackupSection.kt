package com.kikilog.routinecalendar.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.backup.BackupProblem
import com.kikilog.routinecalendar.backup.ImportMode
import com.kikilog.routinecalendar.backup.ImportOutcome

/** 메신저 · 드라이브에서 받은 파일은 JSON 으로 잡히지 않을 수 있어 넓게 고르고 내용으로 거른다 */
private val IMPORT_MIME_TYPES = arrayOf("application/json", "text/plain", "application/octet-stream")

/**
 * 설정의 백업 메뉴: 내보내기 · 가져오기 · (가져온 적이 있으면) 되돌리기.
 * 파일 선택기 결과는 프로세스가 다시 살아나도 받도록 조건 없이 등록한다.
 */
@Composable
fun BackupSection(viewModel: BackupViewModel = viewModel(factory = BackupViewModel.Factory)) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(viewModel::exportTo)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(viewModel::importFrom)
    }

    ListItem(
        modifier = Modifier.clickable(enabled = !state.busy) { exportLauncher.launch(viewModel.suggestedFileName()) },
        headlineContent = { Text(stringResource(R.string.backup_export)) },
        supportingContent = { Text(stringResource(R.string.backup_export_hint)) },
    )
    HorizontalDivider()
    ListItem(
        modifier = Modifier.clickable(enabled = !state.busy) { importLauncher.launch(IMPORT_MIME_TYPES) },
        headlineContent = { Text(stringResource(R.string.backup_import)) },
        supportingContent = { Text(stringResource(R.string.backup_import_hint)) },
    )
    if (state.hasSnapshot) {
        HorizontalDivider()
        ListItem(
            modifier = Modifier.clickable(enabled = !state.busy, onClick = viewModel::requestRestore),
            headlineContent = { Text(stringResource(R.string.backup_restore)) },
            supportingContent = { Text(stringResource(R.string.backup_restore_hint)) },
        )
    }

    state.pending?.let { data ->
        AlertDialog(
            onDismissRequest = viewModel::cancelImport,
            title = { Text(stringResource(R.string.backup_import_title)) },
            text = { Text(stringResource(R.string.backup_import_body, data.routines.size, data.completionCount)) },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmImport(ImportMode.MERGE) }) { Text(stringResource(R.string.backup_merge)) }
                TextButton(onClick = { viewModel.confirmImport(ImportMode.REPLACE) }) { Text(stringResource(R.string.backup_replace)) }
            },
            dismissButton = { TextButton(onClick = viewModel::cancelImport) { Text(stringResource(R.string.cancel)) } },
        )
    }

    if (state.confirmRestore) {
        AlertDialog(
            onDismissRequest = viewModel::cancelRestore,
            title = { Text(stringResource(R.string.backup_restore_title)) },
            text = { Text(stringResource(R.string.backup_restore_body)) },
            confirmButton = { TextButton(onClick = viewModel::confirmRestore) { Text(stringResource(R.string.backup_restore_confirm)) } },
            dismissButton = { TextButton(onClick = viewModel::cancelRestore) { Text(stringResource(R.string.cancel)) } },
        )
    }

    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissMessage,
            text = { Text(messageText(message)) },
            confirmButton = { TextButton(onClick = viewModel::dismissMessage) { Text(stringResource(R.string.ok)) } },
        )
    }
}

@Composable
private fun messageText(message: BackupMessage): String = when (message) {
    BackupMessage.Exported -> stringResource(R.string.backup_exported)
    BackupMessage.ExportFailed -> stringResource(R.string.backup_export_failed)
    is BackupMessage.Imported -> when (val outcome = message.outcome) {
        is ImportOutcome.Merged -> stringResource(R.string.backup_merged, outcome.counts.routines, outcome.counts.completions)
        is ImportOutcome.Replaced -> stringResource(R.string.backup_replaced, outcome.routines, outcome.completions)
    }
    is BackupMessage.Unreadable -> stringResource(
        when (message.problem) {
            BackupProblem.NOT_BACKUP -> R.string.backup_not_backup
            BackupProblem.NEWER_VERSION -> R.string.backup_newer
            BackupProblem.TOO_LARGE -> R.string.backup_too_large
            null -> R.string.backup_read_failed
        },
    )
    BackupMessage.ImportFailed -> stringResource(R.string.backup_import_failed)
    BackupMessage.Restored -> stringResource(R.string.backup_restored)
    BackupMessage.RestoreFailed -> stringResource(R.string.backup_restore_failed)
}
