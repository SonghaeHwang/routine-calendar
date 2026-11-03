package com.kikilog.routinecalendar.ui.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate

// Material3 날짜 선택기는 날짜를 "UTC 자정 기준 밀리초" 로 주고받는다.
// 시간대를 거치지 않고 날짜 수(epoch day)로 바꿔야 하루 밀리지 않는다.

private const val MILLIS_PER_DAY = 86_400_000L

fun LocalDate.toPickerMillis(): Long = toJavaLocalDate().toEpochDay() * MILLIS_PER_DAY

fun pickerMillisToDate(utcMillis: Long): LocalDate =
    java.time.LocalDate.ofEpochDay(Math.floorDiv(utcMillis, MILLIS_PER_DAY)).toKotlinLocalDate()
