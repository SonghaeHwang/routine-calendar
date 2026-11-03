package com.kikilog.routinecalendar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.kikilog.routinecalendar.ui.navigation.RoutineCalendarRoot
import com.kikilog.routinecalendar.ui.navigation.TopLevelTab
import com.kikilog.routinecalendar.ui.navigation.toTab
import com.kikilog.routinecalendar.ui.theme.RoutineCalendarTheme
import kotlinx.coroutines.flow.first

class MainActivity : ComponentActivity() {

    private val container get() = (application as RoutineCalendarApp).container

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RoutineCalendarTheme {
                // 첫 화면 설정은 켤 때 한 번 읽는다. 설정을 바꾸면 다음에 켤 때부터 적용된다
                var startTab by rememberSaveable { mutableStateOf<TopLevelTab?>(null) }
                LaunchedEffect(Unit) {
                    if (startTab == null) startTab = container.settingsRepository.startScreen.first().toTab()
                }
                startTab?.let { RoutineCalendarRoot(startTab = it) }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 밤새 잠든 뒤 켰을 때 자정 대기가 늦게 깨어날 수 있어, 화면이 보일 때마다 오늘을 다시 읽는다
        container.todayClock.refresh()
    }
}
