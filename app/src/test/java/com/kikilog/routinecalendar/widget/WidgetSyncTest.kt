package com.kikilog.routinecalendar.widget

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WidgetSyncTest {

    @Test
    fun `첫 값은 건너뛰고, 연달아 바뀌면 한 번만 다시 그린다`() = runTest {
        val changes = MutableStateFlow<Any>(0)
        var updates = 0
        val sync = WidgetSync(changes, backgroundScope, update = { updates++ })

        sync.start()
        runCurrent()
        advanceTimeBy(WIDGET_SYNC_DEBOUNCE_MILLIS * 2)
        assertEquals(0, updates)

        changes.value = 1
        changes.value = 2
        changes.value = 3
        advanceTimeBy(WIDGET_SYNC_DEBOUNCE_MILLIS + 1)
        runCurrent()
        assertEquals(1, updates)
    }

    @Test
    fun `두 번 시작해도 구독은 하나`() = runTest {
        val changes = MutableStateFlow<Any>(0)
        var updates = 0
        val sync = WidgetSync(changes, backgroundScope, update = { updates++ })

        sync.start()
        sync.start()
        runCurrent()
        changes.value = 1
        advanceTimeBy(WIDGET_SYNC_DEBOUNCE_MILLIS + 1)
        runCurrent()
        assertEquals(1, updates)
    }

    @Test
    fun `다시 그리기가 실패해도 다음 변경을 계속 받는다`() = runTest {
        val changes = MutableStateFlow<Any>(0)
        var calls = 0
        val sync = WidgetSync(changes, backgroundScope, update = { calls++; error("위젯 없음") })

        sync.start()
        runCurrent()
        changes.value = 1
        advanceTimeBy(WIDGET_SYNC_DEBOUNCE_MILLIS + 1)
        runCurrent()
        changes.value = 2
        advanceTimeBy(WIDGET_SYNC_DEBOUNCE_MILLIS + 1)
        runCurrent()
        assertEquals(2, calls)
    }
}
