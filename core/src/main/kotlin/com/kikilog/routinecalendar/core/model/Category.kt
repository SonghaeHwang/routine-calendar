package com.kikilog.routinecalendar.core.model

/**
 * 루틴 분류. 사용자가 추가 · 이름 변경 · 색 변경 · 삭제할 수 있다(DB 버전 3).
 * 루틴과 빠른 선택은 [id] 로 분류를 가리킨다.
 *
 * @property colorArgb 0xAARRGGBB. [ColorFamily] 의 색 중 하나
 * @property position 화면에 보이는 순서(작을수록 앞)
 * @property removable false 면 지울 수 없다(「기타」 — 지운 분류의 항목이 옮겨 가는 곳)
 */
data class Category(
    val id: Long,
    val name: String,
    val colorArgb: Int,
    val position: Int,
    val removable: Boolean = true,
) {
    companion object {
        /** 처음 채우는 분류의 id. DB 버전 2 의 이름(HOUSE · BEAUTY · SUPPLY · ETC)이 이 번호로 옮겨진다 */
        const val HOUSE = 1L
        const val BEAUTY = 2L
        const val SUPPLY = 3L
        const val ETC = 4L
    }
}

/** 처음 채우는 분류 4개 */
val DEFAULT_CATEGORIES: List<Category> = listOf(
    Category(Category.HOUSE, "집안일", ColorFamily.GREEN.defaultColor, position = 0),
    Category(Category.BEAUTY, "뷰티", ColorFamily.PINK.defaultColor, position = 1),
    Category(Category.SUPPLY, "구매", ColorFamily.BLUE.defaultColor, position = 2),
    Category(Category.ETC, "기타", ColorFamily.GRAY.defaultColor, position = 3, removable = false),
)

/**
 * 새 분류의 색. 아직 아무 분류도 쓰지 않은 계열의 가운데 단계를 차례로 고른다.
 * 모든 계열을 쓰고 있으면 분류 수에 따라 돌아가며 고른다.
 */
fun autoCategoryColor(existing: Collection<Category>): Int {
    val usedFamilies = existing.mapNotNull { ColorFamily.of(it.colorArgb) }.toSet()
    val free = ColorFamily.entries.firstOrNull { it !in usedFamilies && it != ColorFamily.GRAY }
    return (free ?: ColorFamily.entries[existing.size % ColorFamily.entries.size]).defaultColor
}
