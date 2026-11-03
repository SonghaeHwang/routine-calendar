package com.kikilog.routinecalendar

import android.content.Context
import com.kikilog.routinecalendar.core.schedule.DEFAULT_WEEK_START
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.PresetRepository
import com.kikilog.routinecalendar.data.RoutineRepository
import com.kikilog.routinecalendar.data.TodayClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.datetime.DayOfWeek

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

    /** 주 시작 요일. 4단계에서 DataStore 설정으로 바꾼다. */
    val weekStart: Flow<DayOfWeek> = flowOf(DEFAULT_WEEK_START)

    val presetRepository: PresetRepository by lazy { PresetRepository(database.presetDao()) }

    val routineRepository: RoutineRepository by lazy {
        RoutineRepository(
            dao = database.routineDao(),
            today = todayClock::current,
            now = System::currentTimeMillis,
        )
    }
}
