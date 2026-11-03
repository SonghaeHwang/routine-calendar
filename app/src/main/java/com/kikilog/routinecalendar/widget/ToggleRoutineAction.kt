package com.kikilog.routinecalendar.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.datetime.LocalDate

internal val RoutineIdKey = ActionParameters.Key<Long>("routineId")

/** 위젯이 그릴 때의 오늘(ISO). 자정 직전에 누른 체크가 다음 날로 저장되지 않게 — 오늘 화면과 같은 규칙 */
internal val DateKey = ActionParameters.Key<String>("date")
internal val DoneTodayKey = ActionParameters.Key<Boolean>("doneToday")

/** 체크 칸에 붙이는 파라미터 */
internal fun toggleParameters(row: WidgetRow, today: LocalDate): ActionParameters = actionParametersOf(
    RoutineIdKey to row.routineId,
    DateKey to today.toString(),
    DoneTodayKey to row.doneToday,
)

/** 위젯의 체크 칸. 저장소를 바꾼 뒤 그 위젯을 다시 그린다 */
class ToggleRoutineAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val id = parameters[RoutineIdKey] ?: return
        val date = parameters[DateKey]?.let(LocalDate::parse) ?: return
        val repository = (context.applicationContext as RoutineCalendarApp).container.routineRepository
        // 그 사이 루틴이 지워졌거나 날짜가 맞지 않으면 아무것도 바꾸지 않고 다시 그리기만 한다
        runCatching { toggleRoutine(repository, id, date, doneToday = parameters[DoneTodayKey] == true) }
        TodayWidget().update(context, glanceId)
    }
}

/** 체크돼 있으면 취소, 아니면 체크 */
internal suspend fun toggleRoutine(repository: RoutineRepository, id: Long, date: LocalDate, doneToday: Boolean): Boolean =
    if (doneToday) repository.uncheck(id, date) else repository.check(id, date)
