package com.kikilog.routinecalendar.widget

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

@RunWith(RobolectricTestRunner::class)
class WidgetRefreshTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val seoul = ZoneId.of("Asia/Seoul")

    private fun millis(text: String, zone: ZoneId) = LocalDateTime.parse(text).atZone(zone).toInstant().toEpochMilli()
    private fun local(millis: Long, zone: ZoneId) = Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime().toString()

    @Test
    fun `다음 갱신은 다음 날 0시 1분`() {
        assertEquals("2026-11-04T00:01", local(nextWidgetRefreshMillis(millis("2026-11-03T15:30", seoul), seoul), seoul))
        // 자정 직후에 돌아도 다음 날로
        assertEquals("2026-11-05T00:01", local(nextWidgetRefreshMillis(millis("2026-11-04T00:01", seoul), seoul), seoul))
    }

    @Test
    fun `0시가 없는 날은 그날의 첫 시각 + 1분`() {
        // 상파울루 2018-11-04 0시 → 1시 (서머타임 시작)
        val saoPaulo = ZoneId.of("America/Sao_Paulo")
        assertEquals("2018-11-04T01:01", local(nextWidgetRefreshMillis(millis("2018-11-03T20:00", saoPaulo), saoPaulo), saoPaulo))
    }

    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
    }

    private fun infos() = workManager.getWorkInfosForUniqueWork(WIDGET_REFRESH_WORK).get()

    @Test
    fun `여러 번 예약해도 하나, 취소하면 사라진다`() {
        val scheduler = WidgetRefreshScheduler({ workManager }, zone = { seoul })

        scheduler.schedule()
        scheduler.schedule()
        assertEquals(1, infos().count { it.state == WorkInfo.State.ENQUEUED })

        scheduler.cancel()
        assertTrue(infos().all { it.state == WorkInfo.State.CANCELLED })
    }
}
