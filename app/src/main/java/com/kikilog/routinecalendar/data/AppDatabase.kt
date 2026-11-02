package com.kikilog.routinecalendar.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

/** 스키마는 app/schemas 에 내보내 커밋한다. 테이블을 바꾸면 version 을 올리고 마이그레이션을 추가한다. */
@Database(
    entities = [RoutineEntity::class, CompletionEntity::class],
    version = 1,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        private const val FILE_NAME = "routine-calendar.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, FILE_NAME).build()
    }
}
