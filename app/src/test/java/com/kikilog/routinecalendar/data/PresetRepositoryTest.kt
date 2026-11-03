package com.kikilog.routinecalendar.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.DEFAULT_PRESETS
import com.kikilog.routinecalendar.core.preset.Preset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PresetRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: PresetRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(AppDatabase.SeedOnCreate)
            .allowMainThreadQueries()
            .build()
        repo = PresetRepository(db.presetDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun presets() = repo.observePresets().first()

    @Test
    fun `처음 만들면 기본 14개가 순서대로 채워진다`() = runTest {
        val list = presets()
        assertEquals(DEFAULT_PRESETS.map { it.copy(id = 0) }, list.map { it.copy(id = 0) })
        assertNull(list.single { it.title == "리들샷" }.interval)
    }

    @Test
    fun `내 항목을 더하면 목록 끝에 붙는다`() = runTest {
        repo.add(Preset("  손톱 정리 ", Category.BEAUTY, Interval(2, IntervalUnit.WEEK)))

        val last = presets().last()
        assertEquals("손톱 정리", last.title)
        assertEquals(Interval(2, IntervalUnit.WEEK), last.interval)
        assertEquals(15, presets().size)
    }

    @Test
    fun `기본 주기를 바꿔도 순서는 그대로다`() = runTest {
        val gel = presets().single { it.title == "젤팩" }
        val indexBefore = presets().indexOf(gel)

        assertTrue(repo.update(gel.copy(interval = Interval(2, IntervalUnit.WEEK))))

        val after = presets()
        assertEquals(indexBefore, after.indexOfFirst { it.id == gel.id })
        assertEquals(Interval(2, IntervalUnit.WEEK), after.single { it.id == gel.id }.interval)
    }

    @Test
    fun `주기를 비우면 직접 입력 항목이 된다`() = runTest {
        val gel = presets().single { it.title == "젤팩" }
        repo.update(gel.copy(interval = null))
        assertNull(presets().single { it.id == gel.id }.interval)
    }

    @Test
    fun `지우면 목록에서 빠지고, 없는 항목은 false`() = runTest {
        val id = presets().first().id
        assertTrue(repo.delete(id))
        assertEquals(13, presets().size)
        assertFalse(repo.delete(id))
        assertFalse(repo.update(Preset("없음", Category.ETC, null, id = 999)))
    }

    @Test
    fun `기본값으로 되돌리면 처음 14개로 돌아간다`() = runTest {
        presets().take(5).forEach { repo.delete(it.id) }
        repo.add(Preset("손톱 정리", Category.BEAUTY, null))

        repo.resetToDefaults()
        assertEquals(DEFAULT_PRESETS.map { it.title }, presets().map { it.title })
    }

    @Test
    fun `빈 이름과 너무 긴 주기는 거부`() = runTest {
        val blank = runCatching { repo.add(Preset("  ", Category.ETC, null)) }.exceptionOrNull()
        assertTrue(blank is IllegalArgumentException)
        val tooLong = runCatching { repo.add(Preset("긴 주기", Category.ETC, Interval(366, IntervalUnit.DAY))) }.exceptionOrNull()
        assertTrue(tooLong is IllegalArgumentException)
        assertEquals(14, presets().size)
    }
}
