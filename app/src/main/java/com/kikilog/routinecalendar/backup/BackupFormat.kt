package com.kikilog.routinecalendar.backup

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.DEFAULT_CATEGORIES
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.data.CategoryEntity
import com.kikilog.routinecalendar.data.CategoryRepository
import com.kikilog.routinecalendar.data.CompletionEntity
import com.kikilog.routinecalendar.data.PresetEntity
import com.kikilog.routinecalendar.data.RoutineEntity
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

// 백업 JSON 파일 형식. DB 엔티티와 따로 둬서 저장 형식(Room)과 파일 형식을 나눈다.
// DB id 는 넣지 않는다 — 기록은 루틴 안에 넣고, 분류는 파일 안에서만 쓰는 key 로 가리킨다.

const val BACKUP_FORMAT = "routine-calendar-backup"

/** 파일 형식 판. DB 판과 따로 올린다. 이 앱이 읽을 수 있는 가장 새 판 */
const val BACKUP_SCHEMA_VERSION = 1

/** 이보다 큰 파일은 백업 파일로 보지 않는다(루틴 수백 개 · 기록 수천 개도 1MB 안팎) */
const val MAX_BACKUP_BYTES = 5 * 1024 * 1024

private const val MAX_INTERVAL_AMOUNT = 365

@Serializable
data class BackupFile(
    val format: String,
    val schemaVersion: Int,
    /** epoch millis */
    val exportedAt: Long,
    val appVersion: String = "",
    val settings: SettingsDto = SettingsDto(),
    val categories: List<CategoryDto> = emptyList(),
    val routines: List<RoutineDto> = emptyList(),
    val presets: List<PresetDto> = emptyList(),
)

/**
 * 덮어쓰기 때 쓰는 설정. 값이 없거나 알 수 없으면 지금 앱 값을 그대로 둔다.
 * 알림 켜기는 넣지 않는다 — 새 휴대폰에서는 권한을 다시 받아야 하므로.
 */
@Serializable
data class SettingsDto(
    val weekStart: String? = null,
    val startScreen: String? = null,
    /** "HH:mm" */
    val notifyTime: String? = null,
)

@Serializable
data class CategoryDto(
    val key: Long,
    val name: String,
    /** 0xAARRGGBB */
    val color: Int,
    val position: Int,
    val removable: Boolean = true,
)

@Serializable
data class RoutineDto(
    val title: String,
    val categoryKey: Long,
    val intervalAmount: Int,
    val intervalUnit: String,
    val repeats: Boolean,
    /** ISO 날짜 "2026-11-03" */
    val firstDue: String,
    val notify: Boolean,
    val archived: Boolean,
    val createdAt: Long,
    val completions: List<CompletionDto> = emptyList(),
)

@Serializable
data class CompletionDto(
    /** ISO 날짜 */
    val doneOn: String,
    val createdAt: Long,
)

@Serializable
data class PresetDto(
    val title: String,
    val categoryKey: Long,
    /** 둘 다 null 이면 주기는 등록할 때 직접 입력 */
    val intervalAmount: Int? = null,
    val intervalUnit: String? = null,
    val repeats: Boolean,
    val position: Int,
)

/** 같은 판 안에서 필드가 늘어도 옛 앱이 읽을 수 있게 모르는 키는 무시한다 */
val BackupJson = Json {
    prettyPrint = true
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/** 가져올 수 없는 이유. 화면이 이유별 문구를 보인다 */
enum class BackupProblem { NOT_BACKUP, NEWER_VERSION, TOO_LARGE }

class BackupFormatException(val problem: BackupProblem, message: String, cause: Throwable? = null) :
    Exception(message, cause)

/** 가져오기 설정. null 이면 그 값은 바꾸지 않는다 */
data class BackupSettings(
    val weekStart: String?,
    val startScreen: String?,
    val notifyTime: String?,
)

/** 루틴 하나와 그 기록. 루틴 · 기록의 id 는 0(넣을 때 새로 받음), 분류 id 는 파일의 key */
data class BackupRoutine(val routine: RoutineEntity, val completions: List<CompletionEntity>)

/**
 * 검사를 통과한 백업 내용. DB 에 넣기 바로 전 모양이다.
 * 분류 id 는 파일 key 그대로이고, 「기타」는 늘 [Category.ETC] 로 맞춰 둔다.
 */
data class BackupData(
    val categories: List<CategoryEntity>,
    val routines: List<BackupRoutine>,
    val presets: List<PresetEntity>,
    val settings: BackupSettings,
) {
    val completionCount: Int get() = routines.sumOf { it.completions.size }
}

/** 현재 DB 내용을 파일 형식으로 */
fun encodeBackup(
    categories: List<CategoryEntity>,
    routines: List<RoutineEntity>,
    completions: List<CompletionEntity>,
    presets: List<PresetEntity>,
    settings: BackupSettings,
    exportedAt: Long,
    appVersion: String,
): String {
    val byRoutine = completions.groupBy { it.routineId }
    val file = BackupFile(
        format = BACKUP_FORMAT,
        schemaVersion = BACKUP_SCHEMA_VERSION,
        exportedAt = exportedAt,
        appVersion = appVersion,
        settings = SettingsDto(settings.weekStart, settings.startScreen, settings.notifyTime),
        categories = categories.map { CategoryDto(it.id, it.name, it.color, it.position, it.removable) },
        routines = routines.map { r ->
            RoutineDto(
                title = r.title,
                categoryKey = r.categoryId,
                intervalAmount = r.intervalAmount,
                intervalUnit = r.intervalUnit.name,
                repeats = r.repeats,
                firstDue = r.firstDue.toString(),
                notify = r.notify,
                archived = r.archived,
                createdAt = r.createdAt,
                completions = byRoutine[r.id].orEmpty().sortedBy { it.doneOn }
                    .map { CompletionDto(it.doneOn.toString(), it.createdAt) },
            )
        },
        presets = presets.map { p ->
            PresetDto(p.title, p.categoryId, p.intervalAmount, p.intervalUnit?.name, p.repeats, p.position)
        },
    )
    return BackupJson.encodeToString(BackupFile.serializer(), file)
}

/**
 * 파일 글자를 읽어 검사한다. DB 를 건드리기 전에 전부 확인하고, 하나라도 틀리면 예외.
 * - 루틴 · 빠른 선택이 없는 분류를 가리키면 「기타」로
 * - 같은 루틴의 같은 날 기록은 하나만
 * @throws BackupFormatException
 */
fun decodeBackup(text: String): BackupData {
    if (text.length > MAX_BACKUP_BYTES) throw BackupFormatException(BackupProblem.TOO_LARGE, "파일이 너무 큼")
    val file = try {
        BackupJson.decodeFromString(BackupFile.serializer(), text)
    } catch (e: SerializationException) {
        throw notBackup("JSON 형식이 아님", e)
    } catch (e: IllegalArgumentException) {
        throw notBackup("JSON 형식이 아님", e)
    }
    if (file.format != BACKUP_FORMAT) throw notBackup("format 이 다름: ${file.format}")
    if (file.schemaVersion > BACKUP_SCHEMA_VERSION) {
        throw BackupFormatException(BackupProblem.NEWER_VERSION, "새 판 파일: ${file.schemaVersion}")
    }
    if (file.schemaVersion < 1) throw notBackup("schemaVersion 이 잘못됨: ${file.schemaVersion}")

    val categories = decodeCategories(file.categories)
    val known = categories.map { it.id }.toSet()
    fun categoryOf(key: Long) = if (key in known) key else Category.ETC

    val routines = file.routines.map { dto ->
        val title = dto.title.trim().ifEmpty { throw notBackup("루틴 이름이 비었음") }
        BackupRoutine(
            routine = RoutineEntity(
                title = title,
                categoryId = categoryOf(dto.categoryKey),
                intervalAmount = dto.intervalAmount.validAmount(),
                intervalUnit = unitOf(dto.intervalUnit),
                repeats = dto.repeats,
                firstDue = dateOf(dto.firstDue),
                notify = dto.notify,
                archived = dto.archived,
                createdAt = dto.createdAt,
            ),
            completions = dto.completions
                .map { CompletionEntity(routineId = 0, doneOn = dateOf(it.doneOn), createdAt = it.createdAt) }
                .distinctBy { it.doneOn }
                .sortedBy { it.doneOn },
        )
    }

    val presets = file.presets.map { dto ->
        val title = dto.title.trim().ifEmpty { throw notBackup("빠른 선택 이름이 비었음") }
        if ((dto.intervalAmount == null) != (dto.intervalUnit == null)) throw notBackup("빠른 선택 주기가 반만 있음")
        PresetEntity(
            title = title,
            categoryId = categoryOf(dto.categoryKey),
            intervalAmount = dto.intervalAmount?.validAmount(),
            intervalUnit = dto.intervalUnit?.let(::unitOf),
            repeats = dto.repeats,
            position = dto.position,
        )
    }

    return BackupData(
        categories = categories,
        routines = routines,
        presets = presets,
        settings = BackupSettings(file.settings.weekStart, file.settings.startScreen, file.settings.notifyTime),
    )
}

/**
 * 분류 검사. 지울 수 없는 분류(「기타」)는 하나뿐이고 늘 [Category.ETC] 로 맞춘다.
 * 파일에 「기타」가 없으면 기본 「기타」를 더한다(없는 분류를 가리키는 항목이 옮겨 갈 곳).
 */
private fun decodeCategories(dtos: List<CategoryDto>): List<CategoryEntity> {
    if (dtos.map { it.key }.toSet().size != dtos.size) throw notBackup("분류 key 가 겹침")
    if (dtos.count { !it.removable } > 1) throw notBackup("지울 수 없는 분류가 둘 이상")
    val etcKey = dtos.firstOrNull { !it.removable }?.key
    // 「기타」가 아닌 분류가 ETC 번호를 쓰고 있으면 바꿔 줄 수 없으므로 거부한다(이 앱이 만든 파일에서는 생기지 않음)
    if (etcKey != null && etcKey != Category.ETC && dtos.any { it.key == Category.ETC }) {
        throw notBackup("분류 번호가 맞지 않음")
    }
    val categories = dtos.map { dto ->
        val name = dto.name.trim()
        if (name.isEmpty() || name.length > CategoryRepository.MAX_NAME_LENGTH) throw notBackup("분류 이름이 잘못됨")
        CategoryEntity(
            id = if (dto.key == etcKey) Category.ETC else dto.key,
            name = name,
            color = dto.color,
            position = dto.position,
            removable = dto.removable,
        )
    }
    if (etcKey != null) return categories
    val etc = DEFAULT_CATEGORIES.first { it.id == Category.ETC }
    if (categories.any { it.id == Category.ETC }) throw notBackup("분류 번호가 맞지 않음")
    return categories + CategoryEntity(
        id = Category.ETC,
        name = etc.name,
        color = etc.colorArgb,
        position = (categories.maxOfOrNull { it.position } ?: -1) + 1,
        removable = false,
    )
}

private fun Int.validAmount(): Int {
    if (this !in 1..MAX_INTERVAL_AMOUNT) throw notBackup("주기가 범위를 벗어남: $this")
    return this
}

private fun unitOf(name: String): IntervalUnit =
    IntervalUnit.entries.firstOrNull { it.name == name } ?: throw notBackup("모르는 주기 단위: $name")

private fun dateOf(text: String): LocalDate =
    try {
        LocalDate.parse(text)
    } catch (e: IllegalArgumentException) {
        throw notBackup("날짜가 잘못됨: $text", e)
    }

private fun notBackup(message: String, cause: Throwable? = null) =
    BackupFormatException(BackupProblem.NOT_BACKUP, message, cause)
