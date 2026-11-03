package com.kikilog.routinecalendar.ui.common

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.core.model.IntervalUnit

/** 하나만 고르는 단추 줄(분류 · 주기 단위). 등록 화면과 빠른 선택 편집이 함께 쓴다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> ChoiceRow(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier.fillMaxWidth(),
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
            ) {
                Text(label(option))
            }
        }
    }
}

@Composable
fun unitLabel(unit: IntervalUnit): String = stringResource(
    when (unit) {
        IntervalUnit.DAY -> R.string.unit_day
        IntervalUnit.WEEK -> R.string.unit_week
        IntervalUnit.MONTH -> R.string.unit_month
    },
)
