package com.kikilog.routinecalendar

import android.app.Application

class RoutineCalendarApp : Application() {
    val container: AppContainer by lazy { AppContainer(this) }
}
