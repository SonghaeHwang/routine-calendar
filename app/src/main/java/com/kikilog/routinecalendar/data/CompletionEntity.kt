package com.kikilog.routinecalendar.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

/**
 * `completion` 테이블. 루틴을 지우면 기록도 함께 지워진다.
 * `(routine_id, done_on)` 유일 인덱스로 같은 날 두 번 체크를 막고, FK 조회 인덱스도 겸한다.
 */
@Entity(
    tableName = "completion",
    foreignKeys = [
        ForeignKey(
            entity = RoutineEntity::class,
            parentColumns = ["id"],
            childColumns = ["routine_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["routine_id", "done_on"], unique = true)],
)
data class CompletionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "routine_id") val routineId: Long,
    @ColumnInfo(name = "done_on") val doneOn: LocalDate,
    /** epoch millis */
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
