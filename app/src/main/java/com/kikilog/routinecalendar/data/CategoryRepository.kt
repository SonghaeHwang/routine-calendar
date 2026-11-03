package com.kikilog.routinecalendar.data

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.autoCategoryColor
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** 분류 저장소. 분류 편집 화면 · 등록 화면 · 목록 화면(이름 · 색 표시)이 함께 쓴다 */
class CategoryRepository(private val dao: CategoryDao) {

    fun observeCategories(): Flow<List<Category>> = dao.observeAll().map { list -> list.map { it.toModel() } }

    /** 목록 끝에 더한다. 색을 주지 않으면 아직 안 쓴 계열에서 자동으로 고른다 */
    suspend fun add(name: String, colorArgb: Int? = null): Long {
        val trimmed = name.validated()
        val color = colorArgb ?: autoCategoryColor(dao.all().map { it.toModel() })
        return dao.insert(CategoryEntity(name = trimmed, color = color, position = dao.maxPosition() + 1, removable = true))
    }

    /** 이름 · 색을 바꾼다. 「기타」도 바꿀 수 있다 */
    suspend fun update(id: Long, name: String, colorArgb: Int): Boolean = dao.update(id, name.validated(), colorArgb) > 0

    /** 지울 때 경고가 필요한지(루틴 · 빠른 선택이 들어 있는지) */
    suspend fun itemCount(id: Long): Int = dao.itemCount(id)

    /** 든 항목을 「기타」로 옮기고 지운다. 「기타」는 지울 수 없다 */
    suspend fun delete(id: Long): Boolean = dao.deleteMovingTo(id, to = Category.ETC)

    private fun String.validated(): String {
        val trimmed = trim()
        require(trimmed.isNotEmpty()) { "이름이 비어 있습니다" }
        require(trimmed.length <= MAX_NAME_LENGTH) { "이름이 너무 깁니다" }
        return trimmed
    }

    companion object {
        const val MAX_NAME_LENGTH = 10
    }
}
