package com.kikilog.routinecalendar.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.ColorFamily
import com.kikilog.routinecalendar.data.CategoryRepository
import com.kikilog.routinecalendar.ui.common.CategoryDot
import com.kikilog.routinecalendar.ui.common.SubScaffold
import com.kikilog.routinecalendar.ui.common.color

@Composable
fun CategoriesRoute(onBack: () -> Unit) {
    val viewModel: CategoriesViewModel = viewModel(factory = CategoriesViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    CategoriesScreen(
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onReorder = viewModel::reorder,
        onRequestDelete = viewModel::requestDelete,
        onConfirmDelete = viewModel::confirmDelete,
        onDismissDelete = viewModel::dismissDelete,
    )
}

/**
 * 분류를 누르거나 「수정」 으로 이름 · 색을 고치고, 꾹 눌러 끌어 순서를 바꾼다.
 * 오른쪽 위 + 로 추가, 각 줄의 삭제. 「기타」는 지울 수 없다
 */
@Composable
fun CategoriesScreen(
    state: CategoriesUiState,
    onBack: () -> Unit,
    onSave: (id: Long?, name: String, colorArgb: Int?) -> Boolean,
    onRequestDelete: (Category) -> Unit,
    onConfirmDelete: () -> Unit,
    onDismissDelete: () -> Unit,
    onReorder: (List<Long>) -> Unit = {},
) {
    // 편집 중인 분류. NEW 면 새 분류, null 이면 대화상자 닫힘
    var editing by remember { mutableStateOf<Category?>(null) }
    val listState = rememberLazyListState()
    val drag = rememberCategoryDragState(listState)
    // 끌지 않을 때는 저장된 순서를 따른다. 끌는 동안에는 화면 안 순서만 바꾼다
    LaunchedEffect(state.categories) { drag.sync(state.categories) }

    SubScaffold(
        title = stringResource(R.string.categories_edit),
        onBack = onBack,
        actions = {
            IconButton(onClick = { editing = NEW }) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.category_add))
            }
        },
    ) { padding ->
        if (state.isLoading) return@SubScaffold
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
        ) {
            item(key = "hint") {
                Text(
                    text = stringResource(R.string.categories_reorder_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            }
            items(drag.items, key = { it.id }) { category ->
                val dragging = drag.draggingId == category.id
                Column(
                    modifier = Modifier
                        .then(if (dragging) Modifier.zIndex(1f) else Modifier.animateItem())
                        .graphicsLayer { translationY = if (dragging) drag.offsetY else 0f }
                        .categoryDraggable(category.id, drag) { onReorder(it) },
                ) {
                    ListItem(
                        modifier = Modifier.clickable(onClickLabel = stringResource(R.string.category_edit)) { editing = category },
                        leadingContent = { CategoryDot(category.color, size = 16.dp) },
                        headlineContent = { Text(category.name) },
                        supportingContent = if (!category.removable) {
                            { Text(stringResource(R.string.category_etc_hint)) }
                        } else {
                            null
                        },
                        trailingContent = {
                            Row {
                                TextButton(onClick = { editing = category }) { Text(stringResource(R.string.action_edit)) }
                                if (category.removable) {
                                    TextButton(onClick = { onRequestDelete(category) }) { Text(stringResource(R.string.action_delete)) }
                                }
                            }
                        },
                        colors = ListItemDefaults.colors(
                            containerColor = if (dragging) MaterialTheme.colorScheme.surfaceContainerHigh else MaterialTheme.colorScheme.surface,
                        ),
                        tonalElevation = if (dragging) 4.dp else 0.dp,
                        shadowElevation = if (dragging) 4.dp else 0.dp,
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    editing?.let { category ->
        CategoryDialog(
            initial = category.takeIf { it !== NEW },
            onDismiss = { editing = null },
            onSave = { name, color -> if (onSave(category.takeIf { it !== NEW }?.id, name, color)) editing = null },
        )
    }

    state.pendingDelete?.let { pending ->
        AlertDialog(
            onDismissRequest = onDismissDelete,
            title = { Text(stringResource(R.string.category_delete_title, pending.category.name)) },
            text = {
                Text(
                    if (pending.itemCount > 0) {
                        stringResource(R.string.category_delete_body_move, pending.itemCount)
                    } else {
                        stringResource(R.string.category_delete_body_empty)
                    },
                )
            },
            confirmButton = { TextButton(onClick = onConfirmDelete) { Text(stringResource(R.string.action_delete)) } },
            dismissButton = { TextButton(onClick = onDismissDelete) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** 새 분류를 뜻하는 자리표시 */
private val NEW = Category(id = -1, name = "", colorArgb = 0, position = -1)

/**
 * 분류 이름 · 색 대화상자. 색은 계열(빨 · 주 · 노 · 초 · 파 · 남 · 보 · 분홍 · 회색)을 누르면
 * 그 계열의 진한 색 → 연한 색 5단계가 나온다. 새 분류는 색을 고르지 않으면 자동으로 정한다.
 */
@Composable
private fun CategoryDialog(initial: Category?, onDismiss: () -> Unit, onSave: (name: String, colorArgb: Int?) -> Unit) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var color by remember { mutableStateOf(initial?.colorArgb) }
    var family by remember { mutableStateOf(color?.let(ColorFamily::of)) }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial == null) R.string.category_add else R.string.category_edit)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(CategoryRepository.MAX_NAME_LENGTH) },
                    label = { Text(stringResource(R.string.category_name)) },
                    singleLine = true,
                    isError = showError && name.isBlank(),
                    supportingText = if (showError && name.isBlank()) {
                        { Text(stringResource(R.string.error_title_empty)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.category_color), style = MaterialTheme.typography.titleSmall)
                ColorSwatches(
                    colors = ColorFamily.entries.map { it.defaultColor },
                    selected = family?.defaultColor,
                    labels = ColorFamily.entries.map { it.label },
                    onSelect = { picked ->
                        val f = ColorFamily.of(picked)
                        family = f
                        // 계열을 바꾸면 우선 가운데 단계를 고른다
                        if (f != null && ColorFamily.of(color ?: 0) != f) color = f.defaultColor
                    },
                )
                family?.let { f ->
                    Text(stringResource(R.string.category_shade, f.label), style = MaterialTheme.typography.bodySmall)
                    ColorSwatches(
                        colors = f.shades,
                        selected = color,
                        labels = f.shades.indices.map { stringResource(R.string.category_shade_level, it + 1) },
                        onSelect = { color = it },
                    )
                }
                if (initial == null && color == null) {
                    Text(
                        stringResource(R.string.category_color_auto),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                showError = true
                if (name.isNotBlank()) onSave(name, color)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

/** 동그란 색 견본 줄. 고른 색은 테두리로 보인다 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSwatches(colors: List<Int>, selected: Int?, labels: List<String>, onSelect: (Int) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        colors.forEachIndexed { index, argb ->
            val isSelected = argb == selected
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outlineVariant,
                        shape = CircleShape,
                    )
                    .padding(if (isSelected) 5.dp else 2.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.RadioButton) { onSelect(argb) }
                    .semantics {
                        contentDescription = labels.getOrElse(index) { "" }
                        this.selected = isSelected
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(modifier = Modifier.fillMaxSize().background(Color(argb), CircleShape))
            }
        }
    }
}
