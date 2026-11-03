package com.kikilog.routinecalendar.data

import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.preset.Preset
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 빠른 선택 목록 저장소. 설정의 「빠른 선택 편집」 과 등록 화면이 함께 쓴다. */
class PresetRepository(private val dao: PresetDao) {

    fun observePresets(): Flow<List<Preset>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    /** 목록 끝에 더한다. @throws IllegalArgumentException 이름이 비었거나 주기가 범위를 벗어날 때 */
    suspend fun add(preset: Preset): Long {
        val valid = preset.validated()
        return dao.insert(valid.toEntity(position = dao.maxPosition() + 1))
    }

    /** 순서는 그대로 두고 값만 바꾼다. 바꿨으면 true */
    suspend fun update(preset: Preset): Boolean {
        val current = dao.byId(preset.id) ?: return false
        return dao.update(preset.validated().toEntity(position = current.position).copy(id = current.id)) > 0
    }

    suspend fun delete(id: Long): Boolean = dao.delete(id) > 0

    /** 다른 분류로 옮긴다(빠른 선택 편집에서 끌어 놓기). 그 분류의 끝에 보이도록 순서를 맨 뒤로 */
    suspend fun move(id: Long, categoryId: Long): Boolean {
        val current = dao.byId(id) ?: return false
        if (current.categoryId == categoryId) return false
        return dao.update(current.copy(categoryId = categoryId, position = dao.maxPosition() + 1)) > 0
    }

    /** 처음 14개로 되돌린다. 사용자가 더하거나 고친 항목은 사라진다 */
    suspend fun resetToDefaults() = dao.replaceAll(defaultPresetEntities())

    private fun Preset.validated(): Preset {
        val trimmed = title.trim()
        require(trimmed.isNotEmpty()) { "이름이 비어 있습니다" }
        require(trimmed.length <= MAX_TITLE_LENGTH) { "이름이 너무 깁니다" }
        interval?.let { require(it.amount <= MAX_AMOUNT) { "주기가 너무 깁니다: ${it.amount}" } }
        return copy(title = trimmed)
    }

    companion object {
        const val MAX_TITLE_LENGTH = 30
        const val MAX_AMOUNT = 365
    }
}

fun PresetEntity.toModel(): Preset = Preset(
    title = title,
    categoryId = categoryId,
    interval = if (intervalAmount != null && intervalUnit != null) Interval(intervalAmount, intervalUnit) else null,
    repeats = repeats,
    id = id,
)

fun Preset.toEntity(position: Int): PresetEntity = PresetEntity(
    id = id,
    title = title,
    categoryId = categoryId,
    intervalAmount = interval?.amount,
    intervalUnit = interval?.unit,
    repeats = repeats,
    position = position,
)
