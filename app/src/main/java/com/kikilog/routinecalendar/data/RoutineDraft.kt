package com.kikilog.routinecalendar.data

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval

/** 등록 · 수정 화면에서 받는 값. id · firstDue · archived 는 리포지토리가 정한다. */
data class RoutineDraft(
    val title: String,
    val category: Category,
    val interval: Interval,
    val repeats: Boolean,
    val notify: Boolean,
)
