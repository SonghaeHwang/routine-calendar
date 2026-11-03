package com.kikilog.routinecalendar.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.core.model.Category

/** 분류 색 */
val Category.color: Color get() = Color(colorArgb)

/** id 로 분류 찾기. 지워진 id 면 null */
fun List<Category>.byId(id: Long): Category? = firstOrNull { it.id == id }

/** 분류 색 점. 루틴 줄 · 달력 칸 · 칩 앞에 붙인다 */
@Composable
fun CategoryDot(color: Color, modifier: Modifier = Modifier, size: Dp = 10.dp) {
    Box(modifier = modifier.size(size).background(color, CircleShape))
}

/**
 * 분류 하나를 고르는 칩 줄(색 점 + 이름). 분류가 많아도 줄을 바꿔 보인다.
 * @param onEdit null 이 아니면 끝에 「분류 편집」 칩을 붙인다
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryChips(
    categories: List<Category>,
    selectedId: Long,
    onSelect: (Long) -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        categories.forEach { category ->
            FilterChip(
                selected = category.id == selectedId,
                onClick = { onSelect(category.id) },
                label = { Text(category.name) },
                leadingIcon = { CategoryDot(category.color) },
            )
        }
        if (onEdit != null) {
            AssistChip(onClick = onEdit, label = { Text(stringResource(R.string.categories_edit)) })
        }
    }
}
