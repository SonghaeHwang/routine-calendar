package com.kikilog.routinecalendar.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.kikilog.routinecalendar.RoutineCalendarApp

/**
 * 하루 1회 요약 알림 작업. 실제 판단은 [DailySummarySender] 가 하고,
 * 끝나면 다음 알림 시각을 다시 지정한다(보내지 않은 날도). 실패해도 재시도하지 않는다 — 다음 날 다시 돈다.
 */
class DailySummaryWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as RoutineCalendarApp).container
        try {
            container.dailySummarySender.send()
        } finally {
            container.notifyScheduler.sync()
        }
        return Result.success()
    }
}
