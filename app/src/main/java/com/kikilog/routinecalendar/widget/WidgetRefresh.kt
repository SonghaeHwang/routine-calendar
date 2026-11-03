package com.kikilog.routinecalendar.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.kikilog.routinecalendar.RoutineCalendarApp
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** WorkManager 고유 작업 이름 */
const val WIDGET_REFRESH_WORK = "widget_midnight_refresh"

/** 자정 직후 날짜를 다시 읽도록 1분 여유 */
private const val AFTER_MIDNIGHT_MILLIS = 60_000L

/**
 * 날짜가 바뀌면 위젯의 「오늘」도 바뀌도록 자정마다 다시 그린다.
 * 알림 예약([com.kikilog.routinecalendar.notify.NotifyScheduler])과 같은 방식: 24시간 주기 작업에 다음 시각만 다시 지정한다.
 * 도즈 상태에서는 몇 분 늦을 수 있다(정확한 알람 권한은 쓰지 않는다).
 */
class WidgetRefreshScheduler(
    private val workManager: () -> WorkManager,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    fun schedule() {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(1, TimeUnit.DAYS)
            .setNextScheduleTimeOverride(nextWidgetRefreshMillis(nowMillis(), zone()))
            .build()
        workManager().enqueueUniquePeriodicWork(WIDGET_REFRESH_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel() {
        workManager().cancelUniqueWork(WIDGET_REFRESH_WORK)
    }
}

/** [nowMillis] 다음 날 0시 + 1분(epoch millis). 서머타임으로 0시가 없는 날은 그날의 첫 시각 */
fun nextWidgetRefreshMillis(nowMillis: Long, zone: ZoneId): Long {
    val tomorrow = Instant.ofEpochMilli(nowMillis).atZone(zone).toLocalDate().plusDays(1)
    return tomorrow.atStartOfDay(zone).toInstant().toEpochMilli() + AFTER_MIDNIGHT_MILLIS
}

/** 자정 갱신 작업. 끝나면 다음 자정을 다시 지정한다 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as RoutineCalendarApp).container
        try {
            container.todayClock.refresh()
            TodayWidget().updateAll(applicationContext)
        } finally {
            container.widgetRefreshScheduler.schedule()
        }
        return Result.success()
    }
}
