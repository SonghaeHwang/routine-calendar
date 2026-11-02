package com.kikilog.routinecalendar.data

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate

/** LocalDate 는 ISO 문자열("2026-11-02")로 저장한다. 길이가 일정해 문자열 비교가 곧 날짜 비교다. */
class Converters {
    @TypeConverter
    fun fromLocalDate(date: LocalDate): String = date.toString()

    @TypeConverter
    fun toLocalDate(value: String): LocalDate = LocalDate.parse(value)
}
