package com.kikilog.routinecalendar.widget

import android.content.Context
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasText
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.RoutineDraft
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TodayWidgetTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val today = LocalDate.parse("2026-11-03")

    private fun row(id: Long, title: String, overdue: Int = 0, done: Boolean = false) =
        WidgetRow(id, title, colorArgb = null, overdueDays = overdue, doneToday = done)

    // --- 그리기 ---

    @Test
    fun `넓은 크기에는 날짜 머리글 · 남은 개수 · N일 지남`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(250.dp, 150.dp))
        provideComposable {
            TodayWidgetContent(WidgetState(today, listOf(row(1, "각질제거", overdue = 3), row(2, "모델링팩")), next = null, hasAnyRoutine = true))
        }

        onNode(hasText("11월 3일 (화)")).assertExists()
        onNode(hasText("2개 남음")).assertExists()
        onNode(hasText("각질제거")).assertExists()
        onNode(hasText("3일 지남")).assertExists()
    }

    @Test
    fun `좁은 크기에는 짧은 날짜와 짧은 지남 표시`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(110.dp, 110.dp))
        provideComposable {
            TodayWidgetContent(WidgetState(today, listOf(row(1, "각질제거", overdue = 3)), next = null, hasAnyRoutine = true))
        }

        onNode(hasText("11/3 (화)")).assertExists()
        onNode(hasText("+3일")).assertExists()
    }

    @Test
    fun `오늘 할 것이 없으면 안내와 다음 예정`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        setAppWidgetSize(DpSize(250.dp, 150.dp))
        provideComposable {
            TodayWidgetContent(
                WidgetState(today, emptyList(), next = WidgetNext("이불 세탁", LocalDate.parse("2026-11-06")), hasAnyRoutine = true),
            )
        }

        onNode(hasText("오늘 할 루틴이 없어요")).assertExists()
        onNode(hasText("다음: 이불 세탁 · 11/6 (금)")).assertExists()
    }

    @Test
    fun `등록한 루틴이 없으면 등록 안내`() = runGlanceAppWidgetUnitTest {
        setContext(context)
        provideComposable { TodayWidgetContent(WidgetState(today, emptyList(), next = null, hasAnyRoutine = false)) }

        onNode(hasText("앱에서 루틴을 등록해 보세요")).assertExists()
    }

    // --- 체크 칸 ---

    @Test
    fun `체크 칸 파라미터에 루틴 id · 그린 날 · 체크 여부`() {
        val params = toggleParameters(row(7, "빨래", done = true), today)

        assertEquals(7L, params[RoutineIdKey])
        assertEquals("2026-11-03", params[DateKey])
        assertEquals(true, params[DoneTodayKey])
    }

    @Test
    fun `체크 안 된 줄은 체크, 체크된 줄은 취소`() = runTest {
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        try {
            val repo = RoutineRepository(db.routineDao(), today = { today }, now = { 1L })
            val (id, _) = repo.addStartingOn(
                RoutineDraft("빨래", Category.HOUSE, Interval(1, IntervalUnit.WEEK), repeats = true, notify = true),
                start = today,
            )

            assertTrue(toggleRoutine(repo, id, today, doneToday = false))
            assertEquals(listOf(today), repo.observeRoutine(id).first()!!.completions.map { it.doneOn })

            assertTrue(toggleRoutine(repo, id, today, doneToday = true))
            assertTrue(repo.observeRoutine(id).first()!!.completions.isEmpty())
        } finally {
            db.close()
        }
    }
}
