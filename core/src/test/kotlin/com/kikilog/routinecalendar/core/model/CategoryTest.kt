package com.kikilog.routinecalendar.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryTest {

    @Test
    fun `기본 분류 4개 - 기타만 지울 수 없다`() {
        assertEquals(listOf("집안일", "뷰티", "구매", "기타"), DEFAULT_CATEGORIES.map { it.name })
        assertEquals(listOf(Category.HOUSE, Category.BEAUTY, Category.SUPPLY, Category.ETC), DEFAULT_CATEGORIES.map { it.id })
        assertEquals(listOf(Category.ETC), DEFAULT_CATEGORIES.filterNot { it.removable }.map { it.id })
    }

    @Test
    fun `기본 색 - 집안일 초록 · 뷰티 분홍 · 구매 파랑 · 기타 회색`() {
        assertEquals(
            listOf(ColorFamily.GREEN, ColorFamily.PINK, ColorFamily.BLUE, ColorFamily.GRAY),
            DEFAULT_CATEGORIES.map { ColorFamily.of(it.colorArgb) },
        )
    }

    @Test
    fun `색 계열은 진한 색부터 5단계, 모두 불투명`() {
        ColorFamily.entries.forEach { family ->
            assertEquals(5, family.shades.size)
            assertTrue(family.shades.all { (it ushr 24) == 0xFF })
        }
        assertEquals(0xFFB71C1C.toInt(), ColorFamily.RED.shades.first())
        assertNull(ColorFamily.of(0x12345678))
    }

    @Test
    fun `새 분류는 아직 안 쓴 계열의 가운데 색`() {
        val color = autoCategoryColor(DEFAULT_CATEGORIES)
        val family = ColorFamily.of(color)!!
        assertFalse(family in DEFAULT_CATEGORIES.map { ColorFamily.of(it.colorArgb) })
        assertEquals(family.defaultColor, color)
        // 회색은 기타 몫이라 자동으로는 고르지 않는다
        assertEquals(ColorFamily.RED.defaultColor, autoCategoryColor(emptyList()))
    }

    @Test
    fun `모든 계열을 쓰고 있어도 색을 정한다`() {
        val all = ColorFamily.entries.mapIndexed { i, f -> Category(i.toLong() + 1, "분류$i", f.defaultColor, i) }
        assertTrue(ColorFamily.of(autoCategoryColor(all)) != null)
    }
}
