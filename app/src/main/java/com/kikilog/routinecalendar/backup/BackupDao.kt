package com.kikilog.routinecalendar.backup

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.data.CategoryEntity
import com.kikilog.routinecalendar.data.CompletionEntity
import com.kikilog.routinecalendar.data.PresetEntity
import com.kikilog.routinecalendar.data.RoutineEntity

/** 내보내기용 네 테이블 전체. 한 트랜잭션에서 읽어 서로 맞는다 */
data class BackupDump(
    val categories: List<CategoryEntity>,
    val routines: List<RoutineEntity>,
    val completions: List<CompletionEntity>,
    val presets: List<PresetEntity>,
)

/** 합치기로 새로 더한 개수 */
data class MergeCounts(val routines: Int, val completions: Int, val categories: Int, val presets: Int)

/**
 * 백업 전용 DAO: 네 테이블 전체 읽기와, 한 트랜잭션으로 바꾸기(덮어쓰기 · 합치기).
 * 중간에 예외가 나면 전체가 되돌려져 지금 데이터가 그대로 남는다.
 */
@Dao
abstract class BackupDao {

    @Query("SELECT * FROM category ORDER BY position, id")
    abstract suspend fun categories(): List<CategoryEntity>

    @Query("SELECT * FROM routine ORDER BY id")
    abstract suspend fun routines(): List<RoutineEntity>

    @Query("SELECT * FROM completion ORDER BY routine_id, done_on")
    abstract suspend fun completions(): List<CompletionEntity>

    @Query("SELECT * FROM preset ORDER BY position, id")
    abstract suspend fun presets(): List<PresetEntity>

    @Transaction
    open suspend fun dump(): BackupDump = BackupDump(categories(), routines(), completions(), presets())

    @Query("DELETE FROM completion")
    protected abstract suspend fun deleteCompletions()

    @Query("DELETE FROM routine")
    protected abstract suspend fun deleteRoutines()

    @Query("DELETE FROM preset")
    protected abstract suspend fun deletePresets()

    @Query("DELETE FROM category")
    protected abstract suspend fun deleteCategories()

    @Insert
    protected abstract suspend fun insertCategory(category: CategoryEntity): Long

    @Insert
    protected abstract suspend fun insertRoutine(routine: RoutineEntity): Long

    /** 같은 루틴 · 같은 날이 이미 있으면 무시하고 -1 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    protected abstract suspend fun insertCompletion(completion: CompletionEntity): Long

    @Insert
    protected abstract suspend fun insertPreset(preset: PresetEntity): Long

    /**
     * 덮어쓰기: 네 테이블을 비우고 [data] 로 채운다. 분류는 파일 key 를 id 로 그대로 쓴다
     * (「기타」는 [decodeBackup] 이 [Category.ETC] 로 맞춰 두었다).
     */
    @Transaction
    open suspend fun replaceAll(data: BackupData) {
        deleteCompletions()
        deleteRoutines()
        deletePresets()
        deleteCategories()
        data.categories.forEach { insertCategory(it) }
        for (item in data.routines) {
            val id = insertRoutine(item.routine.copy(id = 0))
            item.completions.forEach { insertCompletion(it.copy(id = 0, routineId = id)) }
        }
        data.presets.forEach { insertPreset(it.copy(id = 0)) }
    }

    /**
     * 합치기. 지금 데이터는 지우거나 고치지 않고 더하기만 한다.
     * - 분류: 같은 이름이면 그 분류, 없으면 끝에 추가. 파일의 「기타」는 앱의 「기타」
     * - 루틴: 같은 이름(보관 포함)이면 같은 루틴으로 보고 기록만 더한다(같은 날은 하나). 설정은 지금 것. 없으면 추가
     * - 빠른 선택: 같은 이름이 없을 때만 끝에 추가
     */
    @Transaction
    open suspend fun merge(data: BackupData): MergeCounts {
        val existingCategories = categories()
        val categoryByName = existingCategories.associate { it.name to it.id }
        var nextCategoryPosition = (existingCategories.maxOfOrNull { it.position } ?: -1) + 1
        var addedCategories = 0
        val categoryIds = mutableMapOf<Long, Long>()
        for (category in data.categories.sortedBy { it.position }) {
            categoryIds[category.id] = when {
                !category.removable -> Category.ETC
                category.name in categoryByName -> categoryByName.getValue(category.name)
                else -> insertCategory(category.copy(id = 0, position = nextCategoryPosition++, removable = true))
                    .also { addedCategories++ }
            }
        }
        fun categoryOf(fileKey: Long) = categoryIds[fileKey] ?: Category.ETC

        // 같은 이름 루틴이 여럿이면 먼저 등록한 것
        val routineByTitle = routines().groupBy { it.title }.mapValues { (_, list) -> list.minOf { it.id } }.toMutableMap()
        var addedRoutines = 0
        var addedCompletions = 0
        for (item in data.routines) {
            // 파일 안에 같은 이름이 두 번 있어도 하나로 합친다
            val id = routineByTitle.getOrPut(item.routine.title) {
                insertRoutine(item.routine.copy(id = 0, categoryId = categoryOf(item.routine.categoryId)))
                    .also { addedRoutines++ }
            }
            for (completion in item.completions) {
                if (insertCompletion(completion.copy(id = 0, routineId = id)) != -1L) addedCompletions++
            }
        }

        val existingPresets = presets()
        val presetTitles = existingPresets.map { it.title }.toMutableSet()
        var nextPresetPosition = (existingPresets.maxOfOrNull { it.position } ?: -1) + 1
        var addedPresets = 0
        for (preset in data.presets.sortedBy { it.position }) {
            if (!presetTitles.add(preset.title)) continue
            insertPreset(preset.copy(id = 0, categoryId = categoryOf(preset.categoryId), position = nextPresetPosition++))
            addedPresets++
        }
        return MergeCounts(addedRoutines, addedCompletions, addedCategories, addedPresets)
    }
}
