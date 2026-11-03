package com.kikilog.routinecalendar.ui.common

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.kikilog.routinecalendar.notify.canPostNotifications
import com.kikilog.routinecalendar.notify.hasNotificationPermission

/**
 * 알림을 켤 때 권한을 묻는 함수를 돌려준다. 결과(허용 true / 거절 false)는 [onResult] 로 온다.
 * - 이미 허용돼 있으면 묻지 않고 true
 * - 권한은 있는데 휴대폰 설정에서 앱 알림을 꺼 두었으면 앱 알림 설정을 연다(돌아온 뒤 다시 켜야 한다)
 * - 두 번 넘게 거절하면 시스템 창이 다시 뜨지 않고 곧바로 false 가 온다
 */
@Composable
fun rememberNotificationEnabler(onResult: (Boolean) -> Unit): () -> Unit {
    val context = LocalContext.current
    val currentOnResult by rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        currentOnResult(granted && canPostNotifications(context))
    }
    return remember(context, launcher) {
        {
            when {
                canPostNotifications(context) -> currentOnResult(true)
                hasNotificationPermission(context) -> {
                    openAppNotificationSettings(context)
                    currentOnResult(false)
                }
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> currentOnResult(false)
            }
        }
    }
}

/** 지금 알림을 띄울 수 있는지. 휴대폰 설정에서 바꾸고 돌아올 수 있어 화면이 다시 보일 때마다 새로 읽는다 */
@Composable
fun rememberCanPostNotifications(): Boolean {
    val context = LocalContext.current
    var canPost by remember { mutableStateOf(canPostNotifications(context)) }
    LifecycleResumeEffect(context) {
        canPost = canPostNotifications(context)
        onPauseOrDispose { }
    }
    return canPost
}

/** 휴대폰 설정 → 이 앱의 알림 */
fun openAppNotificationSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    startSafely(context, intent)
}

/** 휴대폰 설정 → 배터리 최적화 목록. 예외를 직접 요청하지 않고 사용자가 고르게 한다(Play 정책) */
fun openBatteryOptimizationSettings(context: Context) {
    startSafely(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
}

private fun startSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        // 제조사 설정 앱에 그 화면이 없으면 아무것도 하지 않는다
    }
}
