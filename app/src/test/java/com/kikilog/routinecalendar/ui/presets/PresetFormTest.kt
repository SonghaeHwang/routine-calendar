package com.kikilog.routinecalendar.ui.presets

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.Preset
import com.kikilog.routinecalendar.ui.edit.AmountError
import com.kikilog.routinecalendar.ui.edit.TitleError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PresetFormTest {

    @Test
    fun `이름과 주기가 있으면 프리셋으로 바뀐다 - 앞뒤 공백 제거`() {
        val preset = PresetForm(title = " 손톱 정리 ", category = Category.BEAUTY, amountText = "2", unit = IntervalUnit.WEEK).toPresetOrNull()
        assertEquals(Preset("손톱 정리", Category.BEAUTY, Interval(2, IntervalUnit.WEEK)), preset)
    }

    @Test
    fun `주기 칸을 비우면 직접 입력 항목`() {
        val form = PresetForm(title = "리들샷", amountText = "")
        assertNull(form.amountError)
        assertNull(form.toPresetOrNull()!!.interval)
    }

    @Test
    fun `빈 이름 · 0 · 366 은 거부`() {
        assertEquals(TitleError.EMPTY, PresetForm(title = "  ").titleError)
        assertEquals(AmountError.INVALID, PresetForm(title = "a", amountText = "0").amountError)
        assertEquals(AmountError.TOO_LARGE, PresetForm(title = "a", amountText = "366").amountError)
        assertNull(PresetForm(title = "a", amountText = "0").toPresetOrNull())
    }

    @Test
    fun `기존 프리셋에서 불러오면 값과 번호를 그대로 담는다`() {
        val preset = Preset("젤팩", Category.BEAUTY, Interval(1, IntervalUnit.WEEK), repeats = true, id = 7)
        val form = PresetForm.from(preset)
        assertEquals(7L, form.id)
        assertEquals("1", form.amountText)
        assertEquals(preset, form.toPresetOrNull())

        val custom = PresetForm.from(Preset("리들샷", Category.BEAUTY, null, id = 8))
        assertEquals("", custom.amountText)
    }
}
