package com.kikilog.routinecalendar.ui.presets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.ui.common.CategoryChips
import com.kikilog.routinecalendar.ui.common.CategoryDot
import com.kikilog.routinecalendar.ui.common.color
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.Preset
import com.kikilog.routinecalendar.ui.common.ChoiceRow
import com.kikilog.routinecalendar.ui.common.EmptyState
import com.kikilog.routinecalendar.ui.common.SubScaffold
import com.kikilog.routinecalendar.ui.common.unitLabel
import com.kikilog.routinecalendar.ui.edit.AmountError
import com.kikilog.routinecalendar.ui.format.intervalLabel

@Composable
fun PresetsRoute(onBack: () -> Unit) {
    val viewModel: PresetsViewModel = viewModel(factory = PresetsViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()
    PresetsScreen(
        state = state,
        onBack = onBack,
        onSave = viewModel::save,
        onDelete = viewModel::delete,
        onReset = viewModel::resetToDefaults,
    )
}

/** 빠른 선택 편집: 누르면 고치고, 오른쪽 위 + 로 추가, 각 줄의 삭제, ⋮ 로 기본값 되돌리기 */
@Composable
fun PresetsScreen(
    state: PresetsUiState,
    onBack: () -> Unit,
    onSave: (PresetForm) -> Boolean,
    onDelete: (Long) -> Unit,
    onReset: () -> Unit,
) {
    // 편집 중인 항목. null 이면 대화상자 닫힘
    var editing by remember { mutableStateOf<PresetForm?>(null) }
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var confirmReset by rememberSaveable { mutableStateOf(false) }

    SubScaffold(
        title = stringResource(R.string.settings_presets),
        onBack = onBack,
        actions = {
            IconButton(onClick = { editing = PresetForm() }) {
                Icon(painterResource(R.drawable.ic_add), contentDescription = stringResource(R.string.preset_add))
            }
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.more))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.presets_reset)) },
                        onClick = {
                            menuOpen = false
                            confirmReset = true
                        },
                    )
                }
            }
        },
    ) { padding ->
        when {
            state.isLoading -> Unit
            state.presets.isEmpty() -> EmptyState(stringResource(R.string.presets_empty), padding)
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = padding.calculateTopPadding(), bottom = padding.calculateBottomPadding()),
            ) {
                val groups = state.presets.groupBy { it.categoryId }
                state.categories.forEach { category ->
                    val group = groups[category.id].orEmpty()
                    item(key = "header-${category.id}") {
                        CategoryHeader(category)
                    }
                    items(group, key = { it.id }) { preset ->
                        PresetRow(
                            preset = preset,
                            onClick = { editing = PresetForm.from(preset) },
                            onDelete = { onDelete(preset.id) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    editing?.let { form ->
        PresetDialog(
            initial = form,
            categories = state.categories,
            onDismiss = { editing = null },
            onSave = { edited -> if (onSave(edited)) editing = null },
        )
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.presets_reset_title)) },
            text = { Text(stringResource(R.string.presets_reset_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onReset()
                }) { Text(stringResource(R.string.presets_reset_confirm)) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

/** 분류 머리글(색 점 + 이름) */
@Composable
private fun CategoryHeader(category: Category, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)
            .semantics { heading() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryDot(category.color)
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun PresetRow(preset: Preset, onClick: () -> Unit, onDelete: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(preset.title) },
        supportingContent = {
            Text(preset.interval?.let { intervalLabel(it, preset.repeats) } ?: stringResource(R.string.preset_custom_interval))
        },
        trailingContent = {
            TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete)) }
        },
    )
}

/** 빠른 선택 하나를 더하거나 고치는 대화상자. 저장을 누른 뒤부터 오류를 보여 준다. */
@Composable
private fun PresetDialog(
    initial: PresetForm,
    categories: List<Category>,
    onDismiss: () -> Unit,
    onSave: (PresetForm) -> Unit,
) {
    var form by remember(initial) { mutableStateOf(initial) }
    var showErrors by remember(initial) { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (initial.id == null) R.string.preset_add else R.string.preset_edit)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = form.title,
                    onValueChange = { form = form.copy(title = it.take(30)) },
                    label = { Text(stringResource(R.string.label_title)) },
                    singleLine = true,
                    isError = showErrors && form.titleError != null,
                    supportingText = if (showErrors && form.titleError != null) {
                        { Text(stringResource(R.string.error_title_empty)) }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                CategoryChips(
                    categories = categories,
                    selectedId = form.categoryId,
                    onSelect = { form = form.copy(categoryId = it) },
                )
                Row(verticalAlignment = Alignment.Top) {
                    OutlinedTextField(
                        value = form.amountText,
                        onValueChange = { text -> form = form.copy(amountText = text.filter(Char::isDigit).take(3)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = showErrors && form.amountError != null,
                        modifier = Modifier.width(80.dp),
                    )
                    ChoiceRow(
                        options = IntervalUnit.entries,
                        selected = form.unit,
                        onSelect = { form = form.copy(unit = it) },
                        label = { unitLabel(it) },
                        modifier = Modifier
                            .padding(start = 8.dp, top = 4.dp)
                            .weight(1f),
                    )
                }
                Text(
                    text = if (showErrors && form.amountError != null) {
                        stringResource(
                            if (form.amountError == AmountError.TOO_LARGE) R.string.error_amount_too_large else R.string.error_amount_invalid,
                        )
                    } else {
                        stringResource(R.string.preset_amount_hint)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (showErrors && form.amountError != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.label_repeats), modifier = Modifier.weight(1f))
                    Switch(checked = form.repeats, onCheckedChange = { form = form.copy(repeats = it) })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                showErrors = true
                if (form.isValid) onSave(form)
            }) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
