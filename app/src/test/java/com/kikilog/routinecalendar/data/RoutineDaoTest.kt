package com.kikilog.routinecalendar.data

import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.IntervalUnit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoutineDaoTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: RoutineDao

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.routineDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun d(s: String) = LocalDate.parse(s)

    private fun routine(title: String = "각질제거", archived: Boolean = false) = RoutineEntity(
        title = title,
        categoryId = Category.BEAUTY,
        intervalAmount = 10,
        intervalUnit = IntervalUnit.DAY,
        repeats = true,
        firstDue = d("2026-11-02"),
        notify = true,
        archived = archived,
        createdAt = 1_000L,
    )

    private fun completion(routineId: Long, on: String) =
        CompletionEntity(routineId = routineId, doneOn = d(on), createdAt = 2_000L)

    // ── 완료 기준 ──

    @Test
    fun `체크하면 기록이 생긴다`() = runTest {
        val id = dao.insertRoutine(routine())
        assertTrue(dao.insertCompletion(completion(id, "2026-11-02")) > 0)

        val saved = dao.completionsOf(id)
        assertEquals(1, saved.size)
        assertEquals(d("2026-11-02"), saved.single().doneOn)
    }

    @Test
    fun `같은 날 두 번 체크는 무시된다`() = runTest {
        val id = dao.insertRoutine(routine())
        dao.insertCompletion(completion(id, "2026-11-02"))

        assertEquals(-1L, dao.insertCompletion(completion(id, "2026-11-02")))
        assertEquals(1, dao.completionsOf(id).size)
    }

    @Test
    fun `다른 날이나 다른 루틴은 같은 날짜라도 허용`() = runTest {
        val a = dao.insertRoutine(routine("a"))
        val b = dao.insertRoutine(routine("b"))
        dao.insertCompletion(completion(a, "2026-11-02"))

        assertTrue(dao.insertCompletion(completion(a, "2026-11-03")) > 0)
        assertTrue(dao.insertCompletion(completion(b, "2026-11-02")) > 0)
        assertEquals(2, dao.completionsOf(a).size)
        assertEquals(1, dao.completionsOf(b).size)
    }

    @Test
    fun `체크 취소는 그날 기록만 지운다`() = runTest {
        val id = dao.insertRoutine(routine())
        dao.insertCompletion(completion(id, "2026-10-23"))
        dao.insertCompletion(completion(id, "2026-11-02"))

        assertEquals(1, dao.deleteCompletion(id, d("2026-11-02")))
        assertEquals(listOf(d("2026-10-23")), dao.completionsOf(id).map { it.doneOn })
        // 없는 기록을 지우면 0
        assertEquals(0, dao.deleteCompletion(id, d("2026-11-02")))
    }

    @Test
    fun `루틴을 지우면 그 기록도 지워진다`() = runTest {
        val a = dao.insertRoutine(routine("a"))
        val b = dao.insertRoutine(routine("b"))
        dao.insertCompletion(completion(a, "2026-11-01"))
        dao.insertCompletion(completion(a, "2026-11-02"))
        dao.insertCompletion(completion(b, "2026-11-02"))

        assertEquals(1, dao.deleteRoutine(a))
        assertEquals(emptyList<CompletionEntity>(), dao.completionsOf(a))
        assertEquals(1, dao.completionsOf(b).size)
    }

    // ── 그 밖 ──

    @Test
    fun `없는 루틴에 체크하면 FK 위반`() = runTest {
        val error = runCatching { dao.insertCompletion(completion(999, "2026-11-02")) }.exceptionOrNull()
        assertTrue("SQLiteConstraintException 이어야 함: $error", error is SQLiteConstraintException)
    }

    @Test
    fun `보관한 루틴은 기본 조회에서 빠지고 기록은 남는다`() = runTest {
        val kept = dao.insertRoutine(routine("kept"))
        val archived = dao.insertRoutine(routine("archived"))
        dao.insertCompletion(completion(archived, "2026-11-01"))

        assertEquals(1, dao.setArchived(archived, true))

        assertEquals(listOf(kept), dao.observeWithCompletions(includeArchived = false).first().map { it.routine.id })
        assertEquals(listOf(kept, archived), dao.observeWithCompletions(includeArchived = true).first().map { it.routine.id })
        assertEquals(1, dao.completionsOf(archived).size)
    }

    @Test
    fun `루틴과 기록을 함께 조회`() = runTest {
        val id = dao.insertRoutine(routine())
        dao.insertCompletion(completion(id, "2026-11-02"))
        dao.insertCompletion(completion(id, "2026-10-23"))

        val row = dao.observeOneWithCompletions(id).first()
        assertNotNull(row)
        assertEquals("각질제거", row!!.routine.title)
        assertEquals(setOf(d("2026-10-23"), d("2026-11-02")), row.completions.map { it.doneOn }.toSet())
    }

    @Test
    fun `등록 트랜잭션 - 초기 완료일이 있으면 기록 1건, 없으면 0건`() = runTest {
        val withDone = dao.insertWithInitialCompletion(routine("a"), d("2026-10-20"), createdAt = 3_000L)
        val withoutDone = dao.insertWithInitialCompletion(routine("b"), null, createdAt = 3_000L)

        assertEquals(listOf(d("2026-10-20")), dao.completionsOf(withDone).map { it.doneOn })
        assertEquals(3_000L, dao.completionsOf(withDone).single().createdAt)
        assertEquals(emptyList<CompletionEntity>(), dao.completionsOf(withoutDone))
    }

    @Test
    fun `수정은 first_due · created_at · archived 를 바꾸지 않는다`() = runTest {
        val id = dao.insertRoutine(routine(archived = true))
        val edit = RoutineEditColumns(
            id = id,
            title = "젤팩",
            categoryId = Category.HOUSE,
            intervalAmount = 2,
            intervalUnit = IntervalUnit.WEEK,
            repeats = false,
            notify = false,
        )

        assertEquals(1, dao.updateRoutine(edit))

        val saved = dao.routine(id)!!
        assertEquals(
            routine(archived = true).copy(
                id = id,
                title = "젤팩",
                categoryId = Category.HOUSE,
                intervalAmount = 2,
                intervalUnit = IntervalUnit.WEEK,
                repeats = false,
                notify = false,
            ),
            saved,
        )
    }

    @Test
    fun `체크 · 취소 뒤 Flow 가 새 값을 내보낸다`() = runTest {
        val id = dao.insertRoutine(routine())
        val flow = dao.observeWithCompletions(includeArchived = false)
        assertEquals(0, flow.first().single().completions.size)

        dao.insertCompletion(completion(id, "2026-11-02"))
        assertEquals(1, flow.first().single().completions.size)

        dao.deleteCompletion(id, d("2026-11-02"))
        assertEquals(0, flow.first().single().completions.size)
    }
}
