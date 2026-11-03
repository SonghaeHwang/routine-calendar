package com.kikilog.routinecalendar.notify

import android.content.Context
import com.kikilog.routinecalendar.core.notify.buildDailySummary
import com.kikilog.routinecalendar.data.RoutineRepository
import com.kikilog.routinecalendar.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate

/** 요약 알림 한 번의 결과. 시험 · 로그용 */
enum class SendResult { SENT, DISABLED, CANNOT_POST, ALREADY_SENT, NOTHING_TO_SEND }

/**
 * 하루 1회 요약 알림을 보낼지 정하고 보낸다. Worker 가 부른다.
 * 늦게 실행돼도(예: 저녁) 그날 아직 안 보냈으면 보낸다. 할 것이 0개인 날은 보내지 않는다.
 */
class DailySummarySender(
    private val context: Context,
    private val routines: RoutineRepository,
    private val settings: SettingsRepository,
    private val today: () -> LocalDate,
) {
    suspend fun send(): SendResult {
        if (!settings.notifyEnabled.first()) return SendResult.DISABLED
        if (!canPostNotifications(context)) return SendResult.CANNOT_POST

        val date = today()
        if (settings.lastNotifiedOn.first() == date) return SendResult.ALREADY_SENT

        val items = routines.observeRoutines().first().map { it.routine to it.completions }
        val summary = buildDailySummary(items, date)
        if (summary.isEmpty) return SendResult.NOTHING_TO_SEND

        if (!postDailySummary(context, summary)) return SendResult.CANNOT_POST
        settings.setLastNotifiedOn(date)
        return SendResult.SENT
    }
}
