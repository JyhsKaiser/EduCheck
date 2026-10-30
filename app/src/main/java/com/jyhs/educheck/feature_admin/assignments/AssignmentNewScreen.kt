package com.jyhs.educheck.feature_admin.assignments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.CenteredProgress
import com.jyhs.educheck.core.ui.RadioRow
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.rememberErrorHost

@Composable
fun AssignmentNewScreen(
    onBack: () -> Unit,
    vm: AssignmentNewViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown)
    LaunchedEffect(ui.created) { if (ui.created) onBack() }

    SubScreenScaffold(title = R.string.admin_assignment_new, onBack = onBack, snackbarHost = snackbar) { padding ->
        if (ui.loading) {
            CenteredProgress(Modifier.padding(padding))
        } else {
            Column(
                modifier = Modifier
                    .padding(padding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ui.group?.let { Text(it.name, style = MaterialTheme.typography.headlineSmall) }

                Text(stringResource(R.string.field_subject), style = MaterialTheme.typography.titleSmall)
                if (ui.subjects.isEmpty()) {
                    Text(stringResource(R.string.assignment_no_subjects), style = MaterialTheme.typography.bodyMedium)
                }
                ui.subjects.forEach { subject ->
                    RadioRow(
                        selected = ui.subjectId == subject.id,
                        label = subject.name + " (" + subject.code + ")",
                        onClick = { vm.onSubjectChange(subject.id) },
                    )
                }

                Text(
                    stringResource(R.string.field_teacher),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
                if (ui.teachers.isEmpty()) {
                    Text(stringResource(R.string.assignment_no_teachers), style = MaterialTheme.typography.bodyMedium)
                }
                ui.teachers.forEach { teacher ->
                    RadioRow(
                        selected = ui.teacherUid == teacher.uid,
                        label = teacher.name,
                        onClick = { vm.onTeacherChange(teacher.uid) },
                    )
                }

                Button(
                    onClick = vm::create,
                    enabled = !ui.saving && ui.subjectId != null && ui.teacherUid != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                ) {
                    if (ui.saving) {
                        CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.action_create_assignment))
                    }
                }
            }
        }
    }
}
