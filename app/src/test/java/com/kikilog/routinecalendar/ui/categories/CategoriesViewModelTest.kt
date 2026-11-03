package com.kikilog.routinecalendar.ui.categories

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.ColorFamily
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.CategoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class CategoriesViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: CategoriesViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(AppDatabase.SeedOnCreate)
            .allowMainThreadQueries()
            .build()
        viewModel = CategoriesViewModel(CategoryRepository(db.categoryDao()))
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun categories(predicate: (CategoriesUiState) -> Boolean) = viewModel.state.first(predicate).categories

    @Test
    fun `새 분류를 더하고 이름과 색을 고친다`() = runTest {
        assertTrue(viewModel.save(null, "운동", null))
        val added = categories { it.categories.size == 5 }.last()
        assertEquals("운동", added.name)

        val navy = ColorFamily.NAVY.shades[1]
        assertTrue(viewModel.save(added.id, "헬스", navy))
        val edited = categories { list -> list.categories.any { it.name == "헬스" } }.single { it.id == added.id }
        assertEquals(navy, edited.colorArgb)
    }

    @Test
    fun `빈 이름은 저장하지 않는다`() = runTest {
        assertFalse(viewModel.save(null, "   ", null))
    }

    @Test
    fun `삭제를 누르면 든 항목 수로 확인 창, 확인하면 지워진다`() = runTest {
        val house = categories { !it.isLoading }.single { it.id == Category.HOUSE }
        viewModel.requestDelete(house)
        val pending = viewModel.state.first { it.pendingDelete != null }.pendingDelete!!
        assertEquals(7, pending.itemCount) // 기본 빠른 선택 집안일 7개

        viewModel.confirmDelete()
        val left = categories { list -> list.categories.none { it.id == Category.HOUSE } }
        assertEquals(3, left.size)
        assertNull(viewModel.state.value.pendingDelete)
    }

    @Test
    fun `기타는 삭제 확인 창도 뜨지 않는다`() = runTest {
        val etc = categories { !it.isLoading }.single { it.id == Category.ETC }
        viewModel.requestDelete(etc)
        assertNull(viewModel.state.first { !it.isLoading }.pendingDelete)
    }
}
