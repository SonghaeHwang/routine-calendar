package com.kikilog.routinecalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kikilog.routinecalendar.ui.navigation.RoutineCalendarRoot
import com.kikilog.routinecalendar.ui.theme.RoutineCalendarTheme

class MainActivity : ComponentActivity() {

    private val container get() = (application as RoutineCalendarApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoutineCalendarTheme {
                RoutineCalendarRoot()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 밤새 잠든 뒤 켰을 때 자정 대기가 늦게 깨어날 수 있어, 화면이 보일 때마다 오늘을 다시 읽는다
        container.todayClock.refresh()
    }
}
