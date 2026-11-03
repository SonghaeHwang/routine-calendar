package com.kikilog.routinecalendar.backup

import com.kikilog.routinecalendar.core.notify.NotifyTime
import com.kikilog.routinecalendar.data.SettingsRepository
import com.kikilog.routinecalendar.data.StartScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.datetime.DayOfWeek
import java.io.File

/** 가져오기 방식 */
enum class ImportMode {
    /** 지금 데이터에 더한다. 같은 이름 루틴은 기록만 합친다. 설정은 그대로 */
    MERGE,

    /** 지금 데이터를 파일 내용으로 바꾼다. 설정도 파일 것 */
    REPLACE,
}

/** 가져오기 결과. 화면이 개수를 보인다 */
sealed interface ImportOutcome {
    data class Merged(val counts: MergeCounts) : ImportOutcome
    data class Replaced(val routines: Int, val completions: Int) : ImportOutcome
}

/** 파일로 고를 수 있는 주 시작 요일(설정 화면과 같은 두 가지) */
private val BACKUP_WEEK_STARTS = setOf(DayOfWeek.SUNDAY, DayOfWeek.MONDAY)

/**
 * 백업 내보내기 · 가져오기. 파일을 고르고 읽고 쓰는 일(저장소 접근 프레임워크)은 화면 쪽이 하고,
 * 여기서는 글자 ↔ DB 만 다룬다.
 *
 * 가져오기 직전 지금 상태를 [snapshotFile] 에 한 벌 저장해 [restoreSnapshot] 으로 되돌릴 수 있다.
 * 이 파일은 자동 백업에서 빠지는 폴더(noBackupFilesDir)에 둔다.
 *
 * @param onSettingsReplaced 설정을 바꾼 뒤(알림 시각) 알림 예약을 다시 맞춘다
 */
class BackupRepository(
    private val dao: BackupDao,
    private val settings: SettingsRepository,
    private val snapshotFile: File,
    private val now: () -> Long,
    private val appVersion: String,
    private val onSettingsReplaced: suspend () -> Unit = {},
) {
    private val _hasSnapshot = MutableStateFlow(snapshotFile.exists())

    /** 「가져오기 전으로 되돌리기」를 보일지 */
    val hasSnapshot: StateFlow<Boolean> = _hasSnapshot.asStateFlow()

    /** 지금 루틴 · 기록 · 분류 · 빠른 선택 · 설정을 JSON 글자로 */
    suspend fun exportText(): String {
        val dump = dao.dump()
        return encodeBackup(
            categories = dump.categories,
            routines = dump.routines,
            completions = dump.completions,
            presets = dump.presets,
            settings = currentSettings(),
            exportedAt = now(),
            appVersion = appVersion,
        )
    }

    /**
     * 파일 글자를 검사한다. DB 는 건드리지 않는다. 확인 창에 개수를 보이고, 고른 방식으로 [import] 한다.
     * @throws BackupFormatException
     */
    fun parse(text: String): BackupData = decodeBackup(text)

    /** 지금 상태를 자동 저장한 뒤 바꾼다. DB 를 바꾸다 실패하면 아무것도 바뀌지 않는다 */
    suspend fun import(data: BackupData, mode: ImportMode): ImportOutcome {
        saveSnapshot()
        return when (mode) {
            ImportMode.MERGE -> ImportOutcome.Merged(dao.merge(data))
            ImportMode.REPLACE -> {
                dao.replaceAll(data)
                applySettings(data.settings)
                ImportOutcome.Replaced(data.routines.size, data.completionCount)
            }
        }
    }

    /** 가져오기 전 상태로 덮어쓴다. 되돌린 뒤에는 저장해 둔 것을 지운다. 저장해 둔 것이 없으면 false */
    suspend fun restoreSnapshot(): Boolean {
        val text = withContext(Dispatchers.IO) { snapshotFile.takeIf { it.exists() }?.readText() } ?: return false
        val data = decodeBackup(text)
        dao.replaceAll(data)
        applySettings(data.settings)
        withContext(Dispatchers.IO) { snapshotFile.delete() }
        _hasSnapshot.value = false
        return true
    }

    private suspend fun saveSnapshot() {
        val text = exportText()
        withContext(Dispatchers.IO) {
            snapshotFile.parentFile?.mkdirs()
            // 다 쓴 뒤 이름을 바꿔, 쓰다 멈춰도 반쪽 파일이 남지 않게
            val temp = File(snapshotFile.parentFile, snapshotFile.name + ".tmp")
            temp.writeText(text)
            if (!temp.renameTo(snapshotFile)) {
                snapshotFile.delete()
                check(temp.renameTo(snapshotFile)) { "자동 저장 파일을 만들지 못함" }
            }
        }
        _hasSnapshot.value = true
    }

    private suspend fun currentSettings() = BackupSettings(
        weekStart = settings.weekStart.first().name,
        startScreen = settings.startScreen.first().name,
        notifyTime = settings.notifyTime.first().encode(),
    )

    /** 알 수 없는 값은 건너뛰고 지금 값을 둔다. 알림 켜기는 바꾸지 않는다 */
    private suspend fun applySettings(values: BackupSettings) {
        values.weekStart
            ?.let { name -> BACKUP_WEEK_STARTS.firstOrNull { it.name == name } }
            ?.let { settings.setWeekStart(it) }
        values.startScreen
            ?.let { name -> StartScreen.entries.firstOrNull { it.name == name } }
            ?.let { settings.setStartScreen(it) }
        val time = values.notifyTime?.let(NotifyTime::decode)
        if (time != null) {
            settings.setNotifyTime(time)
            onSettingsReplaced()
        }
    }
}
