package com.kikilog.routinecalendar.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.backup.BackupData
import com.kikilog.routinecalendar.backup.BackupFormatException
import com.kikilog.routinecalendar.backup.BackupProblem
import com.kikilog.routinecalendar.backup.BackupRepository
import com.kikilog.routinecalendar.backup.ImportMode
import com.kikilog.routinecalendar.backup.ImportOutcome
import com.kikilog.routinecalendar.backup.backupFileName
import com.kikilog.routinecalendar.backup.readBackupFile
import com.kikilog.routinecalendar.backup.writeBackupFile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/** 백업 작업 결과. 화면이 확인 창으로 보인다 */
sealed interface BackupMessage {
    data object Exported : BackupMessage
    data object ExportFailed : BackupMessage
    data class Imported(val outcome: ImportOutcome) : BackupMessage

    /** 읽거나 검사하지 못함. [problem] 이 null 이면 파일을 열지 못한 것 */
    data class Unreadable(val problem: BackupProblem?) : BackupMessage

    /** 검사는 통과했지만 DB 에 넣다 실패. 트랜잭션이라 지금 데이터는 그대로 */
    data object ImportFailed : BackupMessage
    data object Restored : BackupMessage
    data object RestoreFailed : BackupMessage
}

data class BackupUiState(
    /** 읽기 · 쓰기 중. 단추를 잠근다 */
    val busy: Boolean = false,
    val hasSnapshot: Boolean = false,
    /** 검사를 통과해 합치기 · 덮어쓰기를 고르기를 기다리는 파일 */
    val pending: BackupData? = null,
    val confirmRestore: Boolean = false,
    val message: BackupMessage? = null,
)

/**
 * 설정의 백업 메뉴. 파일 고르기는 화면(저장소 접근 프레임워크)이 하고, 고른 Uri 를 받아 읽고 쓴다.
 * 가져오기는 ① 파일 읽기 · 검사 → ② 합치기/덮어쓰기 고르기 → ③ 바꾸기 순서다.
 */
class BackupViewModel(
    private val repository: BackupRepository,
    private val read: suspend (Uri) -> String,
    private val write: suspend (Uri, String) -> Unit,
    private val today: () -> LocalDate,
) : ViewModel() {

    private val local = MutableStateFlow(BackupUiState())

    val state: StateFlow<BackupUiState> =
        combine(local, repository.hasSnapshot) { state, hasSnapshot -> state.copy(hasSnapshot = hasSnapshot) }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BackupUiState(hasSnapshot = repository.hasSnapshot.value))

    /** 내보내기 때 파일 선택기에 제안하는 이름 */
    fun suggestedFileName(): String = backupFileName(today())

    fun exportTo(uri: Uri) = work {
        val message = try {
            write(uri, repository.exportText())
            BackupMessage.Exported
        } catch (e: Exception) {
            BackupMessage.ExportFailed
        }
        local.update { it.copy(message = message) }
    }

    fun importFrom(uri: Uri) = work {
        try {
            val data = repository.parse(read(uri))
            local.update { it.copy(pending = data) }
        } catch (e: BackupFormatException) {
            local.update { it.copy(message = BackupMessage.Unreadable(e.problem)) }
        } catch (e: Exception) {
            local.update { it.copy(message = BackupMessage.Unreadable(null)) }
        }
    }

    fun confirmImport(mode: ImportMode) {
        val data = local.value.pending ?: return
        local.update { it.copy(pending = null) }
        work {
            val message = try {
                BackupMessage.Imported(repository.import(data, mode))
            } catch (e: Exception) {
                BackupMessage.ImportFailed
            }
            local.update { it.copy(message = message) }
        }
    }

    fun cancelImport() = local.update { it.copy(pending = null) }

    fun requestRestore() = local.update { it.copy(confirmRestore = true) }

    fun cancelRestore() = local.update { it.copy(confirmRestore = false) }

    fun confirmRestore() {
        local.update { it.copy(confirmRestore = false) }
        work {
            val message = try {
                if (repository.restoreSnapshot()) BackupMessage.Restored else BackupMessage.RestoreFailed
            } catch (e: Exception) {
                BackupMessage.RestoreFailed
            }
            local.update { it.copy(message = message) }
        }
    }

    fun dismissMessage() = local.update { it.copy(message = null) }

    /** 한 번에 하나만. 하는 동안 단추를 잠근다 */
    private fun work(block: suspend () -> Unit) {
        if (local.value.busy) return
        local.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                block()
            } finally {
                local.update { it.copy(busy = false) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as RoutineCalendarApp
                val resolver = app.contentResolver
                BackupViewModel(
                    repository = app.container.backupRepository,
                    read = { uri -> readBackupFile(resolver, uri) },
                    write = { uri, text -> writeBackupFile(resolver, uri, text) },
                    today = app.container.todayClock::current,
                )
            }
        }
    }
}
