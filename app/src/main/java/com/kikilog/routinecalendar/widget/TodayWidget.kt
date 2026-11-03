package com.kikilog.routinecalendar.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.Action
import androidx.glance.action.clickable
import androidx.glance.appwidget.CheckBox
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import com.kikilog.routinecalendar.MainActivity
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.RoutineCalendarApp
import com.kikilog.routinecalendar.notify.EXTRA_OPEN_TAB
import com.kikilog.routinecalendar.notify.OPEN_TAB_ON_TAP
import com.kikilog.routinecalendar.ui.format.formatDate
import com.kikilog.routinecalendar.ui.format.formatShortDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

// 이 파일은 Glance 컴포저블만 쓴다(androidx.compose.ui 의 Text · Row 와 섞지 않는다).

/** 좁은 크기(2칸)에서는 날짜 머리글과 「N일 지남」을 줄인다 */
private val COMPACT = DpSize(110.dp, 110.dp)
private val WIDE = DpSize(220.dp, 110.dp)

/**
 * 홈 화면 「오늘 루틴」 위젯: 지난 것 + 오늘, 체크 칸으로 바로 체크.
 * 저장소 Flow 를 구성 안에서 구독하고(세션이 살아 있는 약 45초), 그 밖의 변경은 [WidgetSync] 가 updateAll 로 알린다.
 */
class TodayWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(COMPACT, WIDE))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val container = (context.applicationContext as RoutineCalendarApp).container
        // 앱이 오래 잠들어 있었으면 자정 갱신을 놓쳤을 수 있다
        container.todayClock.refresh()
        val states = combine(
            container.routineRepository.observeRoutines(),
            container.categoryRepository.observeCategories(),
            container.todayClock.today,
        ) { items, categories, today -> buildWidgetState(items, categories, today) }
        val initial = withContext(Dispatchers.IO) { states.first() }
        provideContent {
            val state by states.collectAsState(initial)
            GlanceTheme { TodayWidgetContent(state) }
        }
    }
}

/** 앱을 열어 달력(오늘 날짜가 골라져 있음)으로. 오늘 탭을 숨긴 뒤로 알림과 같은 곳 */
private fun openAppAction(context: Context): Action = actionStartActivity(
    Intent(context, MainActivity::class.java).putExtra(EXTRA_OPEN_TAB, OPEN_TAB_ON_TAP),
)

@Composable
fun TodayWidgetContent(state: WidgetState) {
    val context = LocalContext.current
    val compact = LocalSize.current.width < WIDE.width
    val openApp = openAppAction(context)
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .appWidgetBackground()
            .background(GlanceTheme.colors.widgetBackground)
            .cornerRadius(16.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Header(state, compact, openApp)
        when {
            state.rows.isNotEmpty() -> LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                items(state.rows, itemId = { it.routineId }) { row ->
                    RoutineRow(row, state, compact, openApp)
                }
            }
            else -> EmptyMessage(state, openApp)
        }
    }
}

@Composable
private fun Header(state: WidgetState, compact: Boolean, openApp: Action) {
    val context = LocalContext.current
    val title = if (compact) formatShortDate(state.today) else formatDate(state.today, state.today)
    val left = state.rows.count { !it.doneToday }
    Row(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp).clickable(openApp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = TextStyle(color = GlanceTheme.colors.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (left > 0) {
            Text(
                text = context.getString(R.string.widget_left, left),
                style = TextStyle(color = GlanceTheme.colors.primary, fontSize = 13.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun RoutineRow(row: WidgetRow, state: WidgetState, compact: Boolean, openApp: Action) {
    val context = LocalContext.current
    Row(
        modifier = GlanceModifier.fillMaxWidth().clickable(openApp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryDot(row.colorArgb)
        Spacer(GlanceModifier.width(4.dp))
        CheckBox(
            checked = row.doneToday,
            onCheckedChange = actionRunCallback<ToggleRoutineAction>(toggleParameters(row, state.today)),
            text = row.title,
            style = TextStyle(
                color = if (row.doneToday) GlanceTheme.colors.onSurfaceVariant else GlanceTheme.colors.onSurface,
                fontSize = 14.sp,
                textDecoration = if (row.doneToday) TextDecoration.LineThrough else TextDecoration.None,
            ),
            maxLines = 1,
            modifier = GlanceModifier.defaultWeight(),
        )
        if (row.overdueDays > 0) {
            Text(
                text = if (compact) {
                    context.getString(R.string.widget_overdue_short, row.overdueDays)
                } else {
                    context.resources.getQuantityString(R.plurals.overdue_days, row.overdueDays, row.overdueDays)
                },
                style = TextStyle(color = GlanceTheme.colors.error, fontSize = 12.sp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun CategoryDot(colorArgb: Int?) {
    // 분류 색은 낮 · 밤 같은 색(앱의 달력 점과 같다)
    val color = colorArgb?.let { ColorProvider(day = Color(it), night = Color(it)) } ?: GlanceTheme.colors.outline
    // 둥근 모서리는 Android 12 이상에서만 보인다(그 아래는 네모 점)
    Box(modifier = GlanceModifier.size(8.dp).cornerRadius(4.dp).background(color)) {}
}

@Composable
private fun EmptyMessage(state: WidgetState, openApp: Action) {
    val context = LocalContext.current
    Column(modifier = GlanceModifier.fillMaxSize().clickable(openApp)) {
        Spacer(GlanceModifier.height(4.dp))
        Text(
            text = context.getString(if (state.hasAnyRoutine) R.string.widget_empty else R.string.widget_no_routines),
            style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 14.sp),
        )
        state.next?.let { next ->
            Text(
                text = context.getString(R.string.widget_next, next.title, formatShortDate(next.due)),
                style = TextStyle(color = GlanceTheme.colors.onSurfaceVariant, fontSize = 13.sp),
                maxLines = 2,
                modifier = GlanceModifier.padding(top = 4.dp),
            )
        }
    }
}
