package com.jyhs.educheck.feature_admin.students

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role as SemanticsRole
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.CenteredProgress
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.rememberErrorHost

@Composable
fun StudentEditScreen(
    onBack: () -> Unit,
    vm: StudentEditViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown)
    LaunchedEffect(ui.saved) { if (ui.saved) onBack() }

    SubScreenScaffold(
        title = if (ui.isNew) R.string.admin_student_new else R.string.admin_student_edit,
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
                    value = ui.studentNumber,
                    onValueChange = vm::onNumberChange,
                    label = { Text(stringResource(R.string.field_student_number)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ui.firstNames,
                    onValueChange = vm::onFirstNamesChange,
                    label = { Text(stringResource(R.string.field_first_names)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = ui.lastNames,
                    onValueChange = vm::onLastNamesChange,
                    label = { Text(stringResource(R.string.field_last_names)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text(stringResource(R.string.field_guardians), style = MaterialTheme.typography.titleSmall)
                if (ui.guardianOptions.isEmpty()) {
                    Text(
                        stringResource(R.string.guardians_none_available),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                ui.guardianOptions.forEach { guardian ->
                    val checked = guardian.uid in ui.selectedGuardianUids
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .toggleable(
                                value = checked,
                                role = SemanticsRole.Checkbox,
                                onValueChange = { vm.onToggleGuardian(guardian.uid) },
                            )
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Spacer(Modifier.width(8.dp))
                        Text(guardian.name + " (" + guardian.email + ")")
                    }
                }

                Button(
                    onClick = vm::save,
                    enabled = !ui.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (ui.saving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.action_save))
                    }
                }
            }
        }
    }
}
