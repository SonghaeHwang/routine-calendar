package com.kikilog.routinecalendar.data

import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kikilog.routinecalendar.core.preset.DEFAULT_PRESETS

/** 처음 채우는 빠른 선택 14개(PLAN 프리셋 표) */
fun defaultPresetEntities(): List<PresetEntity> =
    DEFAULT_PRESETS.mapIndexed { index, preset ->
        PresetEntity(
            title = preset.title,
            category = preset.category,
            intervalAmount = preset.interval?.amount,
            intervalUnit = preset.interval?.unit,
            repeats = preset.repeats,
            position = index,
        )
    }

/** DAO 를 쓸 수 없는 곳(DB 생성 · 마이그레이션 직후)에서 기본값을 SQL 로 넣는다 */
fun SupportSQLiteDatabase.insertDefaultPresets() {
    defaultPresetEntities().forEach { p ->
        execSQL(
            "INSERT INTO preset (title, category, interval_amount, interval_unit, repeats, position) VALUES (?, ?, ?, ?, ?, ?)",
            arrayOf<Any?>(p.title, p.category.name, p.intervalAmount, p.intervalUnit?.name, if (p.repeats) 1 else 0, p.position),
        )
    }
}

/** 버전 1 → 2 자동 마이그레이션이 preset 테이블을 만든 뒤 기본값을 채운다. 기존 루틴 · 기록은 그대로다. */
class SeedDefaultPresetsSpec : AutoMigrationSpec {
    override fun onPostMigrate(db: SupportSQLiteDatabase) {
        db.insertDefaultPresets()
    }
}
