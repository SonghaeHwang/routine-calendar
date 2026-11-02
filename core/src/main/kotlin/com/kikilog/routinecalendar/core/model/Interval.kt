package com.kikilog.routinecalendar.core.model

import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

enum class IntervalUnit {
    DAY,
    WEEK,
    MONTH,
}

/** 반복 주기. "2개월" 은 `Interval(2, MONTH)`. */
data class Interval(val amount: Int, val unit: IntervalUnit) {
    init {
        require(amount >= 1) { "주기는 1 이상이어야 합니다: $amount" }
    }

    /** 주기의 [times] 배를 [date] 에 더한다. 월말은 그 달 마지막 날로 맞춘다(1/31 + 1개월 = 2/28). */
    fun addTo(date: LocalDate, times: Int = 1): LocalDate {
        val n = amount * times
        val period = when (unit) {
            IntervalUnit.DAY -> DatePeriod(days = n)
            IntervalUnit.WEEK -> DatePeriod(days = n * 7)
            IntervalUnit.MONTH -> DatePeriod(months = n)
        }
        return date.plus(period)
    }
}
