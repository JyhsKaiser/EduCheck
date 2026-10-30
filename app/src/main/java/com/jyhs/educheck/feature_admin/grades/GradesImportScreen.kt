package com.jyhs.educheck.feature_admin.grades

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.rememberErrorHost
import com.jyhs.educheck.domain.model.GradeImportErrorType

/** Errors listed on screen are capped; the count shown is always the real one. */
private const val MAX_ERRORS_SHOWN = 20

@Composable
fun GradesImportScreen(
    onBack: () -> Unit,
    vm: GradesImportViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.messageRes, vm::onMessageShown, ui.messageArg)
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) vm.onFilePicked(uri.toString())
    }

    SubScreenScaffold(title = R.string.admin_grades_title, onBack = onBack, snackbarHost = snackbar) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.grades_format_title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(R.string.grades_format_help), style = MaterialTheme.typography.bodyMedium)
            Text(
                "studentNumber,subjectCode,groupName,partial1,partial2,partial3\n2026-0001,MAT-301,3A,8.5,9,",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedButton(
                onClick = { picker.launch(arrayOf("text/*", "application/vnd.ms-excel", "application/csv")) },
                enabled = !ui.reading && !ui.importing,
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (ui.reading) {
                    CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.action_pick_csv))
                }
            }

            ui.plan?.let { plan ->
                Text(
                    stringResource(R.string.grades_summary, plan.records.size, plan.errors.size, plan.totalRows),
                    style = MaterialTheme.typography.titleMedium,
                )
                plan.errors.take(MAX_ERRORS_SHOWN).forEach { error ->
                    val reason = stringResource(error.type.labelRes())
                    Text(
                        text = stringResource(R.string.grades_error_line, error.line, reason) +
                            (if (error.detail.isNotEmpty()) " (" + error.detail + ")" else ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                if (plan.errors.size > MAX_ERRORS_SHOWN) {
                    Text(
                        stringResource(R.string.grades_more_errors, plan.errors.size - MAX_ERRORS_SHOWN),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (plan.records.isNotEmpty()) {
                    Text(stringResource(R.string.grades_import_note), style = MaterialTheme.typography.bodySmall)
                    Button(onClick = vm::onImport, enabled = !ui.importing, modifier = Modifier.fillMaxWidth()) {
                        if (ui.importing) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.action_import_grades, plan.records.size))
                        }
                    }
                }
            }
        }
    }
}

@StringRes
private fun GradeImportErrorType.labelRes(): Int = when (this) {
    GradeImportErrorType.HEADER -> R.string.grades_error_header
    GradeImportErrorType.COLUMNS -> R.string.grades_error_columns
    GradeImportErrorType.STUDENT_NOT_FOUND -> R.string.grades_error_student_not_found
    GradeImportErrorType.SUBJECT_NOT_FOUND -> R.string.grades_error_subject_not_found
    GradeImportErrorType.GROUP_NOT_FOUND -> R.string.grades_error_group_not_found
    GradeImportErrorType.GROUP_AMBIGUOUS -> R.string.grades_error_group_ambiguous
    GradeImportErrorType.NO_ASSIGNMENT -> R.string.grades_error_no_assignment
    GradeImportErrorType.STUDENT_NOT_IN_GROUP -> R.string.grades_error_not_in_group
    GradeImportErrorType.INVALID_GRADE -> R.string.grades_error_invalid_grade
    GradeImportErrorType.DUPLICATE_ROW -> R.string.grades_error_duplicate
    GradeImportErrorType.TOO_MANY_ROWS -> R.string.grades_error_too_many
}
