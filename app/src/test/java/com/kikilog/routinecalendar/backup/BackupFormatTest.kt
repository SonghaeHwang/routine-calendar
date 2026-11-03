package com.kikilog.routinecalendar.backup

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.data.CategoryEntity
import com.kikilog.routinecalendar.data.CompletionEntity
import com.kikilog.routinecalendar.data.PresetEntity
import com.kikilog.routinecalendar.data.RoutineEntity
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupFormatTest {

    private fun d(s: String) = LocalDate.parse(s)

    private val categories = listOf(
        CategoryEntity(1, "집안일", 0xFF2E7D32.toInt(), 0, removable = true),
        CategoryEntity(4, "기타", 0xFF757575.toInt(), 1, removable = false),
    )
    private val routine = RoutineEntity(
        id = 7, title = "각질제거", categoryId = 1, intervalAmount = 10, intervalUnit = IntervalUnit.DAY,
        repeats = true, firstDue = d("2026-10-30"), notify = true, archived = false, createdAt = 100,
    )
    private val completions = listOf(
        CompletionEntity(1, 7, d("2026-10-20"), 200),
        CompletionEntity(2, 7, d("2026-10-31"), 300),
    )
    private val presets = listOf(
        PresetEntity(1, "리들샷", 2, null, null, repeats = true, position = 0),
        PresetEntity(2, "청소기", 1, 1, IntervalUnit.WEEK, repeats = true, position = 1),
    )
    private val settings = BackupSettings("MONDAY", "ROUTINES", "21:30")

    private fun encoded() = encodeBackup(categories, listOf(routine), completions, presets, settings, exportedAt = 9, appVersion = "1.0")

    @Test
    fun `내보낸 글자를 다시 읽으면 같은 내용, id 는 빠진다`() {
        val data = decodeBackup(encoded())

        assertEquals(categories, data.categories)
        val item = data.routines.single()
        assertEquals(routine.copy(id = 0), item.routine)
        assertEquals(listOf(d("2026-10-20"), d("2026-10-31")), item.completions.map { it.doneOn })
        assertTrue(item.completions.all { it.id == 0L && it.routineId == 0L })
        assertEquals(listOf(200L, 300L), item.completions.map { it.createdAt })
        assertEquals(2, data.completionCount)
        assertEquals(settings, data.settings)
    }

    @Test
    fun `없는 분류를 가리키는 빠른 선택은 기타로, 직접 입력 주기는 그대로`() {
        val data = decodeBackup(encoded())

        assertEquals(Category.ETC, data.presets.first { it.title == "리들샷" }.categoryId)
        assertNull(data.presets.first { it.title == "리들샷" }.intervalAmount)
        assertEquals(IntervalUnit.WEEK, data.presets.first { it.title == "청소기" }.intervalUnit)
    }

    @Test
    fun `날짜는 ISO 문자열로 나간다`() {
        val text = encoded()
        assertTrue(text.contains("\"doneOn\": \"2026-10-20\""))
        assertTrue(text.contains("\"format\": \"routine-calendar-backup\""))
    }

    @Test
    fun `모르는 키는 무시한다`() {
        val text = encoded().replaceFirst("{", "{\n  \"futureField\": [1, 2, 3],")
        assertEquals(1, decodeBackup(text).routines.size)
    }

    @Test
    fun `새 판 파일은 거부한다`() {
        val text = encoded().replace("\"schemaVersion\": 1", "\"schemaVersion\": 2")
        val error = assertThrows(BackupFormatException::class.java) { decodeBackup(text) }
        assertEquals(BackupProblem.NEWER_VERSION, error.problem)
    }

    @Test
    fun `백업 파일이 아니면 거부한다`() {
        listOf(
            "그냥 글자",
            "{}",
            "[1, 2]",
            encoded().replace("routine-calendar-backup", "다른 앱"),
            encoded().replace("\"intervalAmount\": 10", "\"intervalAmount\": 0"),
            encoded().replace("\"intervalUnit\": \"DAY\"", "\"intervalUnit\": \"YEAR\""),
            encoded().replace("\"2026-10-20\"", "\"2026-13-40\""),
            encoded().replace("\"title\": \"각질제거\"", "\"title\": \"  \""),
        ).forEach { text ->
            val error = assertThrows(text, BackupFormatException::class.java) { decodeBackup(text) }
            assertEquals(BackupProblem.NOT_BACKUP, error.problem)
        }
    }

    @Test
    fun `같은 날 기록은 하나만 남긴다`() {
        val doubled = encodeBackup(
            categories, listOf(routine), completions + CompletionEntity(3, 7, d("2026-10-20"), 400), presets, settings, 9, "1.0",
        )
        assertEquals(listOf(d("2026-10-20"), d("2026-10-31")), decodeBackup(doubled).routines.single().completions.map { it.doneOn })
    }

    @Test
    fun `파일에 기타가 없으면 기본 기타를 더한다`() {
        val text = encodeBackup(categories.take(1), listOf(routine.copy(categoryId = 9)), emptyList(), emptyList(), settings, 9, "1.0")
        val data = decodeBackup(text)

        val etc = data.categories.single { !it.removable }
        assertEquals(Category.ETC, etc.id)
        assertEquals("기타", etc.name)
        assertEquals(Category.ETC, data.routines.single().routine.categoryId)
    }

    @Test
    fun `다른 번호의 기타는 ETC 번호로 맞춘다`() {
        val moved = listOf(categories[0], categories[1].copy(id = 12, name = "그 밖"))
        val text = encodeBackup(moved, listOf(routine.copy(categoryId = 12)), emptyList(), emptyList(), settings, 9, "1.0")
        val data = decodeBackup(text)

        assertEquals(Category.ETC, data.categories.single { !it.removable }.id)
        assertEquals("그 밖", data.categories.single { !it.removable }.name)
        assertEquals(Category.ETC, data.routines.single().routine.categoryId)
        assertFalse(data.categories.any { it.id == 12L })
    }

    @Test
    fun `너무 큰 파일은 거부한다`() {
        val error = assertThrows(BackupFormatException::class.java) { decodeBackup(" ".repeat(MAX_BACKUP_BYTES + 1)) }
        assertEquals(BackupProblem.TOO_LARGE, error.problem)
    }
}
