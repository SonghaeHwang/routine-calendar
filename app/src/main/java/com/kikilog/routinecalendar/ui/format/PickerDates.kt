package com.kikilog.routinecalendar.ui.format

import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.Instant
import java.time.ZoneId

// Material3 날짜 선택기는 날짜를 "UTC 자정 기준 밀리초" 로 주고받는다.
// 시간대를 거치지 않고 날짜 수(epoch day)로 바꿔야 하루 밀리지 않는다.

private const val MILLIS_PER_DAY = 86_400_000L

fun LocalDate.toPickerMillis(): Long = toJavaLocalDate().toEpochDay() * MILLIS_PER_DAY

fun pickerMillisToDate(utcMillis: Long): LocalDate =
    java.time.LocalDate.ofEpochDay(Math.floorDiv(utcMillis, MILLIS_PER_DAY)).toKotlinLocalDate()

/** 저장된 시각(created_at)을 기기 시간대의 날짜로. 선택기 밀리초와 달리 실제 시각이라 시간대를 거친다 */
fun epochMillisToLocalDate(epochMillis: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate().toKotlinLocalDate()
