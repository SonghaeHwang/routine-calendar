package com.kikilog.routinecalendar.ui.presets

import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.Interval
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.Preset
import com.kikilog.routinecalendar.ui.edit.AmountError
import com.kikilog.routinecalendar.ui.edit.MAX_INTERVAL_AMOUNT
import com.kikilog.routinecalendar.ui.edit.TitleError

/**
 * 빠른 선택 하나를 고치는 대화상자의 입력값. 주기 칸은 비워도 된다(등록할 때 직접 입력).
 * @property id null 이면 새 항목
 */
data class PresetForm(
    val id: Long? = null,
    val title: String = "",
    val categoryId: Long = Category.HOUSE,
    val amountText: String = "1",
    val unit: IntervalUnit = IntervalUnit.WEEK,
    val repeats: Boolean = true,
) {
    val titleError: TitleError? get() = if (title.isBlank()) TitleError.EMPTY else null

    val amountError: AmountError?
        get() {
            if (amountText.isBlank()) return null
            val amount = amountText.toIntOrNull() ?: return AmountError.INVALID
            return when {
                amount < 1 -> AmountError.INVALID
                amount > MAX_INTERVAL_AMOUNT -> AmountError.TOO_LARGE
                else -> null
            }
        }

    val isValid: Boolean get() = titleError == null && amountError == null

    /** 검사를 통과했을 때만 값이 있다 */
    fun toPresetOrNull(): Preset? {
        if (!isValid) return null
        val interval = amountText.toIntOrNull()?.let { Interval(it, unit) }
        return Preset(title = title.trim(), categoryId = categoryId, interval = interval, repeats = repeats, id = id ?: 0)
    }

    companion object {
        fun from(preset: Preset): PresetForm = PresetForm(
            id = preset.id,
            title = preset.title,
            categoryId = preset.categoryId,
            amountText = preset.interval?.amount?.toString() ?: "",
            unit = preset.interval?.unit ?: IntervalUnit.WEEK,
            repeats = preset.repeats,
        )
    }
}
