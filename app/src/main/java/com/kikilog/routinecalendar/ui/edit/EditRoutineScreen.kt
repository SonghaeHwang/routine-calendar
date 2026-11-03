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
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import com.kikilog.routinecalendar.core.model.IntervalUnit
import com.kikilog.routinecalendar.core.preset.DEFAULT_PRESETS
import com.kikilog.routinecalendar.core.preset.Preset
import com.kikilog.routinecalendar.ui.common.SubScaffold
import com.kikilog.routinecalendar.ui.format.categoryLabel
import com.kikilog.routinecalendar.ui.format.formatDate
import com.kikilog.routinecalendar.ui.format.pickerMillisToDate
import com.kikilog.routinecalendar.ui.format.toPickerMillis

/** 화면 이동에서 부르는 입구. ViewModel 은 이 화면이 닫힐 때 함께 정리된다. */
@Composable
fun EditRoutineRoute(onDone: () -> Unit) {
    val viewModel: EditRoutineViewModel = viewModel(factory = EditRoutineViewModel.Factory)
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state.saved) {
        if (state.saved) onDone()
    }

    EditRoutineScreen(
        state = state,
        onBack = onDone,
        onTitleChange = viewModel::onTitleChange,
        onCategoryChange = viewModel::onCategoryChange,
        onAmountChange = viewModel::onAmountChange,
        onUnitChange = viewModel::onUnitChange,
        onRepeatsChange = viewModel::onRepeatsChange,
        onNotifyChange = viewModel::onNotifyChange,
        onLastDoneChange = viewModel::onLastDoneChange,
        onPreset = viewModel::applyPreset,
        onSave = viewModel::save,
        onSaveFailedShown = viewModel::onSaveFailedShown,
    )
}

@Composable
fun EditRoutineScreen(
    state: EditUiState,
    onBack: () -> Unit,
    onTitleChange: (String) -> Unit,
    onCategoryChange: (Category) -> Unit,
    onAmountChange: (String) -> Unit,
    onUnitChange: (IntervalUnit) -> Unit,
    onRepeatsChange: (Boolean) -> Unit,
    onNotifyChange: (Boolean) -> Unit,
    onLastDoneChange: (kotlinx.datetime.LocalDate?) -> Unit,
    onPreset: (Preset) -> Unit,
    onSave: () -> Unit,
    onSaveFailedShown: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val saveFailedMessage = stringResource(R.string.error_save_failed)
    LaunchedEffect(state.saveFailed) {
        if (state.saveFailed) {
            snackbarHostState.showSnackbar(saveFailedMessage)
            onSaveFailedShown()
        }
    }

    SubScaffold(
        title = stringResource(R.string.add_routine),
        onBack = onBack,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            PresetSection(onPreset)
            HorizontalDivider()

            OutlinedTextField(
                value = state.title,
                onValueChange = onTitleChange,
                label = { Text(stringResource(R.string.label_title)) },
                singleLine = true,
                isError = state.showErrors && state.titleError != null,
                supportingText = if (state.showErrors && state.titleError != null) {
                    { Text(stringResource(R.string.error_title_empty)) }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Section(stringResource(R.string.label_category)) {
                ChoiceRow(
                    options = Category.entries,
                    selected = state.category,
                    onSelect = onCategoryChange,
                    label = { categoryLabel(it) },
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
                    Text(stringResource(if (state.repeats) R.string.repeats_on_hint else R.string.repeats_off_hint))
                },
                trailingContent = { Switch(checked = state.repeats, onCheckedChange = onRepeatsChange) },
            )

            LastDoneField(
                lastDoneOn = state.lastDoneOn,
                today = state.today,
                onChange = onLastDoneChange,
            )

            ListItem(
                headlineContent = { Text(stringResource(R.string.label_notify)) },
                supportingContent = { Text(stringResource(R.string.notify_hint)) },
                trailingContent = { Switch(checked = state.notify, onCheckedChange = onNotifyChange) },
            )

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
private fun PresetSection(onPreset: (Preset) -> Unit) {
    Section(stringResource(R.string.label_presets)) {
        DEFAULT_PRESETS.groupBy { it.category }.forEach { (category, presets) ->
            Text(
                text = categoryLabel(category),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                presets.forEach { preset ->
                    SuggestionChip(onClick = { onPreset(preset) }, label = { Text(preset.title) })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LastDoneField(
    lastDoneOn: kotlinx.datetime.LocalDate?,
    today: kotlinx.datetime.LocalDate,
    onChange: (kotlinx.datetime.LocalDate?) -> Unit,
) {
    var showPicker by rememberSaveable { mutableStateOf(false) }

    Section(stringResource(R.string.label_last_done)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { showPicker = true }, modifier = Modifier.weight(1f)) {
                Text(lastDoneOn?.let { formatDate(it, today) } ?: stringResource(R.string.last_done_none))
            }
            if (lastDoneOn != null) {
                TextButton(onClick = { onChange(null) }) { Text(stringResource(R.string.clear)) }
            }
        }
        Text(
            text = stringResource(R.string.last_done_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (showPicker) {
        val todayMillis = today.toPickerMillis()
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = (lastDoneOn ?: today).toPickerMillis(),
            selectableDates = object : SelectableDates {
                // 아직 오지 않은 날은 고를 수 없다
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis <= todayMillis
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun <T> ChoiceRow(
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
private fun unitLabel(unit: IntervalUnit): String = stringResource(
    when (unit) {
        IntervalUnit.DAY -> R.string.unit_day
        IntervalUnit.WEEK -> R.string.unit_week
        IntervalUnit.MONTH -> R.string.unit_month
    },
)
