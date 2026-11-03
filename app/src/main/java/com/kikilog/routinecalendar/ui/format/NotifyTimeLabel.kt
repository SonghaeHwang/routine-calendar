package com.kikilog.routinecalendar.ui.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.core.notify.NotifyTime

/** 「오전 8:00」 · 「오후 12:30」 · 「오전 12:00」(자정) */
@Composable
fun notifyTimeLabel(time: NotifyTime): String {
    val hour12 = (time.hour % 12).let { if (it == 0) 12 else it }
    val minute = time.minute.toString().padStart(2, '0')
    return stringResource(if (time.hour < 12) R.string.notify_time_am else R.string.notify_time_pm, hour12, minute)
}
