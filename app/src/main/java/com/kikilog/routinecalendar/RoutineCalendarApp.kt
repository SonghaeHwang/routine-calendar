package com.kikilog.routinecalendar

import android.app.Application
import com.kikilog.routinecalendar.notify.ensureNotificationChannels

class RoutineCalendarApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // 알림을 보내기 전에 채널이 있어야 한다. 이미 있으면 아무 일도 없다
        ensureNotificationChannels(this)
    }
}
