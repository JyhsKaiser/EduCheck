package com.jyhs.educheck.feature_admin.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.CenteredMessage
import com.jyhs.educheck.core.ui.CenteredProgress
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.rememberErrorHost

@Composable
fun SubjectsScreen(
    onBack: () -> Unit,
    vm: SubjectsViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown)

    SubScreenScaffold(
        title = R.string.admin_subjects_title,
        onBack = onBack,
        snackbarHost = snackbar,
        floatingActionButton = {
            FloatingActionButton(onClick = vm::onAdd) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.admin_subject_new))
            }
        },
    ) { padding ->
        when {
            ui.loading -> CenteredProgress(Modifier.padding(padding))
            ui.subjects.isEmpty() -> CenteredMessage(R.string.admin_subjects_empty, Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding)) {
                items(ui.subjects, key = { it.id }) { subject ->
                    ListItem(
                        headlineContent = { Text(subject.name) },
                        supportingContent = { Text(subject.code) },
                        modifier = Modifier.clickable { vm.onEdit(subject) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    ui.form?.let { form ->
        AlertDialog(
            onDismissRequest = vm::onDismissForm,
            title = {
                Text(stringResource(if (form.id == null) R.string.admin_subject_new else R.string.admin_subject_edit))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = form.code,
                        onValueChange = vm::onCodeChange,
                        label = { Text(stringResource(R.string.field_subject_code)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = form.name,
                        onValueChange = vm::onNameChange,
                        label = { Text(stringResource(R.string.field_subject_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (form.id != null) {
                        TextButton(onClick = vm::onDelete, enabled = !form.saving) {
                            Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = vm::onSave, enabled = !form.saving) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = vm::onDismissForm) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}
