package com.kikilog.routinecalendar.data

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 스키마는 app/schemas 에 내보내 커밋한다. 테이블을 바꾸면 version 을 올리고 마이그레이션을 추가한다.
 * - 1: routine, completion
 * - 2: preset(빠른 선택 편집). 자동 마이그레이션 뒤 기본값을 채운다
 * - 3: category(분류 편집 · 색). routine · preset 의 분류가 이름 → id. 수동 마이그레이션 [MIGRATION_2_3]
 */
@Database(
    entities = [RoutineEntity::class, CompletionEntity::class, PresetEntity::class, CategoryEntity::class],
    version = 3,
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2, spec = SeedDefaultPresetsSpec::class)],
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao

    abstract fun presetDao(): PresetDao

    abstract fun categoryDao(): CategoryDao

    companion object {
        const val FILE_NAME = "routine-calendar.db"

        /** 새로 설치해 DB 를 처음 만들 때 분류 · 빠른 선택 기본값을 채운다 */
        val SeedOnCreate = object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                db.insertDefaultCategories()
                db.insertDefaultPresets()
            }
        }

        fun build(context: Context, fileName: String = FILE_NAME): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, fileName)
                .addCallback(SeedOnCreate)
                .addMigrations(MIGRATION_2_3)
                .build()
    }
}
