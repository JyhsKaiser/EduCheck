package com.jyhs.educheck.feature_admin.users

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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.RadioRow
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.labelRes
import com.jyhs.educheck.core.ui.rememberErrorHost
import com.jyhs.educheck.domain.model.Role

@Composable
fun CreateUserScreen(
    onBack: () -> Unit,
    vm: CreateUserViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown)
    LaunchedEffect(ui.created) { if (ui.created) onBack() }

    SubScreenScaffold(title = R.string.admin_user_new, onBack = onBack, snackbarHost = snackbar) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // A student account takes its name from the student record, so the field is only for other roles.
            if (ui.role != Role.STUDENT) {
                OutlinedTextField(
                    value = ui.name,
                    onValueChange = vm::onNameChange,
                    label = { Text(stringResource(R.string.field_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            OutlinedTextField(
                value = ui.email,
                onValueChange = vm::onEmailChange,
                label = { Text(stringResource(R.string.login_email)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth(),
            )
            // Visible on purpose: the administrator has to hand this temporary password to the user.
            OutlinedTextField(
                value = ui.password,
                onValueChange = vm::onPasswordChange,
                label = { Text(stringResource(R.string.field_temp_password)) },
                supportingText = { Text(stringResource(R.string.field_temp_password_hint)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text(stringResource(R.string.field_role), style = MaterialTheme.typography.titleSmall)
            Role.entries.forEach { role ->
                RadioRow(
                    selected = ui.role == role,
                    label = stringResource(role.labelRes()),
                    onClick = { vm.onRoleChange(role) },
                )
            }

            if (ui.role == Role.STUDENT) {
                Text(stringResource(R.string.field_student_record), style = MaterialTheme.typography.titleSmall)
                RadioRow(
                    selected = ui.studentMode == StudentMode.NEW,
                    label = stringResource(R.string.option_new_student),
                    onClick = { vm.onStudentModeChange(StudentMode.NEW) },
                )
                RadioRow(
                    selected = ui.studentMode == StudentMode.EXISTING,
                    label = stringResource(R.string.option_existing_student),
                    onClick = { vm.onStudentModeChange(StudentMode.EXISTING) },
                )

                if (ui.studentMode == StudentMode.NEW) {
                    OutlinedTextField(
                        value = ui.studentNumber,
                        onValueChange = vm::onStudentNumberChange,
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
                } else {
                    if (ui.availableStudents.isEmpty()) {
                        Text(
                            stringResource(R.string.students_none_without_account),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    ui.availableStudents.forEach { student ->
                        RadioRow(
                            selected = ui.studentId == student.id,
                            label = student.fullName + " (" + student.studentNumber + ")",
                            onClick = { vm.onStudentChange(student.id) },
                        )
                    }
                }
            }

            Button(
                onClick = vm::submit,
                enabled = !ui.loading,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (ui.loading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_create_user))
                }
            }
        }
    }
}
