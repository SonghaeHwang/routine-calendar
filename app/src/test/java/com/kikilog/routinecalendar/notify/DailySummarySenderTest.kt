package com.kikilog.routinecalendar.notify

import android.Manifest
import android.app.Application
import android.app.NotificationManager
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.RoutineDraft
import com.kikilog.routinecalendar.data.RoutineRepository
import com.kikilog.routinecalendar.data.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DailySummarySenderTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val today = LocalDate.parse("2026-11-03")
    private val app: Application = ApplicationProvider.getApplicationContext()
    private lateinit var db: AppDatabase
    private lateinit var routines: RoutineRepository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java).allowMainThreadQueries().build()
        routines = RoutineRepository(db.routineDao(), today = { today }, now = { 1_000L })
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun TestScope.newSettings() = SettingsRepository(
        PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { folder.newFile("settings.preferences_pb") }),
    )

    private fun sender(settings: SettingsRepository) = DailySummarySender(app, routines, settings, today = { today })

    private val posted get() = shadowOf(app.getSystemService(NotificationManager::class.java)).allNotifications

    /** 마지막으로 한 날 + 주기(일) = 예정일 */
    private suspend fun add(title: String, days: Int, lastDoneOn: String, notify: Boolean = true) {
        routines.add(
            RoutineDraft(title, Category.ETC, Interval(days, IntervalUnit.DAY), repeats = true, notify = notify),
            LocalDate.parse(lastDoneOn),
        )
    }

    @Test
    fun `오늘 할 것이 있으면 보내고 보낸 날을 저장한다`() = runTest {
        val settings = newSettings().apply { setNotifyEnabled(true) }
        add("이불 세탁", days = 7, lastDoneOn = "2026-10-27") // 예정 11-03
        add("칫솔 교체", days = 30, lastDoneOn = "2026-09-30") // 예정 10-30, 지남

        assertEquals(SendResult.SENT, sender(settings).send())

        val notification = posted.single()
        assertEquals("오늘 할 루틴 1개 · 지난 것 1개", shadowOf(notification).contentTitle)
        assertEquals(today, settings.lastNotifiedOn.first())
    }

    @Test
    fun `알림을 끈 상태면 보내지 않는다`() = runTest {
        val settings = newSettings()
        add("이불 세탁", days = 7, lastDoneOn = "2026-10-27")

        assertEquals(SendResult.DISABLED, sender(settings).send())
        assertTrue(posted.isEmpty())
    }

    @Test
    fun `권한이 없으면 보내지 않는다`() = runTest {
        shadowOf(app).denyPermissions(Manifest.permission.POST_NOTIFICATIONS)
        val settings = newSettings().apply { setNotifyEnabled(true) }
        add("이불 세탁", days = 7, lastDoneOn = "2026-10-27")

        assertEquals(SendResult.CANNOT_POST, sender(settings).send())
        assertTrue(posted.isEmpty())
        assertNull(settings.lastNotifiedOn.first())
    }

    @Test
    fun `오늘 이미 보냈으면 다시 보내지 않는다`() = runTest {
        val settings = newSettings().apply {
            setNotifyEnabled(true)
            setLastNotifiedOn(today)
        }
        add("이불 세탁", days = 7, lastDoneOn = "2026-10-27")

        assertEquals(SendResult.ALREADY_SENT, sender(settings).send())
        assertTrue(posted.isEmpty())
    }

    @Test
    fun `할 것이 없는 날은 보내지 않는다 - 알림에 넣기를 끈 루틴만 있는 경우 포함`() = runTest {
        val settings = newSettings().apply { setNotifyEnabled(true) }
        add("화장지", days = 60, lastDoneOn = "2026-11-01") // 예정 12-31
        add("젤팩", days = 7, lastDoneOn = "2026-10-27", notify = false) // 오늘이지만 알림 끔

        assertEquals(SendResult.NOTHING_TO_SEND, sender(settings).send())
        assertTrue(posted.isEmpty())
        assertNull(settings.lastNotifiedOn.first())
    }
}
