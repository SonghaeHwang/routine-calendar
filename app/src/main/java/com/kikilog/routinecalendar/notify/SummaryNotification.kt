package com.kikilog.routinecalendar.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.kikilog.routinecalendar.MainActivity
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.core.notify.DailySummary

/** 알림 채널 id. 한 번 만든 뒤에는 바꾸지 않는다(사용자가 고른 중요도가 채널에 붙어 있다) */
const val DAILY_SUMMARY_CHANNEL_ID = "daily_summary"

/** 같은 id 로 덮어써서 요약 알림은 늘 하나만 남는다 */
const val DAILY_SUMMARY_NOTIFICATION_ID = 1

/** 알림을 눌러 앱을 열 때 MainActivity 에 넘기는 값. 값은 열 탭(TopLevelTab.name). 처음 열린 달력은 오늘을 고른 채다 */
const val EXTRA_OPEN_TAB = "open_tab"

/** 채널을 만든다. 이미 있으면 아무 일도 없다 */
fun ensureNotificationChannels(context: Context) {
    val channel = NotificationChannelCompat.Builder(DAILY_SUMMARY_CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
        .setName(context.getString(R.string.notify_channel_name))
        .setDescription(context.getString(R.string.notify_channel_description))
        .build()
    NotificationManagerCompat.from(context).createNotificationChannel(channel)
}

/** 알림을 띄울 수 있는지: 권한(Android 13+)과 앱 알림 스위치 모두 */
fun canPostNotifications(context: Context): Boolean =
    hasNotificationPermission(context) && NotificationManagerCompat.from(context).areNotificationsEnabled()

/** Android 12 이하는 권한이 없어 늘 true */
fun hasNotificationPermission(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

/** 요약 알림을 띄운다. 띄울 수 없으면 false */
// 권한은 canPostNotifications 에서 확인한다. Lint 는 다른 함수의 확인을 따라가지 못한다
@SuppressLint("MissingPermission")
fun postDailySummary(context: Context, summary: DailySummary): Boolean {
    if (!canPostNotifications(context)) return false
    ensureNotificationChannels(context)
    NotificationManagerCompat.from(context).notify(DAILY_SUMMARY_NOTIFICATION_ID, buildSummaryNotification(context, summary))
    return true
}

/**
 * 제목은 개수(「오늘 할 루틴 3개 · 지난 것 1개」), 펼치면 루틴 이름 목록.
 * 잠금 화면에서 내용을 숨기면 공개 버전(개수만)이 보인다.
 */
fun buildSummaryNotification(context: Context, summary: DailySummary): Notification {
    val countText = summaryCountText(context, summary)
    val names = summary.overdue.map { context.getString(R.string.notify_line_overdue, it.title) } +
        summary.dueToday.map { it.title }

    val publicVersion = NotificationCompat.Builder(context, DAILY_SUMMARY_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_today)
        .setContentTitle(countText)
        .build()

    val inbox = NotificationCompat.InboxStyle().setBigContentTitle(countText)
    names.forEach { inbox.addLine(it) }

    return NotificationCompat.Builder(context, DAILY_SUMMARY_CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_today)
        .setContentTitle(countText)
        .setContentText(names.joinToString(", "))
        .setStyle(inbox)
        .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
        .setPublicVersion(publicVersion)
        .setContentIntent(openTodayIntent(context))
        .setAutoCancel(true)
        .build()
}

/** 「오늘 할 루틴 3개 · 지난 것 1개」. 0개인 쪽은 빼고 보인다 */
fun summaryCountText(context: Context, summary: DailySummary): String = listOfNotNull(
    summary.dueToday.size.takeIf { it > 0 }?.let { context.getString(R.string.notify_count_today, it) },
    summary.overdue.size.takeIf { it > 0 }?.let { context.getString(R.string.notify_count_overdue, it) },
).joinToString(" · ")

private fun openTodayIntent(context: Context): PendingIntent {
    // 앱 안의 화면 하나로만 가므로 URI 딥 링크 대신 명시 Intent + extra
    val intent = Intent(context, MainActivity::class.java)
        .putExtra(EXTRA_OPEN_TAB, OPEN_TAB_ON_TAP)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    return PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}

/** 알림을 눌렀을 때 열 탭. 오늘 탭을 숨긴 뒤로는 달력(오늘 날짜가 골라져 있다) */
const val OPEN_TAB_ON_TAP = "CALENDAR"
