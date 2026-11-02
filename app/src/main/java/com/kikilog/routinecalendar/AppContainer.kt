package com.kikilog.routinecalendar

import android.content.Context
import com.kikilog.routinecalendar.data.AppDatabase
import com.kikilog.routinecalendar.data.RoutineRepository
import kotlinx.datetime.toKotlinLocalDate

/** 수동 의존성 묶음. DB 는 처음 쓸 때 연다. */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    val database: AppDatabase by lazy { AppDatabase.build(appContext) }

    val routineRepository: RoutineRepository by lazy {
        RoutineRepository(
            dao = database.routineDao(),
            // 기기 시간대의 오늘. kotlin.time 의 Clock 은 Kotlin 2.2 에서 opt-in 이 필요해 java.time 을 쓴다(minSdk 26)
            today = { java.time.LocalDate.now().toKotlinLocalDate() },
            now = System::currentTimeMillis,
        )
    }
}
