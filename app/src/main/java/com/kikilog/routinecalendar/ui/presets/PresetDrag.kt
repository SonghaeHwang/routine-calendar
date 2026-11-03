package com.kikilog.routinecalendar.ui.presets

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
import kotlinx.coroutines.delay

/** 목록 머리글의 키. 놓을 자리를 찾을 때 분류 id 로 되돌린다 */
fun categoryHeaderKey(categoryId: Long): String = "header-$categoryId"

/**
 * 빠른 선택 줄을 꾹 눌러 끌어 다른 분류에 놓기. 라이브러리 없이 [LazyListState.layoutInfo] 로
 * 손가락 아래 줄을 찾아, 그 줄이 속한 분류(머리글이면 그 분류)를 놓을 자리로 본다.
 * 목록 위 · 아래 가장자리에 가까우면 저절로 스크롤한다.
 */
class PresetDragState(
    private val listState: LazyListState,
    private val haptic: HapticFeedback,
) {
    /** 끌고 있는 빠른 선택. null 이면 끌지 않는 중 */
    var draggingId by mutableStateOf<Long?>(null)
        private set

    /** 끌기 시작한 자리에서 움직인 거리(px). 줄을 이만큼 옮겨 그린다 */
    var offsetY by mutableFloatStateOf(0f)
        private set

    /** 지금 놓으면 들어갈 분류 */
    var targetCategoryId by mutableStateOf<Long?>(null)
        private set

    private var categoryOfPreset: Map<Long, Long> = emptyMap()

    fun update(categoryOfPreset: Map<Long, Long>) {
        this.categoryOfPreset = categoryOfPreset
    }

    fun start(presetId: Long) {
        draggingId = presetId
        offsetY = 0f
        targetCategoryId = categoryOfPreset[presetId]
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

    fun drag(dy: Float) {
        offsetY += dy
        findTarget()
    }

    /** 놓으면 (빠른 선택 id, 분류 id). 같은 분류면 null */
    fun end(): Pair<Long, Long>? {
        val id = draggingId
        val target = targetCategoryId
        cancel()
        if (id == null || target == null || categoryOfPreset[id] == target) return null
        return id to target
    }

    fun cancel() {
        draggingId = null
        offsetY = 0f
        targetCategoryId = null
    }

    /** 가장자리 근처면 스크롤할 양(px). 위는 음수 */
    fun edgeScrollAmount(): Float {
        val info = draggedItem() ?: return 0f
        val center = info.offset + info.size / 2f + offsetY
        val top = listState.layoutInfo.viewportStartOffset
        val bottom = listState.layoutInfo.viewportEndOffset
        // 가장자리 영역이 넓거나 빠르면 놓으려던 분류를 지나친다(휴대폰 확인)
        val edge = (bottom - top) * EDGE_ZONE
        return when {
            center < top + edge -> -EDGE_SCROLL_STEP
            center > bottom - edge -> EDGE_SCROLL_STEP
            else -> 0f
        }
    }

    /** 스크롤하면 줄이 [consumed] 만큼 반대로 움직이므로 손가락 자리에 머물게 보정한다 */
    fun onScrolled(consumed: Float) {
        offsetY += consumed
        findTarget()
    }

    private fun draggedItem() = listState.layoutInfo.visibleItemsInfo.firstOrNull { it.key == draggingId }

    private fun findTarget() {
        val dragged = draggedItem() ?: return
        val center = dragged.offset + dragged.size / 2f + offsetY
        val under = listState.layoutInfo.visibleItemsInfo
            .filter { it.key != draggingId }
            .firstOrNull { center >= it.offset && center < it.offset + it.size }
            ?: return
        targetCategoryId = when (val key = under.key) {
            is Long -> categoryOfPreset[key]
            is String -> key.removePrefix("header-").toLongOrNull()
            else -> null
        } ?: targetCategoryId
    }

    private companion object {
        const val EDGE_ZONE = 0.08f
        const val EDGE_SCROLL_STEP = 10f
    }
}

@Composable
fun rememberPresetDragState(listState: LazyListState): PresetDragState {
    val haptic = LocalHapticFeedback.current
    val state = remember(listState) { PresetDragState(listState, haptic) }
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

/** 줄에 붙이는 길게 눌러 끌기 */
fun Modifier.presetDraggable(presetId: Long, state: PresetDragState, onDrop: (Long, Long) -> Unit): Modifier =
    pointerInput(presetId) {
        detectDragGesturesAfterLongPress(
            onDragStart = { state.start(presetId) },
            onDrag = { change, amount ->
                change.consume()
                state.drag(amount.y)
            },
            onDragEnd = { state.end()?.let { (id, category) -> onDrop(id, category) } },
            onDragCancel = { state.cancel() },
        )
    }
