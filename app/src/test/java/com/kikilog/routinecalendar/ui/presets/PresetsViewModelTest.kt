package com.kikilog.routinecalendar.ui.presets

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.DEFAULT_PRESETS
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.PresetRepository
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PresetsViewModelTest {

    private lateinit var db: AppDatabase
    private lateinit var viewModel: PresetsViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(AppDatabase.SeedOnCreate)
            .allowMainThreadQueries()
            .build()
        viewModel = PresetsViewModel(PresetRepository(db.presetDao()))
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private suspend fun presets(predicate: (PresetsUiState) -> Boolean) = viewModel.state.first(predicate).presets

    @Test
    fun `처음에는 기본 14개`() = runTest {
        assertEquals(DEFAULT_PRESETS.map { it.title }, presets { !it.isLoading }.map { it.title })
    }

    @Test
    fun `새 항목을 저장하면 끝에 붙는다`() = runTest {
        assertTrue(viewModel.save(PresetForm(title = "손톱 정리", categoryId = Category.BEAUTY, amountText = "2", unit = IntervalUnit.WEEK)))
        assertEquals("손톱 정리", presets { it.presets.size == 15 }.last().title)
    }

    @Test
    fun `기존 항목의 주기를 고친다`() = runTest {
        val gel = presets { !it.isLoading }.single { it.title == "젤팩" }
        viewModel.save(PresetForm.from(gel).copy(amountText = "2"))

        val updated = presets { list -> list.presets.any { it.id == gel.id && it.interval?.amount == 2 } }
        assertEquals(Interval(2, IntervalUnit.WEEK), updated.single { it.id == gel.id }.interval)
    }

    @Test
    fun `검사를 통과하지 못하면 저장하지 않는다`() = runTest {
        assertFalse(viewModel.save(PresetForm(title = " ")))
        assertEquals(14, presets { !it.isLoading }.size)
    }

    @Test
    fun `지우고 기본값으로 되돌리면 14개로 돌아온다`() = runTest {
        val first = presets { !it.isLoading }.first()
        viewModel.delete(first.id)
        assertEquals(13, presets { it.presets.size == 13 }.size)

        viewModel.resetToDefaults()
        assertEquals(DEFAULT_PRESETS.map { it.title }, presets { it.presets.size == 14 }.map { it.title })
    }
}
