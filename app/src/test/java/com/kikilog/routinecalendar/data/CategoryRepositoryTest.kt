package com.kikilog.routinecalendar.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.ColorFamily
import com.kikilog.routinecalendar.core.model.DEFAULT_CATEGORIES
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.Preset
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CategoryRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var categories: CategoryRepository
    private lateinit var routines: RoutineRepository
    private lateinit var presets: PresetRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(AppDatabase.SeedOnCreate)
            .allowMainThreadQueries()
            .build()
        categories = CategoryRepository(db.categoryDao())
        routines = RoutineRepository(db.routineDao(), today = { LocalDate.parse("2026-11-03") }, now = { 1_000L })
        presets = PresetRepository(db.presetDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `처음에는 기본 4개`() = runTest {
        assertEquals(DEFAULT_CATEGORIES, categories.observeCategories().first())
    }

    @Test
    fun `순서를 바꾸면 그 순서로 보이고, 빠진 분류는 뒤에 붙는다`() = runTest {
        val ids = DEFAULT_CATEGORIES.map { it.id }
        categories.reorder(listOf(ids[2], ids[0]))
        assertEquals(
            listOf(ids[2], ids[0], ids[1], ids[3]),
            categories.observeCategories().first().map { it.id },
        )
    }

    @Test
    fun `추가하면 끝에 붙고 색은 아직 안 쓴 계열에서 고른다`() = runTest {
        val id = categories.add("  운동 ")
        val added = categories.observeCategories().first().last()
        assertEquals(id, added.id)
        assertEquals("운동", added.name)
        val family = ColorFamily.of(added.colorArgb)
        assertNotNull(family)
        assertFalse(family in DEFAULT_CATEGORIES.map { ColorFamily.of(it.colorArgb) })
    }

    @Test
    fun `이름과 색을 바꾼다 - 기타도`() = runTest {
        val purple = ColorFamily.PURPLE.shades[0]
        assertTrue(categories.update(Category.ETC, "그 밖", purple))
        val etc = categories.observeCategories().first().single { it.id == Category.ETC }
        assertEquals("그 밖", etc.name)
        assertEquals(purple, etc.colorArgb)
    }

    @Test
    fun `빈 이름은 저장하지 않는다`() = runTest {
        assertTrue(runCatching { categories.add("  ") }.isFailure)
    }

    @Test
    fun `지우면 든 루틴과 빠른 선택이 기타로 옮겨진다`() = runTest {
        val routineId = routines.add(RoutineDraft("이불 세탁", Category.HOUSE, Interval(2, IntervalUnit.MONTH), repeats = true, notify = true), null)
        // 기본 빠른 선택 집안일 7개 + 루틴 1개
        assertEquals(8, categories.itemCount(Category.HOUSE))

        assertTrue(categories.delete(Category.HOUSE))

        assertFalse(categories.observeCategories().first().any { it.id == Category.HOUSE })
        assertEquals(Category.ETC, db.routineDao().routine(routineId)!!.categoryId)
        assertEquals(0, db.presetDao().all().count { it.categoryId == Category.HOUSE })
        assertEquals(7, presets.observePresets().first().count { it.categoryId == Category.ETC })
    }

    @Test
    fun `기타는 지울 수 없다`() = runTest {
        presets.add(Preset("여권", Category.ETC, null))
        assertFalse(categories.delete(Category.ETC))
        assertTrue(categories.observeCategories().first().any { it.id == Category.ETC })
    }

    @Test
    fun `빠른 선택을 다른 분류로 옮기면 그 분류의 끝에 놓인다`() = runTest {
        val gelPack = presets.observePresets().first().single { it.title == "젤팩" }
        assertTrue(presets.move(gelPack.id, Category.HOUSE))

        val house = presets.observePresets().first().filter { it.categoryId == Category.HOUSE }
        assertEquals("젤팩", house.last().title)
        assertFalse(presets.move(gelPack.id, Category.HOUSE))
    }
}
