package com.jyhs.educheck.feature_admin.assignments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.CenteredMessage
import com.jyhs.educheck.core.ui.CenteredProgress
import com.jyhs.educheck.core.ui.ConfirmDialog
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.rememberErrorHost

@Composable
fun AssignmentDetailScreen(
    onBack: () -> Unit,
    vm: AssignmentDetailViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown, ui.errorArg)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val weekdayNames = stringArrayResource(R.array.weekday_names)
    LaunchedEffect(ui.deleted) { if (ui.deleted) onBack() }

    val assignment = ui.assignment
    SubScreenScaffold(
        title = R.string.admin_assignment_detail,
        onBack = onBack,
        snackbarHost = snackbar,
        floatingActionButton = {
            if (assignment != null) {
                FloatingActionButton(onClick = vm::onAddSchedule) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.admin_schedule_new))
                }
            }
        },
    ) { padding ->
        when {
            ui.loading -> CenteredProgress(Modifier.padding(padding))
            assignment == null -> CenteredMessage(R.string.error_not_found, Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding)) {
                item {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(assignment.subjectName, style = MaterialTheme.typography.headlineSmall)
                        Text(assignment.groupName + " · " + ui.teacherName, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            stringResource(R.string.schedules_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        if (ui.schedules.isEmpty()) {
                            Text(
                                stringResource(R.string.schedules_empty),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                items(ui.schedules, key = { it.id }) { schedule ->
                    ListItem(
                        headlineContent = {
                            Text(weekdayNames.getOrElse(schedule.weekday - 1) { "?" } + " " + schedule.startTime + "–" + schedule.endTime)
                        },
                        supportingContent = { Text(stringResource(R.string.schedule_room, schedule.room)) },
                        trailingContent = {
                            IconButton(onClick = { vm.onDeleteSchedule(schedule) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        },
                    )
                    HorizontalDivider()
                }
                item {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = !ui.working,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                    ) {
                        Text(stringResource(R.string.action_delete_assignment), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    ui.form?.let { form ->
        AlertDialog(
            onDismissRequest = vm::onDismissForm,
            title = { Text(stringResource(R.string.admin_schedule_new)) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.field_weekdays), style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        weekdayNames.forEachIndexed { index, name ->
                            val day = index + 1
                            FilterChip(
                                selected = day in form.weekdays,
                                onClick = { vm.onToggleWeekday(day) },
                                label = { Text(name.take(3)) },
                            )
                        }
                    }
                    OutlinedTextField(
                        value = form.startTime,
                        onValueChange = vm::onStartChange,
                        label = { Text(stringResource(R.string.field_start_time)) },
                        placeholder = { Text("07:00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = form.endTime,
                        onValueChange = vm::onEndChange,
                        label = { Text(stringResource(R.string.field_end_time)) },
                        placeholder = { Text("08:00") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = form.room,
                        onValueChange = vm::onRoomChange,
                        label = { Text(stringResource(R.string.field_room)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = vm::onSaveSchedule, enabled = !form.saving) {
                    Text(stringResource(R.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = vm::onDismissForm) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = R.string.confirm_delete_assignment_title,
            message = R.string.confirm_delete_assignment_message,
            confirmLabel = R.string.action_delete,
            onConfirm = {
                confirmDelete = false
                vm.onDeleteAssignment()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
