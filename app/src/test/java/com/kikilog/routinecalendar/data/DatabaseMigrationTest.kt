package com.kikilog.routinecalendar.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.preset.DEFAULT_PRESETS
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 버전 1 DB(3단계 13번까지 휴대폰에 설치된 형태)를 버전 2 로 열었을 때
 * 루틴 · 기록은 그대로 남고 빠른 선택 기본값이 채워지는지 확인한다.
 * 버전 1 의 CREATE 문은 app/schemas/.../1.json 과 같다.
 */
@RunWith(RobolectricTestRunner::class)
class DatabaseMigrationTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val name = "migration-test.db"

    @Before
    fun setUp() {
        context.deleteDatabase(name)
    }

    @After
    fun tearDown() {
        context.deleteDatabase(name)
    }

    private fun createVersion1WithData() {
        val file = context.getDatabasePath(name)
        file.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(file, null).use { db ->
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `routine` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                    "`category` TEXT NOT NULL, `interval_amount` INTEGER NOT NULL, `interval_unit` TEXT NOT NULL, `repeats` INTEGER NOT NULL, " +
                    "`first_due` TEXT NOT NULL, `notify` INTEGER NOT NULL, `archived` INTEGER NOT NULL, `created_at` INTEGER NOT NULL)",
            )
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `completion` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routine_id` INTEGER NOT NULL, " +
                    "`done_on` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                    "FOREIGN KEY(`routine_id`) REFERENCES `routine`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
            )
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_completion_routine_id_done_on` ON `completion` (`routine_id`, `done_on`)")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '3e807767385b398cde6c73abedda3001')")

            db.execSQL(
                "INSERT INTO routine (id, title, category, interval_amount, interval_unit, repeats, first_due, notify, archived, created_at) " +
                    "VALUES (1, '각질제거', 'BEAUTY', 10, 'DAY', 1, '2026-10-30', 1, 0, 1000)",
            )
            db.execSQL("INSERT INTO completion (routine_id, done_on, created_at) VALUES (1, '2026-10-20', 1000)")
            db.version = 1
        }
    }

    @Test
    fun `버전 1 에서 2 로 올려도 루틴과 기록은 그대로이고 빠른 선택 기본값이 채워진다`() = runTest {
        createVersion1WithData()

        val db = AppDatabase.build(context, name)
        try {
            val routine = db.routineDao().routine(1)!!
            assertEquals("각질제거", routine.title)
            assertEquals(LocalDate.parse("2026-10-30"), routine.firstDue)
            assertEquals(listOf(LocalDate.parse("2026-10-20")), db.routineDao().completionsOf(1).map { it.doneOn })

            assertEquals(DEFAULT_PRESETS.map { it.title }, db.presetDao().all().map { it.title })
        } finally {
            db.close()
        }
    }

    @Test
    fun `새로 만든 DB 에도 빠른 선택 기본값이 한 번만 채워진다`() = runTest {
        val db = AppDatabase.build(context, name)
        try {
            assertEquals(DEFAULT_PRESETS.size, db.presetDao().all().size)
        } finally {
            db.close()
        }
        // 다시 열어도 늘어나지 않는다
        val reopened = AppDatabase.build(context, name)
        try {
            assertEquals(DEFAULT_PRESETS.size, reopened.presetDao().all().size)
        } finally {
            reopened.close()
        }
    }
}
