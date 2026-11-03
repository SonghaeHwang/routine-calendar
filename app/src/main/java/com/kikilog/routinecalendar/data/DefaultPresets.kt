package com.kikilog.routinecalendar.data

import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.DEFAULT_CATEGORIES
import com.kikilog.routinecalendar.core.preset.DEFAULT_PRESETS

/** 처음 채우는 빠른 선택 14개(PLAN 프리셋 표) */
fun defaultPresetEntities(): List<PresetEntity> =
    DEFAULT_PRESETS.mapIndexed { index, preset ->
        PresetEntity(
            title = preset.title,
            categoryId = preset.categoryId,
            intervalAmount = preset.interval?.amount,
            intervalUnit = preset.interval?.unit,
            repeats = preset.repeats,
            position = index,
        )
    }

/** DAO 를 쓸 수 없는 곳(DB 생성 · 마이그레이션 직후)에서 기본 분류 4개를 SQL 로 넣는다(현재 스키마) */
fun SupportSQLiteDatabase.insertDefaultCategories() {
    DEFAULT_CATEGORIES.forEach { c ->
        execSQL(
            "INSERT INTO category (id, name, color, position, removable) VALUES (?, ?, ?, ?, ?)",
            arrayOf<Any?>(c.id, c.name, c.colorArgb, c.position, if (c.removable) 1 else 0),
        )
    }
}

/** DAO 를 쓸 수 없는 곳(DB 생성)에서 빠른 선택 기본값을 SQL 로 넣는다(현재 스키마) */
fun SupportSQLiteDatabase.insertDefaultPresets() {
    defaultPresetEntities().forEach { p ->
        execSQL(
            "INSERT INTO preset (title, category_id, interval_amount, interval_unit, repeats, position) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(p.title, p.categoryId, p.intervalAmount, p.intervalUnit?.name, if (p.repeats) 1 else 0, p.position),
        )
    }
}

/** 버전 2 스키마(분류를 이름 TEXT 로 저장)의 기본 분류 이름. 1 → 2 마이그레이션에서만 쓴다 */
private val V2_CATEGORY_NAMES = mapOf(
    Category.HOUSE to "HOUSE",
    Category.BEAUTY to "BEAUTY",
    Category.SUPPLY to "SUPPLY",
    Category.ETC to "ETC",
)

/** 버전 1 → 2 자동 마이그레이션이 preset 테이블을 만든 뒤 기본값을 채운다(버전 2 스키마). 기존 루틴 · 기록은 그대로다. */
class SeedDefaultPresetsSpec : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        defaultPresetEntities().forEach { p ->
            db.execSQL(
                "INSERT INTO preset (title, category, interval_amount, interval_unit, repeats, position) VALUES (?, ?, ?, ?, ?, ?)",
                arrayOf<Any?>(p.title, V2_CATEGORY_NAMES.getValue(p.categoryId), p.intervalAmount, p.intervalUnit?.name, if (p.repeats) 1 else 0, p.position),
            )
        }
    }
}

/**
 * 버전 2 → 3: 분류를 사용자가 고칠 수 있도록 `category` 테이블을 만들고,
 * routine · preset 의 `category`(이름 TEXT) 를 `category_id`(INTEGER) 로 바꾼다.
 *
 * 테이블을 지울 때 외래 키(completion → routine, ON DELETE CASCADE)가 켜져 있으면 기록이 함께 지워질 수 있어,
 * 루틴 · 기록을 백업 테이블로 복사한 뒤 completion → routine 순서로 지우고 다시 만든다.
 * CREATE 문은 app/schemas/.../3.json 과 같아야 한다.
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `category` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, " +
                "`color` INTEGER NOT NULL, `position` INTEGER NOT NULL, `removable` INTEGER NOT NULL)",
        )
        db.insertDefaultCategories()

        val categoryIdOf = "CASE category WHEN 'HOUSE' THEN ${Category.HOUSE} WHEN 'BEAUTY' THEN ${Category.BEAUTY} " +
            "WHEN 'SUPPLY' THEN ${Category.SUPPLY} ELSE ${Category.ETC} END"

        // 루틴 · 기록 백업 (외래 키 없음)
        db.execSQL("CREATE TABLE routine_backup AS SELECT * FROM routine")
        db.execSQL("CREATE TABLE completion_backup AS SELECT * FROM completion")
        db.execSQL("DROP TABLE completion")
        db.execSQL("DROP TABLE routine")

        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `routine` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                "`category_id` INTEGER NOT NULL, `interval_amount` INTEGER NOT NULL, `interval_unit` TEXT NOT NULL, " +
                "`repeats` INTEGER NOT NULL, `first_due` TEXT NOT NULL, `notify` INTEGER NOT NULL, `archived` INTEGER NOT NULL, " +
                "`created_at` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO routine (id, title, category_id, interval_amount, interval_unit, repeats, first_due, notify, archived, created_at) " +
                "SELECT id, title, $categoryIdOf, interval_amount, interval_unit, repeats, first_due, notify, archived, created_at " +
                "FROM routine_backup",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `completion` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `routine_id` INTEGER NOT NULL, " +
                "`done_on` TEXT NOT NULL, `created_at` INTEGER NOT NULL, " +
                "FOREIGN KEY(`routine_id`) REFERENCES `routine`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )",
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_completion_routine_id_done_on` ON `completion` (`routine_id`, `done_on`)")
        db.execSQL(
            "INSERT INTO completion (id, routine_id, done_on, created_at) SELECT id, routine_id, done_on, created_at FROM completion_backup",
        )
        db.execSQL("DROP TABLE completion_backup")
        db.execSQL("DROP TABLE routine_backup")

        // 빠른 선택 (외래 키 없음)
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `preset_new` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, " +
                "`category_id` INTEGER NOT NULL, `interval_amount` INTEGER, `interval_unit` TEXT, `repeats` INTEGER NOT NULL, " +
                "`position` INTEGER NOT NULL)",
        )
        db.execSQL(
            "INSERT INTO preset_new (id, title, category_id, interval_amount, interval_unit, repeats, position) " +
                "SELECT id, title, $categoryIdOf, interval_amount, interval_unit, repeats, position FROM preset",
        )
        db.execSQL("DROP TABLE preset")
        db.execSQL("ALTER TABLE preset_new RENAME TO preset")
    }
}
