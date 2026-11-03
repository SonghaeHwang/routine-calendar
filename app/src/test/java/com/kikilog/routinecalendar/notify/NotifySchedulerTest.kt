package com.kikilog.routinecalendar.notify

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.kikilog.routinecalendar.core.notify.NotifyTime
import com.kikilog.routinecalendar.data.SettingsRepository
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.ZoneId
import java.time.ZonedDateTime

@RunWith(RobolectricTestRunner::class)
class NotifySchedulerTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val seoul = ZoneId.of("Asia/Seoul")
    private val newYork = ZoneId.of("America/New_York")

    private fun millis(text: String, zone: ZoneId) = ZonedDateTime.of(java.time.LocalDateTime.parse(text), zone).toInstant().toEpochMilli()
    private fun local(millis: Long, zone: ZoneId) = java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDateTime().toString()

    // --- 시간대 변환 ---

    @Test
    fun `서울 시각 기준으로 다음 08시`() {
        val next = nextNotifyMillis(millis("2026-11-03T07:00", seoul), seoul, NotifyTime(8, 0))
        assertEquals("2026-11-03T08:00", local(next, seoul))
        val after = nextNotifyMillis(millis("2026-11-03T09:00", seoul), seoul, NotifyTime(8, 0))
        assertEquals("2026-11-04T08:00", local(after, seoul))
    }

    @Test
    fun `서머타임 시작 날 없는 시각은 한 시간 뒤로 밀린다`() {
        // 2026-03-08 02:00 → 03:00 (뉴욕). 02:30 은 없는 시각
        val next = nextNotifyMillis(millis("2026-03-07T23:00", newYork), newYork, NotifyTime(2, 30))
        assertEquals("2026-03-08T03:30", local(next, newYork))
    }

    @Test
    fun `서머타임 끝나는 날에도 벽시계 08시`() {
        // 2026-11-01 02:00 → 01:00 (뉴욕). 하루가 25시간
        val next = nextNotifyMillis(millis("2026-10-31T20:00", newYork), newYork, NotifyTime(8, 0))
        assertEquals("2026-11-01T08:00", local(next, newYork))
    }

    // --- WorkManager 예약 ---

    private lateinit var workManager: WorkManager

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
    }

    private fun TestScope.newSettings() = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { folder.newFile("settings.preferences_pb") }),
    )

    private fun infos() = workManager.getWorkInfosForUniqueWork(DAILY_SUMMARY_WORK).get()

    @Test
    fun `켜면 고유 작업 하나가 예약되고 다시 맞춰도 하나뿐이다`() = runTest {
        val settings = newSettings().apply { setNotifyEnabled(true) }
        val scheduler = NotifyScheduler({ workManager }, settings, zone = { seoul })

        scheduler.sync()
        scheduler.sync()

        val active = infos().filter { it.state == WorkInfo.State.ENQUEUED }
        assertEquals(1, active.size)
    }

    @Test
    fun `끄면 예약이 취소된다`() = runTest {
        val settings = newSettings().apply { setNotifyEnabled(true) }
        val scheduler = NotifyScheduler({ workManager }, settings, zone = { seoul })
        scheduler.sync()

        settings.setNotifyEnabled(false)
        scheduler.sync()

        assertTrue(infos().all { it.state == WorkInfo.State.CANCELLED })
    }

    @Test
    fun `앱을 켤 때 알림이 꺼져 있으면 아무것도 예약하지 않는다`() = runTest {
        val scheduler = NotifyScheduler({ workManager }, newSettings(), zone = { seoul })
        scheduler.syncIfEnabled()
        assertTrue(infos().isEmpty())
    }
}
