package com.jyhs.educheck.feature_admin.groups

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.CenteredProgress
import com.jyhs.educheck.core.ui.ConfirmDialog
import com.jyhs.educheck.core.ui.RadioRow
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.labelRes
import com.jyhs.educheck.core.ui.rememberErrorHost
import com.jyhs.educheck.domain.model.Shift

@Composable
fun GroupEditScreen(
    onBack: () -> Unit,
    onDeleted: () -> Unit,
    vm: GroupEditViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown)
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(ui.saved) { if (ui.saved) onBack() }
    LaunchedEffect(ui.deleted) { if (ui.deleted) onDeleted() }

    SubScreenScaffold(
        title = if (ui.isNew) R.string.admin_group_new else R.string.admin_group_edit,
        onBack = onBack,
        snackbarHost = snackbar,
    ) { padding ->
        if (ui.loading) {
            CenteredProgress(Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = ui.name,
                    onValueChange = vm::onNameChange,
                    label = { Text(stringResource(R.string.field_group_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ui.semester,
                    onValueChange = vm::onSemesterChange,
                    label = { Text(stringResource(R.string.field_semester)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ui.period,
                    onValueChange = vm::onPeriodChange,
                    label = { Text(stringResource(R.string.field_period)) },
                    supportingText = { Text(stringResource(R.string.field_period_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(R.string.field_shift), style = MaterialTheme.typography.titleSmall)
                Shift.entries.forEach { shift ->
                    RadioRow(
                        selected = ui.shift == shift,
                        label = stringResource(shift.labelRes()),
                        onClick = { vm.onShiftChange(shift) },
                    )
                }

                Button(onClick = vm::save, enabled = !ui.working, modifier = Modifier.fillMaxWidth()) {
                    if (ui.working) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.action_save))
                    }
                }
                if (!ui.isNew) {
                    OutlinedButton(
                        onClick = { confirmDelete = true },
                        enabled = !ui.working,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.action_delete_group), color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = R.string.confirm_delete_group_title,
            message = R.string.confirm_delete_group_message,
            confirmLabel = R.string.action_delete,
            onConfirm = {
                confirmDelete = false
                vm.delete()
            },
            onDismiss = { confirmDelete = false },
        )
    }
}
