package com.kikilog.routinecalendar.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kikilog.routinecalendar.core.model.IntervalUnit

/**
 * `preset` 테이블(DB 버전 2). 빠른 선택 목록. 주기 두 열이 모두 null 이면 직접 입력.
 * @property position 화면에 보이는 순서(작을수록 앞)
 */
@Entity(tableName = "preset")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    @ColumnInfo(name = "category_id") val categoryId: Long,
    @ColumnInfo(name = "interval_amount") val intervalAmount: Int?,
    @ColumnInfo(name = "interval_unit") val intervalUnit: IntervalUnit?,
    val repeats: Boolean,
    val position: Int,
)
