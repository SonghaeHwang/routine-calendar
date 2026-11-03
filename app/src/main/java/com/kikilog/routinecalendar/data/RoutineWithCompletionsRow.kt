package com.kikilog.routinecalendar.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Relation
import com.kikilog.routinecalendar.core.model.IntervalUnit

/** 루틴 하나와 그 완료 기록 전체. @Relation 은 순서를 보장하지 않으므로 쓰는 쪽에서 정렬한다. */
data class RoutineWithCompletionsRow(
    @Embedded val routine: RoutineEntity,
    @Relation(parentColumn = "id", entityColumn = "routine_id")
    val completions: List<CompletionEntity>,
)

/** 수정 화면에서 바꾸는 열만. first_due · created_at · archived 는 건드리지 않는다. */
data class RoutineEditColumns(
    val id: Long,
    val title: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "interval_amount") val intervalAmount: Int,
    @ColumnInfo(name = "interval_unit") val intervalUnit: IntervalUnit,
    val repeats: Boolean,
    val notify: Boolean,
)
