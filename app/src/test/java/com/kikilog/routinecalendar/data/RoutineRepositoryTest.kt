package com.kikilog.routinecalendar.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.schedule.nextDue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
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

@RunWith(RobolectricTestRunner::class)
class RoutineRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: RoutineRepository

    private val today = LocalDate.parse("2026-11-02")
    private val now = 5_000L

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = RoutineRepository(db.routineDao(), today = { today }, now = { now })
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun d(s: String) = LocalDate.parse(s)

    private fun draft(
        title: String = "각질제거",
        amount: Int = 10,
        unit: IntervalUnit = IntervalUnit.DAY,
        repeats: Boolean = true,
    ) = RoutineDraft(title, Category.BEAUTY, Interval(amount, unit), repeats, notify = true)

    private suspend fun loaded(id: Long) = repo.observeRoutine(id).first()!!

    // ── 등록 · 마지막으로 한 날 ──

    @Test
    fun `반복 루틴에 마지막으로 한 날을 넣으면 첫 예정일은 그날 + 주기, 기록도 남는다`() = runTest {
        val id = repo.add(draft(), lastDoneOn = d("2026-10-20"))

        val saved = loaded(id)
        assertEquals(d("2026-10-30"), saved.routine.firstDue)
        assertEquals(listOf(d("2026-10-20")), saved.completions.map { it.doneOn })
        assertEquals(d("2026-10-30"), nextDue(saved.routine, saved.completions))
    }

    @Test
    fun `반복 없는 루틴은 마지막으로 한 날을 넣어도 기록을 남기지 않는다`() = runTest {
        val id = repo.add(draft(title = "화장지", amount = 2, unit = IntervalUnit.MONTH, repeats = false), lastDoneOn = d("2026-10-15"))

        val saved = loaded(id)
        assertEquals(d("2026-12-15"), saved.routine.firstDue)
        assertEquals(emptyList<Any>(), saved.completions)
        assertEquals(d("2026-12-15"), nextDue(saved.routine, saved.completions))
    }

    @Test
    fun `마지막으로 한 날을 비우면 반복은 오늘, 반복 없음은 오늘 + 주기`() = runTest {
        val repeating = repo.add(draft(), lastDoneOn = null)
        val once = repo.add(draft(title = "화장지", amount = 2, unit = IntervalUnit.MONTH, repeats = false), lastDoneOn = null)

        assertEquals(today, loaded(repeating).routine.firstDue)
        assertEquals(d("2027-01-02"), loaded(once).routine.firstDue)
        assertEquals(emptyList<Any>(), loaded(repeating).completions)
    }

    @Test
    fun `마지막으로 한 날이 미래면 거부하고 아무것도 저장하지 않는다`() = runTest {
        val error = runCatching { repo.add(draft(), lastDoneOn = d("2026-11-03")) }.exceptionOrNull()

        assertTrue(error is IllegalArgumentException)
        assertEquals(emptyList<Any>(), repo.observeRoutines(includeArchived = true).first())
    }

    @Test
    fun `빈 제목은 거부, 앞뒤 공백은 지운다`() = runTest {
        val error = runCatching { repo.add(draft(title = "   "), lastDoneOn = null) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)

        val id = repo.add(draft(title = "  모델링팩 "), lastDoneOn = null)
        assertEquals("모델링팩", loaded(id).routine.title)
    }

    @Test
    fun `등록 시각과 기록 시각은 now 값`() = runTest {
        val id = repo.add(draft(), lastDoneOn = d("2026-10-20"))

        assertEquals(now, db.routineDao().routine(id)!!.createdAt)
        assertEquals(now, db.routineDao().completionsOf(id).single().createdAt)
    }

    // ── 체크 · 취소 ──

    @Test
    fun `체크는 처음이면 true, 같은 날 다시 하면 false`() = runTest {
        val id = repo.add(draft(), lastDoneOn = null)

        assertTrue(repo.check(id))
        assertFalse(repo.check(id))
        assertEquals(listOf(today), loaded(id).completions.map { it.doneOn })
    }

    @Test
    fun `지난 날짜로 체크할 수 있고 미래 날짜는 거부`() = runTest {
        val id = repo.add(draft(), lastDoneOn = null)

        assertTrue(repo.check(id, d("2026-10-30")))
        val error = runCatching { repo.check(id, d("2026-11-03")) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals(listOf(d("2026-10-30")), loaded(id).completions.map { it.doneOn })
    }

    @Test
    fun `체크 취소하면 다음 예정일이 이전 기록 기준으로 돌아간다`() = runTest {
        val id = repo.add(draft(), lastDoneOn = d("2026-10-20"))
        repo.check(id, today)
        loaded(id).let { assertEquals(d("2026-11-12"), nextDue(it.routine, it.completions)) }

        assertTrue(repo.uncheck(id, today))
        loaded(id).let { assertEquals(d("2026-10-30"), nextDue(it.routine, it.completions)) }

        assertFalse(repo.uncheck(id, today))
    }

    // ── 수정 · 보관 · 삭제 ──

    @Test
    fun `수정은 첫 예정일과 기록을 그대로 둔다`() = runTest {
        val id = repo.add(draft(), lastDoneOn = d("2026-10-20"))

        assertTrue(repo.update(id, draft(title = "젤팩", amount = 1, unit = IntervalUnit.WEEK)))

        val saved = loaded(id)
        assertEquals("젤팩", saved.routine.title)
        assertEquals(Interval(1, IntervalUnit.WEEK), saved.routine.interval)
        assertEquals(d("2026-10-30"), saved.routine.firstDue)
        assertEquals(listOf(d("2026-10-20")), saved.completions.map { it.doneOn })
        // 다음 예정일은 새 주기로 다시 계산된다
        assertEquals(d("2026-10-27"), nextDue(saved.routine, saved.completions))
    }

    @Test
    fun `기록이 없으면 수정하면서 처음 할 날을 바꿸 수 있다`() = runTest {
        val id = repo.add(draft(title = "화장지", amount = 2, unit = IntervalUnit.MONTH, repeats = false), lastDoneOn = null)
        assertEquals(d("2027-01-02"), loaded(id).routine.firstDue)

        assertTrue(
            repo.update(id, draft(title = "화장지", amount = 3, unit = IntervalUnit.MONTH, repeats = false), firstDue = d("2027-02-02")),
        )
        assertEquals(d("2027-02-02"), loaded(id).routine.firstDue)
    }

    @Test
    fun `기록이 있으면 처음 할 날은 바뀌지 않는다`() = runTest {
        val id = repo.add(draft(), lastDoneOn = d("2026-10-20"))

        assertTrue(repo.update(id, draft(title = "젤팩"), firstDue = d("2026-12-01")))
        assertEquals("젤팩", loaded(id).routine.title)
        assertEquals(d("2026-10-30"), loaded(id).routine.firstDue)
    }

    @Test
    fun `기록이 있는 반복 루틴의 반복을 끄면 거부하고 바꾸지 않는다`() = runTest {
        val id = repo.add(draft(), lastDoneOn = d("2026-10-20"))

        val error = runCatching { repo.update(id, draft(title = "바뀜 이름", repeats = false)) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertEquals("각질제거", loaded(id).routine.title)
        assertTrue(loaded(id).routine.repeats)
    }

    @Test
    fun `기록이 없으면 반복을 끌 수 있고, 없는 루틴 수정은 false`() = runTest {
        val id = repo.add(draft(), lastDoneOn = null)

        assertTrue(repo.update(id, draft(repeats = false)))
        assertFalse(loaded(id).routine.repeats)
        assertFalse(repo.update(999, draft()))
    }

    @Test
    fun `보관하면 기본 조회에서 빠지고 되돌릴 수 있다`() = runTest {
        val id = repo.add(draft(), lastDoneOn = null)

        assertTrue(repo.setArchived(id, true))
        assertEquals(emptyList<Any>(), repo.observeRoutines().first())
        assertEquals(listOf(id), repo.observeRoutines(includeArchived = true).first().map { it.routine.id })

        assertTrue(repo.setArchived(id, false))
        assertEquals(listOf(id), repo.observeRoutines().first().map { it.routine.id })
    }

    @Test
    fun `알림 넣기를 여러 개 한꺼번에 끄고 켠다`() = runTest {
        val a = repo.add(draft(), lastDoneOn = null)
        val b = repo.add(draft(), lastDoneOn = null)

        assertEquals(2, repo.setNotify(listOf(a, b), false))
        assertFalse(loaded(a).routine.notify)
        assertFalse(loaded(b).routine.notify)

        repo.setNotify(listOf(b), true)
        assertFalse(loaded(a).routine.notify)
        assertTrue(loaded(b).routine.notify)
        assertEquals(0, repo.setNotify(emptyList(), true))
    }

    @Test
    fun `삭제하면 기록도 사라진다`() = runTest {
        val id = repo.add(draft(), lastDoneOn = d("2026-10-20"))
        repo.check(id)

        assertTrue(repo.delete(id))
        assertNull(repo.observeRoutine(id).first())
        assertEquals(emptyList<CompletionEntity>(), db.routineDao().completionsOf(id))
        assertFalse(repo.delete(id))
    }
}
