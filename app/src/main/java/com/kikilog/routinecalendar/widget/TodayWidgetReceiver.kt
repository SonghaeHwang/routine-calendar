package com.kikilog.routinecalendar.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import com.kikilog.routinecalendar.RoutineCalendarApp

/** 홈 화면 위젯 수신자. Manifest 에 등록한다. 위젯이 있는 동안만 자정 갱신을 예약해 둔다 */
class TodayWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TodayWidget()

    /** 첫 위젯을 놓을 때 */
    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        scheduler(context).schedule()
    }

    /** 위젯을 놓을 때 · 재부팅 뒤 · 하루 1회(updatePeriodMillis). 예약이 빠졌어도 다시 맞춘다 */
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        scheduler(context).schedule()
    }

    /** 마지막 위젯을 지울 때 */
    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        scheduler(context).cancel()
    }

    private fun scheduler(context: Context) =
        (context.applicationContext as RoutineCalendarApp).container.widgetRefreshScheduler
}
