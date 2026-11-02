package com.kikilog.routinecalendar.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.IntervalUnit
import kotlinx.datetime.LocalDate

/** `routine` 테이블. enum 은 Room 기본 처리로 이름(TEXT)을 저장한다. */
@Entity(tableName = "routine")
data class RoutineEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val category: Category,
    @ColumnInfo(name = "interval_amount") val intervalAmount: Int,
    @ColumnInfo(name = "interval_unit") val intervalUnit: IntervalUnit,
    val repeats: Boolean,
    @ColumnInfo(name = "first_due") val firstDue: LocalDate,
    val notify: Boolean,
    val archived: Boolean,
    /** epoch millis */
    @ColumnInfo(name = "created_at") val createdAt: Long,
)
