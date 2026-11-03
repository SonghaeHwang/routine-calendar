package com.kikilog.routinecalendar.widget

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/** 연달아 바뀔 때(선택 삭제 등) 한 번만 다시 그리도록 기다리는 시간 */
const val WIDGET_SYNC_DEBOUNCE_MILLIS = 500L

/**
 * 앱에서 바꾼 내용을 위젯에 알린다. 저장소 쓰기가 여러 곳(등록 · 수정 · 체크 · 보관 · 삭제 · 분류 편집)이라
 * 화면마다 갱신 호출을 넣지 않고, 데이터 흐름 하나를 구독해 바뀔 때마다 [update] 한다.
 * 위젯 세션이 살아 있는 동안은 위젯이 스스로 구독하지만, 세션이 끝난 뒤에는 이 호출이 있어야 바뀐다.
 *
 * @param changes 루틴 · 기록 · 분류 · 오늘 날짜를 묶은 흐름. 첫 값(지금 상태)은 건너뛴다
 * @param update 위젯 전부 다시 그리기. 위젯이 하나도 없으면 아무 일도 없다
 */
class WidgetSync(
    private val changes: Flow<Any>,
    private val scope: CoroutineScope,
    private val update: suspend () -> Unit,
) {
    private val started = AtomicBoolean(false)

    /** 한 번만 시작한다. 화면을 다시 만들어도 구독은 하나 */
    @OptIn(FlowPreview::class)
    fun start() {
        if (!started.compareAndSet(false, true)) return
        scope.launch {
            changes
                .distinctUntilChanged()
                .drop(1)
                .debounce(WIDGET_SYNC_DEBOUNCE_MILLIS)
                .collectLatest { runCatching { update() } }
        }
    }
}
