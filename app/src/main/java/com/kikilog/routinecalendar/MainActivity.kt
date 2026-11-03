package com.kikilog.routinecalendar

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.kikilog.routinecalendar.notify.EXTRA_OPEN_TAB
import com.kikilog.routinecalendar.ui.navigation.RoutineCalendarRoot
import com.kikilog.routinecalendar.ui.navigation.TopLevelTab
import com.kikilog.routinecalendar.ui.navigation.toTab
import com.kikilog.routinecalendar.ui.theme.RoutineCalendarTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val container get() = (application as RoutineCalendarApp).container

    /** 알림을 눌러 열었을 때 보여 줄 탭. 처리하면 null 로 되돌린다 */
    private var openTabRequest by mutableStateOf<TopLevelTab?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            // 알림이 켜져 있으면 다음 시각을 다시 지정한다(시간대가 바뀐 경우 등). 꺼져 있으면 아무것도 하지 않는다
            lifecycleScope.launch { container.notifyScheduler.syncIfEnabled() }
            // 화면을 돌려 다시 만들 때는 이미 처리한 요청을 또 처리하지 않는다
            openTabRequest = intent.openTab()
        }
        setContent {
            RoutineCalendarTheme {
                // 첫 화면 설정은 켤 때 한 번 읽는다. 설정을 바꾸면 다음에 켤 때부터 적용된다
                var startTab by rememberSaveable { mutableStateOf<TopLevelTab?>(null) }
                LaunchedEffect(Unit) {
                    if (startTab == null) startTab = container.settingsRepository.startScreen.first().toTab()
                }
                startTab?.let {
                    RoutineCalendarRoot(
                        startTab = it,
                        openTab = openTabRequest,
                        onOpenTabHandled = { openTabRequest = null },
                    )
                }
            }
        }
    }

    /** 앱이 떠 있을 때 알림을 누른 경우 */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.openTab()?.let { openTabRequest = it }
    }

    override fun onResume() {
        super.onResume()
        // 밤새 잠든 뒤 켰을 때 자정 대기가 늦게 깨어날 수 있어, 화면이 보일 때마다 오늘을 다시 읽는다
        container.todayClock.refresh()
    }

    private fun Intent.openTab(): TopLevelTab? =
        getStringExtra(EXTRA_OPEN_TAB)?.let { name -> TopLevelTab.entries.firstOrNull { it.name == name } }
}
