package com.kikilog.routinecalendar

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.WorkManager
import com.kikilog.routinecalendar.backup.BackupRepository
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.CategoryRepository
import com.kikilog.routinecalendar.data.PresetRepository
import com.kikilog.routinecalendar.data.RoutineRepository
import com.kikilog.routinecalendar.data.SettingsRepository
import com.kikilog.routinecalendar.data.settingsDataStore
import com.kikilog.routinecalendar.data.TodayClock
import com.kikilog.routinecalendar.notify.DailySummarySender
import com.kikilog.routinecalendar.notify.NotifyScheduler
import com.kikilog.routinecalendar.widget.TodayWidget
import com.kikilog.routinecalendar.widget.WidgetRefreshScheduler
import com.kikilog.routinecalendar.widget.WidgetSync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek
import java.io.File

/** 수동 의존성 묶음. DB 는 처음 쓸 때 연다. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    /** 앱이 살아 있는 동안 도는 작업(자정 갱신 등)용 */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    /** 기기 시간대의 오늘. 처음 쓸 때 자정 갱신을 시작한다. */
    val todayClock: TodayClock by lazy {
        TodayClock().also { clock -> appScope.launch { clock.runMidnightUpdates() } }
    }

    /** 앱 설정(주 시작 요일 · 첫 화면). 이 휴대폰에만 저장 */
    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext.settingsDataStore) }

    /** 주 시작 요일(설정, 기본 일요일) */
    val weekStart: Flow<DayOfWeek> get() = settingsRepository.weekStart

    val presetRepository: PresetRepository by lazy { PresetRepository(database.presetDao()) }

    val categoryRepository: CategoryRepository by lazy { CategoryRepository(database.categoryDao()) }

    val routineRepository: RoutineRepository by lazy {
        RoutineRepository(
            dao = database.routineDao(),
            today = todayClock::current,
            now = System::currentTimeMillis,
        )
    }

    /** 하루 1회 요약 알림 예약. WorkManager 는 처음 예약할 때 가져온다 */
    val notifyScheduler: NotifyScheduler by lazy {
        NotifyScheduler(workManager = { WorkManager.getInstance(appContext) }, settings = settingsRepository)
    }

    val dailySummarySender: DailySummarySender by lazy {
        DailySummarySender(appContext, routineRepository, settingsRepository, today = todayClock::current)
    }

    /** JSON 내보내기 · 가져오기. 가져오기 전 자동 저장은 자동 백업에서 빠지는 폴더에 */
    val backupRepository: BackupRepository by lazy {
        BackupRepository(
            dao = database.backupDao(),
            settings = settingsRepository,
            snapshotFile = File(appContext.noBackupFilesDir, "before-import.json"),
            now = System::currentTimeMillis,
            appVersion = appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName.orEmpty(),
            onSettingsReplaced = notifyScheduler::syncIfEnabled,
        )
    }

    /** 홈 화면 위젯의 자정 갱신 예약 */
    val widgetRefreshScheduler: WidgetRefreshScheduler by lazy {
        WidgetRefreshScheduler(workManager = { WorkManager.getInstance(appContext) })
    }

    /** 앱에서 바꾼 내용을 위젯에 알린다. 보관한 루틴도 포함(보관 · 해제도 위젯을 바꾼다) */
    val widgetSync: WidgetSync by lazy {
        WidgetSync(
            changes = combine(
                routineRepository.observeRoutines(includeArchived = true),
                categoryRepository.observeCategories(),
                todayClock.today,
            ) { routines, categories, today -> Triple(routines, categories, today) },
            scope = appScope,
            update = { TodayWidget().updateAll(appContext) },
        )
    }
}
