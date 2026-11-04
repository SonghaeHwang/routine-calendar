package com.kikilog.routinecalendar.ui.categories

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import com.kikilog.routinecalendar.core.model.Category
import kotlinx.coroutines.delay

/**
 * 분류 줄을 꾹 눌러 끌어 순서 바꾸기. 라이브러리 없이 [LazyListState.layoutInfo] 로 손가락 아래 줄을 찾아
 * 화면 안 목록([items])의 자리를 바로 바꾸고, 놓으면 그 순서를 저장한다. 빠른 선택 끌기(PresetDrag)와 같은 방식.
 */
class CategoryDragState(
    private val listState: LazyListState,
    private val haptic: HapticFeedback,
) {
    /** 화면에 그리는 순서. 끄는 동안에는 저장된 순서와 다르다 */
    var items by mutableStateOf<List<Category>>(emptyList())
        private set

    /** 끌고 있는 분류. null 이면 끌지 않는 중 */
    var draggingId by mutableStateOf<Long?>(null)
        private set

    /** 줄의 원래 자리에서 손가락까지 움직인 거리(px) */
    var offsetY by mutableFloatStateOf(0f)
        private set

    /** 저장된 순서가 바뀌면 따른다. 끄는 중이면 놓을 때까지 기다린다 */
    fun sync(saved: List<Category>) {
        if (draggingId == null) items = saved
    }

    fun start(id: Long) {
        draggingId = id
        offsetY = 0f
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun drag(dy: Float) {
        offsetY += dy
        swapIfNeeded()
    }

    /** 놓으면 새 순서(id 목록) */
    fun end(): List<Long> {
        draggingId = null
        offsetY = 0f
        return items.map { it.id }
    }

    fun cancel(saved: List<Category>) {
        draggingId = null
        offsetY = 0f
        items = saved
    }

    /** 가장자리 근처면 스크롤할 양(px). 위는 음수 */
    fun edgeScrollAmount(): Float {
        val info = draggedItem() ?: return 0f
        val center = info.offset + info.size / 2f + offsetY
        val top = listState.layoutInfo.viewportStartOffset
        val bottom = listState.layoutInfo.viewportEndOffset
        val edge = (bottom - top) * EDGE_ZONE
        return when {
            center < top + edge -> -EDGE_SCROLL_STEP
            center > bottom - edge -> EDGE_SCROLL_STEP
            else -> 0f
        }
    }

    /** 스크롤하면 줄이 반대로 움직이므로 손가락 자리에 머물게 보정한다 */
    fun onScrolled(consumed: Float) {
        offsetY += consumed
        swapIfNeeded()
    }

    private fun draggedItem() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingId }

    /** 분류 줄 앞에 있는 줄(안내 문구) 수. 목록 자리를 분류 순서로 바꿀 때 빼다 */
    private fun indexOfFirstCategory(): Int = listState.layoutInfo.totalItemsCount - items.size

    /** 끄는 줄의 가운데가 다른 분류 줄 위로 오면 둘의 자리를 바꾼다 */
    private fun swapIfNeeded() {
        val dragged = draggedItem() ?: return
        val from = items.indexOfFirst { it.id == draggingId }
        // 방금 바꾼 자리가 아직 화면에 반영되기 전이면 기다린다(두 번 바뀌어 되돌아가지 않게)
        if (dragged.index - indexOfFirstCategory() != from) return
        val center = dragged.offset + dragged.size / 2f + offsetY
        val under = listState.layoutInfo.visibleItemsInfo
            .firstOrNull { it.key is Long && it.key != draggingId && center >= it.offset && center < it.offset + it.size }
            ?: return
        val to = items.indexOfFirst { it.id == under.key }
        if (from < 0 || to < 0) return
        items = items.toMutableList().apply { add(to, removeAt(from)) }
        // 줄의 원래 자리가 바뀐 만큼 되돌려 손가락 아래에 그대로 보이게 한다
        val newOffset = if (to > from) under.offset + under.size - dragged.size else under.offset
        offsetY += dragged.offset - newOffset
    }

    private companion object {
        const val EDGE_ZONE = 0.08f
        const val EDGE_SCROLL_STEP = 10f
    }
}

@Composable
fun rememberCategoryDragState(listState: LazyListState): CategoryDragState {
    val haptic = LocalHapticFeedback.current
    val state = remember(listState) { CategoryDragState(listState, haptic) }
    // 끄는 동안 가장자리면 조금씩 스크롤한다
    LaunchedEffect(state.draggingId) {
        while (state.draggingId != null) {
            val amount = state.edgeScrollAmount()
            if (amount != 0f) state.onScrolled(listState.scrollBy(amount))
            delay(16)
        }
    }
    return state
}

/** 줄에 붙이는 꾹 눌러 끌기. 놓으면 [onDrop] 에 새 순서를 넘긴다 */
fun Modifier.categoryDraggable(id: Long, state: CategoryDragState, onDrop: (List<Long>) -> Unit): Modifier =
    pointerInput(id) {
        var before: List<Category> = emptyList()
        detectDragGesturesAfterLongPress(
            onDragStart = {
                before = state.items
                state.start(id)
            },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
            onDragEnd = { onDrop(state.end()) },
            onDragCancel = { state.cancel(before) },
        )
    }
