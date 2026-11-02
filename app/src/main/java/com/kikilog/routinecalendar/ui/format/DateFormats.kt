package com.kikilog.routinecalendar.ui.format

import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number

// 날짜 글자는 Android 에 기대지 않는 순수 함수로 둬서 일반 JUnit 으로 시험한다.

private val DAY_NAMES = listOf("월", "화", "수", "목", "금", "토", "일")

/** "월" · "화" … */
fun dayOfWeekShort(dayOfWeek: DayOfWeek): String = DAY_NAMES[dayOfWeek.isoDayNumber - 1]

/** "11월 2일 (월)". [today] 와 해가 다르면 "2027년 1월 5일 (화)" */
fun formatDate(date: LocalDate, today: LocalDate): String {
    val monthDay = "${date.month.number}월 ${date.day}일 (${dayOfWeekShort(date.dayOfWeek)})"
    return if (date.year == today.year) monthDay else "${date.year}년 $monthDay"
}

/** 목록 오른쪽에 붙는 짧은 날짜 "11/6 (금)" */
fun formatShortDate(date: LocalDate): String =
    "${date.month.number}/${date.day} (${dayOfWeekShort(date.dayOfWeek)})"
