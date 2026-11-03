package com.kikilog.routinecalendar.ui.edit

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.DEFAULT_PRESETS
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
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
class EditRoutineViewModelTest {

    private val today = LocalDate.parse("2026-11-02")
    private lateinit var db: AppDatabase
    private lateinit var repository: RoutineRepository
    private lateinit var viewModel: EditRoutineViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            // Room 의 백그라운드 스레드에서 Main 으로 돌아오는 동안 tearDown 의 resetMain 이 겹치면
            // 「Dispatchers.Main is used concurrently with setting it」 으로 가끔 실패해서, 시험에서는 같은 스레드로 돈다
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
        repository = RoutineRepository(db.routineDao(), today = { today }, now = { 1_000L })
        viewModel = EditRoutineViewModel(repository, today = { today })
    }

    @After
    fun tearDown() {
        db.close()
        Dispatchers.resetMain()
    }

    private val state get() = viewModel.state.value

    private fun preset(title: String) = DEFAULT_PRESETS.single { it.title == title }

    private suspend fun savedRoutines() = repository.observeRoutines(includeArchived = true).first()

    @Test
    fun `처음 값 - 빈 이름, 1주마다, 반복, 알림 켬, 오류는 아직 안 보임`() {
        assertEquals(today, state.today)
        assertEquals("", state.title)
        assertEquals("1", state.amountText)
        assertEquals(IntervalUnit.WEEK, state.unit)
        assertTrue(state.repeats)
        assertTrue(state.notify)
        assertEquals(today, state.startDate)
        assertFalse(state.showErrors)
    }

    @Test
    fun `등록 화면은 받은 빠른 선택 목록을 쓰고, 바뀌면 바로 반영한다`() {
        val presets = kotlinx.coroutines.flow.MutableStateFlow(DEFAULT_PRESETS.take(2))
        val vm = EditRoutineViewModel(repository, today = { today }, presets = presets)
        assertEquals(DEFAULT_PRESETS.take(2), vm.state.value.presets)

        val mine = com.kikilog.routinecalendar.core.preset.Preset("손톱 정리", Category.BEAUTY, null, id = 99)
        presets.value = listOf(mine)
        assertEquals(listOf(mine), vm.state.value.presets)
    }

    @Test
    fun `프리셋을 고르면 칸이 채워진다`() {
        viewModel.applyPreset(preset("이불 세탁"))

        assertEquals("이불 세탁", state.title)
        assertEquals(Category.HOUSE, state.categoryId)
        assertEquals("2", state.amountText)
        assertEquals(IntervalUnit.MONTH, state.unit)
        assertEquals(Interval(2, IntervalUnit.MONTH), state.interval)
    }

    @Test
    fun `주기 없는 프리셋은 숫자 칸을 비워 직접 입력하게 한다`() {
        viewModel.applyPreset(preset("리들샷"))

        assertEquals("리들샷", state.title)
        assertEquals(Category.BEAUTY, state.categoryId)
        assertEquals("", state.amountText)
        assertEquals(AmountError.INVALID, state.amountError)
    }

    @Test
    fun `주기 칸은 숫자만 세 자리까지`() {
        viewModel.onAmountChange("1a2")
        assertEquals("12", state.amountText)
        viewModel.onAmountChange("12345")
        assertEquals("123", state.amountText)
    }

    @Test
    fun `주기 검사 - 0 은 잘못, 365 는 통과, 366 은 너무 큼`() {
        viewModel.onAmountChange("0")
        assertEquals(AmountError.INVALID, state.amountError)
        viewModel.onAmountChange("365")
        assertNull(state.amountError)
        viewModel.onAmountChange("366")
        assertEquals(AmountError.TOO_LARGE, state.amountError)
    }

    @Test
    fun `빈 이름으로 저장하면 오류를 보여 주고 저장하지 않는다`() = runTest {
        viewModel.onTitleChange("   ")
        viewModel.save()

        assertTrue(state.showErrors)
        assertEquals(TitleError.EMPTY, state.titleError)
        assertFalse(state.saved)
        assertEquals(emptyList<Any>(), savedRoutines())
    }

    @Test
    fun `이름은 30자까지`() {
        viewModel.onTitleChange("가".repeat(40))
        assertEquals(30, state.title.length)
    }

    @Test
    fun `시작일은 지난 날 · 앞날 모두 고를 수 있다`() {
        viewModel.onStartDateChange(LocalDate.parse("2026-11-20"))
        assertEquals(LocalDate.parse("2026-11-20"), state.startDate)

        viewModel.onStartDateChange(LocalDate.parse("2026-10-20"))
        assertEquals(LocalDate.parse("2026-10-20"), state.startDate)
    }

    @Test
    fun `앞날을 시작일로 저장하면 그날이 첫 예정일이고 기록은 없다`() = runTest {
        viewModel.applyPreset(preset("젤팩"))
        viewModel.onStartDateChange(LocalDate.parse("2026-11-10"))
        viewModel.save()

        viewModel.state.first { it.saved }
        val saved = savedRoutines().single()
        assertEquals(LocalDate.parse("2026-11-10"), saved.routine.firstDue)
        assertTrue(saved.completions.isEmpty())
        assertEquals(LocalDate.parse("2026-11-10"), state.savedNextDue)
    }

    @Test
    fun `프리셋 + 지난 날 시작일로 저장하면 그날 한 것으로 보고 첫 예정일과 기록이 저장된다`() = runTest {
        viewModel.applyPreset(preset("각질제거"))
        viewModel.onStartDateChange(LocalDate.parse("2026-10-20"))
        viewModel.save()

        viewModel.state.first { it.saved }
        val saved = savedRoutines().single()
        assertEquals("각질제거", saved.routine.title)
        assertEquals(Interval(10, IntervalUnit.DAY), saved.routine.interval)
        assertEquals(LocalDate.parse("2026-10-30"), saved.routine.firstDue)
        assertEquals(listOf(LocalDate.parse("2026-10-20")), saved.completions.map { it.doneOn })
        // 저장 후 안내 문구에 쓸 다음 예정일
        assertEquals(LocalDate.parse("2026-10-30"), state.savedNextDue)
    }

    @Test
    fun `직접 입력해 반복 없이 저장하면 시작일이 첫 예정일`() = runTest {
        viewModel.onTitleChange("화장지")
        viewModel.onCategoryChange(Category.SUPPLY)
        viewModel.onAmountChange("2")
        viewModel.onUnitChange(IntervalUnit.MONTH)
        viewModel.onRepeatsChange(false)
        viewModel.onNotifyChange(false)
        viewModel.onStartDateChange(LocalDate.parse("2027-01-02"))
        viewModel.save()

        viewModel.state.first { it.saved }
        val saved = savedRoutines().single().routine
        assertEquals(Category.SUPPLY, saved.categoryId)
        assertFalse(saved.repeats)
        assertFalse(saved.notify)
        assertEquals(LocalDate.parse("2027-01-02"), saved.firstDue)
        assertEquals(LocalDate.parse("2027-01-02"), state.savedNextDue)
    }

    // ── 수정 모드 ──

    private suspend fun addRoutine(
        title: String,
        amount: Int,
        unit: IntervalUnit,
        repeats: Boolean = true,
        lastDoneOn: String? = null,
    ): Long = repository.add(
        com.kikilog.routinecalendar.data.RoutineDraft(title, Category.BEAUTY, Interval(amount, unit), repeats, notify = true),
        lastDoneOn?.let(LocalDate::parse),
    )

    private suspend fun editViewModel(id: Long): EditRoutineViewModel =
        EditRoutineViewModel(repository, today = { today }, routineId = id).also { vm -> vm.state.first { !it.isLoading } }

    @Test
    fun `수정 화면은 기존 값을 불러온다`() = runTest {
        val id = addRoutine("젤팩", 1, IntervalUnit.WEEK, lastDoneOn = "2026-11-01")
        val vm = editViewModel(id)

        val s = vm.state.value
        assertTrue(s.isEdit)
        assertEquals("젤팩", s.title)
        assertEquals(Category.BEAUTY, s.categoryId)
        assertEquals("1", s.amountText)
        assertEquals(IntervalUnit.WEEK, s.unit)
        assertTrue(s.repeats)
        assertTrue(s.hasCompletions)
        // 등록일은 저장소의 now(1000ms) 를 기기 시간대 날짜로 바꾼 값
        assertEquals(com.kikilog.routinecalendar.ui.format.epochMillisToLocalDate(1_000L), s.createdOn)
    }

    @Test
    fun `기록이 있는 반복 루틴은 반복 스위치가 잠긴다`() = runTest {
        val id = addRoutine("젤팩", 1, IntervalUnit.WEEK, lastDoneOn = "2026-11-01")
        val vm = editViewModel(id)

        assertTrue(vm.state.value.repeatsLocked)
        vm.onRepeatsChange(false)
        assertTrue(vm.state.value.repeats)
    }

    @Test
    fun `기록이 없으면 반복도 바꿀 수 있고 처음 할 날을 고쳐 저장한다`() = runTest {
        val id = addRoutine("화장지", 2, IntervalUnit.MONTH, repeats = false)
        val vm = editViewModel(id)
        assertFalse(vm.state.value.repeatsLocked)
        assertEquals(LocalDate.parse("2027-01-02"), vm.state.value.firstDue)

        vm.onAmountChange("3")
        vm.onFirstDueChange(LocalDate.parse("2027-02-02"))
        vm.save()
        vm.state.first { it.saved }

        val saved = savedRoutines().single().routine
        assertEquals(Interval(3, IntervalUnit.MONTH), saved.interval)
        assertEquals(LocalDate.parse("2027-02-02"), saved.firstDue)
    }

    @Test
    fun `기록이 있으면 처음 할 날은 바뀌지 않고 주기와 이름만 저장된다`() = runTest {
        val id = addRoutine("젤팩", 1, IntervalUnit.WEEK, lastDoneOn = "2026-11-01")
        val vm = editViewModel(id)

        vm.onFirstDueChange(LocalDate.parse("2026-12-25"))
        vm.onTitleChange("젤팩 마스크")
        vm.onAmountChange("2")
        vm.save()
        vm.state.first { it.saved }

        val saved = savedRoutines().single()
        assertEquals("젤팩 마스크", saved.routine.title)
        assertEquals(Interval(2, IntervalUnit.WEEK), saved.routine.interval)
        assertEquals(LocalDate.parse("2026-11-08"), saved.routine.firstDue)
        assertEquals(listOf(LocalDate.parse("2026-11-01")), saved.completions.map { it.doneOn })
    }

    @Test
    fun `수정 화면 메뉴로 보관하면 보관되고 화면이 닫힌다`() = runTest {
        val id = addRoutine("젤팩", 1, IntervalUnit.WEEK)
        val vm = editViewModel(id)
        assertFalse(vm.state.value.archived)

        vm.onToggleArchive()
        assertTrue(vm.state.first { it.closed }.archived)
        assertTrue(savedRoutines().single().routine.archived)
    }

    @Test
    fun `수정 화면 메뉴로 삭제하면 지워지고 화면이 닫힌다`() = runTest {
        val id = addRoutine("젤팩", 1, IntervalUnit.WEEK, lastDoneOn = "2026-11-01")
        val vm = editViewModel(id)

        vm.onDelete()
        vm.state.first { it.closed }
        assertEquals(emptyList<Any>(), savedRoutines())
    }

    // ── 같은 이름 ──

    @Test
    fun `같은 이름을 입력하면 안내하고, 앞뒤 공백은 무시한다`() = runTest {
        val id = addRoutine("젤팩", 1, IntervalUnit.WEEK)
        viewModel.state.first { it.others.isNotEmpty() }

        viewModel.onTitleChange(" 젤팩 ")
        assertEquals(id, state.duplicate?.id)
        viewModel.onTitleChange("젤팩 마스크")
        assertNull(state.duplicate)
    }

    @Test
    fun `등록할 때 같은 이름이면 확인 창을 띄우고 저장하지 않는다`() = runTest {
        addRoutine("젤팩", 1, IntervalUnit.WEEK)
        viewModel.state.first { it.others.isNotEmpty() }

        viewModel.applyPreset(DEFAULT_PRESETS.single { it.title == "젤팩" })
        viewModel.save()

        assertTrue(state.askDuplicate)
        assertFalse(state.saved)
        assertEquals(1, savedRoutines().size)
    }

    @Test
    fun `그래도 추가를 고르면 같은 이름으로 하나 더 저장된다`() = runTest {
        addRoutine("젤팩", 1, IntervalUnit.WEEK)
        viewModel.state.first { it.others.isNotEmpty() }
        viewModel.onTitleChange("젤팩")
        viewModel.save()

        viewModel.onAddAnyway()
        viewModel.state.first { it.saved }
        assertEquals(listOf("젤팩", "젤팩"), savedRoutines().map { it.routine.title })
    }

    @Test
    fun `기존 루틴 열기를 고르면 그 루틴으로 가고 새로 저장하지 않는다`() = runTest {
        val id = addRoutine("젤팩", 1, IntervalUnit.WEEK)
        viewModel.state.first { it.others.isNotEmpty() }
        viewModel.onTitleChange("젤팩")
        viewModel.save()

        viewModel.onOpenExisting()
        assertEquals(id, state.openExistingId)
        assertFalse(state.askDuplicate)
        assertEquals(1, savedRoutines().size)
    }

    @Test
    fun `보관한 루틴과 이름이 같아도 알려 준다`() = runTest {
        val id = addRoutine("젤팩", 1, IntervalUnit.WEEK)
        repository.setArchived(id, true)
        viewModel.state.first { s -> s.others.any { it.archived } }

        viewModel.onTitleChange("젤팩")
        assertTrue(state.duplicate!!.archived)
    }

    @Test
    fun `수정 중에는 자기 이름은 겹침이 아니고, 다른 루틴 이름으로 바꿔도 창 없이 저장된다`() = runTest {
        addRoutine("젤팩", 1, IntervalUnit.WEEK)
        val id = addRoutine("모델링팩", 1, IntervalUnit.WEEK)
        val vm = editViewModel(id)
        vm.state.first { it.others.isNotEmpty() }
        assertNull(vm.state.value.duplicate)

        vm.onTitleChange("젤팩")
        assertTrue(vm.state.value.duplicate != null)
        vm.save()
        vm.state.first { it.saved }
        assertFalse(vm.state.value.askDuplicate)
    }

    @Test
    fun `없는 루틴을 열면 notFound`() = runTest {
        val vm = EditRoutineViewModel(repository, today = { today }, routineId = 999)
        assertTrue(vm.state.first { !it.isLoading }.notFound)
    }
}
