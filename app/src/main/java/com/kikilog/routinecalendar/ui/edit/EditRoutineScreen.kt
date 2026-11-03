package com.kikilog.routinecalendar.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kikilog.routinecalendar.R
import com.kikilog.routinecalendar.core.model.Category
import com.kikilog.routinecalendar.ui.common.CategoryChips
import com.kikilog.routinecalendar.ui.common.CategoryDot
import com.kikilog.routinecalendar.ui.common.color
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.Preset
import com.kikilog.routinecalendar.ui.common.ChoiceRow
import com.kikilog.routinecalendar.ui.common.ConfirmDeleteDialog
import com.kikilog.routinecalendar.ui.common.unitLabel
import com.kikilog.routinecalendar.ui.common.RoutineActionsMenu
import com.kikilog.routinecalendar.ui.common.SubScaffold
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.res.painterResource
import com.kikilog.routinecalendar.ui.format.formatDate
import com.kikilog.routinecalendar.ui.format.pickerMillisToDate
import com.kikilog.routinecalendar.ui.format.toPickerMillis
import kotlinx.datetime.LocalDate

/**
 * 화면 이동에서 부르는 입구. ViewModel 은 이 화면이 닫힐 때 함께 정리된다.
 * @param routineId null 이면 등록, 아니면 수정
 * @param onAdded 등록되면 이름 · 다음 예정일 · 오늘을 넘겨 안내 문구를 띄우게 한다(화면은 부르는 쪽이 닫는다)
 * @param onEdited 수정이 저장되면 부른다
 */
@Composable
fun EditRoutineRoute(
    routineId: Long?,
    onBack: () -> Unit,
    onAdded: (title: String, nextDue: LocalDate, today: LocalDate) -> Unit,
    onEdited: () -> Unit,
    onOpenExisting: (Long) -> Unit,
    onOpenCategories: () -> Unit = {},
) {
    val viewModel: EditRoutineViewModel = viewModel(factory = EditRoutineViewModel.factory(routineId))
    val state by viewModel.state.collectAsStateWithLifecycle()

    // 같은 이름 확인 창에서 「기존 루틴 열기」
    LaunchedEffect(state.openExistingId) {
        state.openExistingId?.let(onOpenExisting)
    }

    LaunchedEffect(state.saved, state.notFound, state.closed) {
        val nextDue = state.savedNextDue
        when {
            // 없는 루틴(이미 삭제됨)을 열었거나, 보관 · 삭제했으면 돌아간다
            state.notFound || state.closed -> onBack()
            !state.saved -> Unit
            state.isEdit -> onEdited()
            nextDue != null -> onAdded(state.title.trim(), nextDue, state.today)
        }
    }

    EditRoutineScreen(
        state = state,
        onBack = onBack,
        onTitleChange = viewModel::onTitleChange,
        onCategoryChange = viewModel::onCategoryChange,
        onAmountChange = viewModel::onAmountChange,
        onUnitChange = viewModel::onUnitChange,
        onRepeatsChange = viewModel::onRepeatsChange,
        onNotifyChange = viewModel::onNotifyChange,
        onLastDoneChange = viewModel::onLastDoneChange,
        onFirstDueChange = viewModel::onFirstDueChange,
        onPreset = viewModel::applyPreset,
        onSave = viewModel::save,
        onSaveFailedShown = viewModel::onSaveFailedShown,
        onToggleArchive = viewModel::onToggleArchive,
        onDelete = viewModel::onDelete,
        onAddAnyway = viewModel::onAddAnyway,
        onOpenExistingDuplicate = viewModel::onOpenExisting,
        onDismissDuplicate = viewModel::onDismissDuplicate,
        onOpenCategories = onOpenCategories,
    )
}

@Composable
fun EditRoutineScreen(
    state: EditUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Long) -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (IntervalUnit) -> Unit,
    onRepeatsChange: (Boolean) -> Unit,
    onNotifyChange: (Boolean) -> Unit,
    onLastDoneChange: (LocalDate?) -> Unit,
    onFirstDueChange: (LocalDate) -> Unit,
    onPreset: (Preset) -> Unit,
    onSave: () -> Unit,
    onSaveFailedShown: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit,
    onAddAnyway: () -> Unit,
    onOpenExistingDuplicate: () -> Unit,
    onDismissDuplicate: () -> Unit,
    onOpenCategories: () -> Unit = {},
) {
    var menuOpen by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.error_save_failed)
    LaunchedEffect(state.saveFailed) {
        if (state.saveFailed) {
            snackbarHostState.showSnackbar(saveFailedMessage)
            onSaveFailedShown()
        }
    }

    SubScaffold(
        title = stringResource(if (state.isEdit) R.string.edit_routine else R.string.add_routine),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        actions = {
            if (state.isEdit && !state.isLoading) {
                Box {
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(painterResource(R.drawable.ic_more), contentDescription = stringResource(R.string.more))
                    }
                    RoutineActionsMenu(
                        expanded = menuOpen,
                        archived = state.archived,
                        onDismiss = { menuOpen = false },
                        onToggleArchive = onToggleArchive,
                        onDelete = { confirmDelete = true },
                    )
                }
            }
        },
    ) { padding ->
        if (state.askDuplicate) {
            AlertDialog(
                onDismissRequest = onDismissDuplicate,
                title = { Text(stringResource(R.string.duplicate_title, state.title.trim())) },
                text = { Text(stringResource(R.string.duplicate_body)) },
                confirmButton = {
                    TextButton(onClick = onAddAnyway) { Text(stringResource(R.string.duplicate_add_anyway)) }
                },
                dismissButton = {
                    Row {
                        TextButton(onClick = onDismissDuplicate) { Text(stringResource(R.string.cancel)) }
                        TextButton(onClick = onOpenExistingDuplicate) { Text(stringResource(R.string.duplicate_open)) }
                    }
                },
            )
        }
        if (confirmDelete) {
            ConfirmDeleteDialog(
                title = state.title,
                onConfirm = {
                    confirmDelete = false
                    onDelete()
                },
                onDismiss = { confirmDelete = false },
            )
        }
        // 수정할 루틴을 불러오는 짧은 동안은 빈 칸을 보여 주지 않는다
        if (state.isLoading || state.notFound) return@SubScaffold
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (!state.isEdit && state.presets.isNotEmpty()) {
                PresetSection(state.presets, state.categories, onPreset)
                HorizontalDivider()
            }

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.label_title)) },
                singleLine = true,
                isError = state.showErrors && state.titleError != null,
                supportingText = when {
                    state.showErrors && state.titleError != null -> {
                        { Text(stringResource(R.string.error_title_empty)) }
                    }
                    // 같은 이름이 있으면 입력하는 동안 미리 알려 준다(오류는 아니다)
                    state.duplicate != null -> {
                        {
                            Text(
                                stringResource(
                                    if (state.duplicate?.archived == true) R.string.duplicate_hint_archived else R.string.duplicate_hint,
                                ),
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                    else -> null
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Section(stringResource(R.string.label_category)) {
                CategoryChips(
                    categories = state.categories,
                    selectedId = state.categoryId,
                    onSelect = onCategoryChange,
                    onEdit = onOpenCategories,
                )
            }

            Section(stringResource(R.string.label_interval)) {
                Row(verticalAlignment = Alignment.Top) {
                    OutlinedTextField(
                        value = state.amountText,
                        onValueChange = onAmountChange,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = state.showErrors && state.amountError != null,
                        modifier = Modifier.width(96.dp),
                    )
                    ChoiceRow(
                        options = IntervalUnit.entries,
                        selected = state.unit,
                        onSelect = onUnitChange,
                        label = { unitLabel(it) },
                        modifier = Modifier
                            .padding(start = 12.dp, top = 4.dp)
                            .weight(1f),
                    )
                }
                if (state.showErrors && state.amountError != null) {
                    Text(
                        text = stringResource(
                            when (state.amountError) {
                                AmountError.INVALID -> R.string.error_amount_invalid
                                AmountError.TOO_LARGE -> R.string.error_amount_too_large
                                null -> R.string.error_amount_invalid
                            },
                        ),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.label_repeats)) },
                supportingContent = {
                    Text(
                        stringResource(
                            when {
                                state.repeatsLocked -> R.string.repeats_locked_hint
                                state.repeats -> R.string.repeats_on_hint
                                else -> R.string.repeats_off_hint
                            },
                        ),
                    )
                },
                trailingContent = {
                    Switch(checked = state.repeats, onCheckedChange = onRepeatsChange, enabled = !state.repeatsLocked)
                },
            )

            if (!state.isEdit) {
                DateField(
                    title = stringResource(R.string.label_last_done),
                    date = state.lastDoneOn,
                    today = state.today,
                    emptyText = stringResource(R.string.last_done_none),
                    hint = stringResource(R.string.last_done_hint),
                    allowFuture = false,
                    onChange = onLastDoneChange,
                )
            } else if (!state.hasCompletions) {
                state.firstDue?.let { firstDue ->
                    DateField(
                        title = stringResource(R.string.label_first_due),
                        date = firstDue,
                        today = state.today,
                        emptyText = "",
                        hint = stringResource(R.string.first_due_hint),
                        allowFuture = true,
                        onChange = { it?.let(onFirstDueChange) },
                        clearable = false,
                    )
                }
            }

            ListItem(
                headlineContent = { Text(stringResource(R.string.label_notify)) },
                supportingContent = { Text(stringResource(R.string.notify_hint)) },
                trailingContent = { Switch(checked = state.notify, onCheckedChange = onNotifyChange) },
            )

            // 등록일은 상세(수정) 화면에서만 보인다. 고칠 수 없는 정보다
            state.createdOn?.let { createdOn ->
                Text(
                    text = stringResource(R.string.label_created_on, formatDate(createdOn, state.today)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            Button(
                onClick = onSave,
                enabled = !state.isSaving,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.save))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PresetSection(presets: List<Preset>, categories: List<Category>, onPreset: (Preset) -> Unit) {
    Section(stringResource(R.string.label_presets)) {
        // 분류 편집 순서대로. 빈 분류는 건너뛴다
        val groups = presets.groupBy { it.categoryId }
        categories.mapNotNull { c -> groups[c.id]?.let { c to it } }.forEach { (category, group) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryDot(category.color, size = 8.dp)
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.forEach { preset ->
                    SuggestionChip(onClick = { onPreset(preset) }, label = { Text(preset.title) })
                }
            }
        }
    }
}

/**
 * 날짜 하나를 고르는 칸. 누르면 날짜 선택기가 열린다.
 * @param allowFuture false 면 오늘 이후는 고를 수 없다(마지막으로 한 날)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    title: String,
    date: LocalDate?,
    today: LocalDate,
    emptyText: String,
    hint: String,
    allowFuture: Boolean,
    onChange: (LocalDate?) -> Unit,
    clearable: Boolean = true,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Section(title) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.weight(1f)) {
                Text(date?.let { formatDate(it, today) } ?: emptyText)
            }
            if (clearable && date != null) {
                TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.clear)) }
            }
        }
        Text(
            text = hint,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showPicker) {
        val todayMillis = today.toPickerMillis()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (date ?: today).toPickerMillis(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = allowFuture || utcTimeMillis <= todayMillis
            },
        )
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { onChange(pickerMillisToDate(it)) }
                    showPicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.semantics { heading() },
        )
        content()
    }
}

