package com.kikilog.routinecalendar.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit

/** "10일마다" · "2개월 후 한 번" */
@Composable
fun intervalLabel(interval: Interval, repeats: Boolean): String {
    val res = when (interval.unit) {
        IntervalUnit.DAY -> if (repeats) R.string.interval_every_day else R.string.interval_once_day
        IntervalUnit.WEEK -> if (repeats) R.string.interval_every_week else R.string.interval_once_week
        IntervalUnit.MONTH -> if (repeats) R.string.interval_every_month else R.string.interval_once_month
    }
    return stringResource(res, interval.amount)
}

/** "3일 지남" */
@Composable
fun overdueLabel(days: Int): String = pluralStringResource(R.plurals.overdue_days, days, days)
