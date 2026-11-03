package com.kikilog.routinecalendar.backup

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.notify.NotifyTime
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.CategoryRepository
import com.kikilog.routinecalendar.data.RoutineDraft
import com.kikilog.routinecalendar.data.RoutineRepository
import com.kikilog.routinecalendar.data.SettingsRepository
import com.kikilog.routinecalendar.data.StartScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class BackupRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val today = LocalDate.parse("2026-11-03")
    private val databases = mutableListOf<AppDatabase>()

    @After
    fun tearDown() {
        databases.forEach { it.close() }
    }

    /** 휴대폰 한 대: DB(기본 분류 · 빠른 선택 채움) + 설정 + 백업 저장소 */
    private inner class Phone(scope: TestScope, name: String) {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDatabase::class.java)
            .addCallback(AppDatabase.SeedOnCreate)
            .allowMainThreadQueries()
            .build()
            .also { databases += it }
        val routines = RoutineRepository(db.routineDao(), today = { today }, now = { 1L })
        val categories = CategoryRepository(db.categoryDao())
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope.backgroundScope, produceFile = { folder.newFile("$name.preferences_pb") }),
        )
        val snapshot = File(folder.root, "$name-before-import.json")
        var settingsSynced = 0
        val backup = BackupRepository(db.backupDao(), settings, snapshot, now = { 9L }, appVersion = "1.0", onSettingsReplaced = { settingsSynced++ })

        suspend fun add(title: String, category: Long = Category.HOUSE, amount: Int = 1, unit: IntervalUnit = IntervalUnit.WEEK, start: String = "2026-11-03", repeats: Boolean = true): Long =
            routines.addStartingOn(RoutineDraft(title, category, Interval(amount, unit), repeats, notify = true), LocalDate.parse(start)).first

        /** 비교용: 이름 → (분류 이름, 주기, 처음 할 날, 보관, 기록 날짜) */
        suspend fun summary(): Map<String, List<Any>> {
            val names = categories.observeCategories().first().associate { it.id to it.name }
            return routines.observeRoutines(includeArchived = true).first().associate { item ->
                item.routine.title to listOf(
                    names[item.routine.categoryId].orEmpty(),
                    item.routine.interval,
                    item.routine.firstDue,
                    item.routine.archived,
                    item.completions.map { it.doneOn.toString() },
                )
            }
        }

        suspend fun categoryNames() = categories.observeCategories().first().map { it.name to it.colorArgb }
        suspend fun presetTitles() = db.presetDao().all().map { it.title }
    }

    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun `내보내고 새 휴대폰에 덮어쓰면 같은 상태`() = runTest {
        val old = Phone(this, "old")
        val wash = old.add("빨래", start = "2026-10-20")
        old.routines.check(wash, d("2026-10-27"))
        val gel = old.add("젤팩", category = Category.BEAUTY, amount = 10, unit = IntervalUnit.DAY)
        old.routines.setArchived(gel, true)
        old.add("화장지", category = Category.SUPPLY, amount = 2, unit = IntervalUnit.MONTH, start = "2026-12-01", repeats = false)
        val sport = old.categories.add("운동")
        old.add("헬스", category = sport, amount = 2, unit = IntervalUnit.DAY)
        old.categories.update(Category.ETC, "그 밖", 0xFF123456.toInt())
        old.settings.setWeekStart(DayOfWeek.MONDAY)
        old.settings.setStartScreen(StartScreen.ROUTINES)
        old.settings.setNotifyTime(NotifyTime(21, 30))

        val text = old.backup.exportText()
        val new = Phone(this, "new")
        new.add("지울 루틴")
        val outcome = new.backup.import(new.backup.parse(text), ImportMode.REPLACE)

        assertEquals(old.summary(), new.summary())
        assertEquals(old.categoryNames(), new.categoryNames())
        assertEquals(old.presetTitles(), new.presetTitles())
        assertEquals(DayOfWeek.MONDAY, new.settings.weekStart.first())
        assertEquals(StartScreen.ROUTINES, new.settings.startScreen.first())
        assertEquals(NotifyTime(21, 30), new.settings.notifyTime.first())
        assertEquals(1, new.settingsSynced)
        assertEquals(ImportOutcome.Replaced(routines = 4, completions = 2), outcome)
        // 덮어쓴 뒤에도 기타는 지울 수 없다
        assertFalse(new.categories.delete(Category.ETC))
    }

    @Test
    fun `합치기 - 같은 이름 루틴은 기록만 합치고 설정은 지금 것`() = runTest {
        val file = Phone(this, "file")
        val fileWash = file.add("빨래", amount = 2)
        file.routines.check(fileWash, d("2026-10-10"))
        file.routines.check(fileWash, d("2026-10-20"))
        file.add("젤팩", category = Category.BEAUTY)
        file.settings.setWeekStart(DayOfWeek.MONDAY)

        val phone = Phone(this, "phone")
        val wash = phone.add("빨래")
        phone.routines.check(wash, d("2026-10-20"))

        val outcome = phone.backup.import(phone.backup.parse(file.backup.exportText()), ImportMode.MERGE)

        val summary = phone.summary()
        assertEquals(setOf("빨래", "젤팩"), summary.keys)
        // 빨래: 주기 · 처음 할 날은 지금 것(1주), 기록은 합침(같은 날은 하나)
        assertEquals(Interval(1, IntervalUnit.WEEK), summary.getValue("빨래")[1])
        assertEquals(listOf("2026-10-10", "2026-10-20"), summary.getValue("빨래")[4])
        assertEquals(MergeCounts(routines = 1, completions = 1, categories = 0, presets = 0), (outcome as ImportOutcome.Merged).counts)
        assertEquals(DayOfWeek.SUNDAY, phone.settings.weekStart.first())
        assertEquals(0, phone.settingsSynced)
    }

    @Test
    fun `합치기 - 분류는 이름으로 맞추고 없으면 추가, 파일의 기타는 앱의 기타`() = runTest {
        val file = Phone(this, "file")
        val sport = file.categories.add("운동")
        file.add("헬스", category = sport)
        file.categories.update(Category.ETC, "그 밖", 0xFF123456.toInt())
        file.add("아무거나", category = Category.ETC)
        file.db.presetDao().insert(file.db.presetDao().all().first().copy(id = 0, title = "내 항목", categoryId = sport))

        val phone = Phone(this, "phone")
        val outcome = phone.backup.import(phone.backup.parse(file.backup.exportText()), ImportMode.MERGE) as ImportOutcome.Merged

        val summary = phone.summary()
        assertEquals("운동", summary.getValue("헬스")[0])
        assertEquals("기타", summary.getValue("아무거나")[0])
        assertEquals(listOf("집안일", "뷰티", "구매", "기타", "운동"), phone.categoryNames().map { it.first })
        assertEquals(1, outcome.counts.categories)
        // 빠른 선택은 같은 이름이 없는 것만
        assertEquals(1, outcome.counts.presets)
        assertTrue("내 항목" in phone.presetTitles())
        assertEquals(phone.presetTitles().toSet().size, phone.presetTitles().size)
    }

    @Test
    fun `백업 파일이 아니면 아무것도 바뀌지 않는다`() = runTest {
        val phone = Phone(this, "phone")
        phone.add("빨래")
        val before = phone.summary()

        assertThrows(BackupFormatException::class.java) { phone.backup.parse("{\"format\": \"other\"}") }

        assertEquals(before, phone.summary())
        assertFalse(phone.backup.hasSnapshot.value)
    }

    @Test
    fun `가져오기 전으로 되돌리기`() = runTest {
        val phone = Phone(this, "phone")
        val wash = phone.add("빨래", start = "2026-10-20")
        phone.routines.check(wash, d("2026-10-27"))
        phone.settings.setStartScreen(StartScreen.ROUTINES)
        val before = phone.summary()

        val file = Phone(this, "file")
        file.add("젤팩")
        phone.backup.import(phone.backup.parse(file.backup.exportText()), ImportMode.REPLACE)
        assertTrue(phone.backup.hasSnapshot.value)
        assertEquals(StartScreen.CALENDAR, phone.settings.startScreen.first())

        assertTrue(phone.backup.restoreSnapshot())

        assertEquals(before, phone.summary())
        assertEquals(StartScreen.ROUTINES, phone.settings.startScreen.first())
        assertFalse(phone.backup.hasSnapshot.value)
        assertFalse(phone.snapshot.exists())
        assertFalse(phone.backup.restoreSnapshot())
    }
}
