package com.kikilog.routinecalendar.notify

import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.kikilog.routinecalendar.core.notify.NotifyTime
import com.kikilog.routinecalendar.core.notify.nextNotifyAt
import com.kikilog.routinecalendar.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/** WorkManager 고유 작업 이름 */
const val DAILY_SUMMARY_WORK = "daily_summary"

/**
 * 하루 1회 요약 작업을 예약한다.
 *
 * 24시간 주기 작업 하나에 "다음 1회 시각"(setNextScheduleTimeOverride)을 매번 다시 지정한다.
 * - 주기만 쓰면 실행이 늦어질 때 다음 날도 밀린다 → 실행이 끝날 때마다 다음 알림 시각을 다시 계산
 * - 다시 지정하지 못해도 24시간 주기로 계속 돈다
 * - UPDATE 는 실행 중인 작업(Worker 자신)을 끊지 않는다
 * 정확한 시각은 보장하지 않는다(Doze · 절전). 정확한 알람 권한은 쓰지 않는다.
 */
class NotifyScheduler(
    private val workManager: () -> WorkManager,
    private val settings: SettingsRepository,
    private val zone: () -> ZoneId = ZoneId::systemDefault,
    private val nowMillis: () -> Long = System::currentTimeMillis,
) {
    /** 설정대로 맞춘다: 켜져 있으면 예약, 꺼져 있으면 취소 */
    suspend fun sync() {
        if (settings.notifyEnabled.first()) schedule(settings.notifyTime.first()) else cancel()
    }

    /** 앱을 켤 때: 켜져 있을 때만 다시 예약(시간대가 바뀐 경우 등). 꺼져 있으면 WorkManager 를 건드리지 않는다 */
    suspend fun syncIfEnabled() {
        if (settings.notifyEnabled.first()) schedule(settings.notifyTime.first())
    }

    fun schedule(time: NotifyTime) {
        val request = PeriodicWorkRequestBuilder<DailySummaryWorker>(1, TimeUnit.DAYS)
            .setNextScheduleTimeOverride(nextNotifyMillis(nowMillis(), zone(), time))
            .build()
        workManager().enqueueUniquePeriodicWork(DAILY_SUMMARY_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel() {
        workManager().cancelUniqueWork(DAILY_SUMMARY_WORK)
    }
}

/**
 * [nowMillis] 다음의 알림 시각(epoch millis). 벽시계 계산은 :core 의 [nextNotifyAt],
 * 시간대 변환은 java.time 이 한다 — 서머타임으로 없는 시각(봄)은 뒤로 밀리고, 두 번 있는 시각(가을)은 앞쪽을 쓴다.
 */
fun nextNotifyMillis(nowMillis: Long, zone: ZoneId, time: NotifyTime): Long {
    val now = LocalDateTime.ofInstant(Instant.ofEpochMilli(nowMillis), zone).toKotlinLocalDateTime()
    return nextNotifyAt(now, time).toJavaLocalDateTime().atZone(zone).toInstant().toEpochMilli()
}
