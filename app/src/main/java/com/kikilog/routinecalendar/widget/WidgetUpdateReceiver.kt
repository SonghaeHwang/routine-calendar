package com.kikilog.routinecalendar.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.updateAll
import com.kikilog.routinecalendar.RoutineCalendarApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * 앱을 업데이트하면 런처가 예전 위젯 화면을 버리고 「불러오는 중」으로 남는다(2026-11-04 S26 에서 확인).
 * 시스템이 이 앱에만 보내는 MY_PACKAGE_REPLACED 를 받아 위젯을 곧바로 다시 그리고 자정 갱신도 다시 맞춘다.
 * 위젯이 없으면 updateAll 은 아무 일도 하지 않는다.
 */
class WidgetUpdateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val appContext = context.applicationContext
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                TodayWidget().updateAll(appContext)
                (appContext as RoutineCalendarApp).container.widgetRefreshScheduler.schedule()
            } finally {
                pending.finish()
            }
        }
    }
}
