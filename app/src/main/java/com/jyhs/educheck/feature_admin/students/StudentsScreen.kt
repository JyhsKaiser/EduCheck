package com.jyhs.educheck.feature_admin.students

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.CenteredMessage
import com.jyhs.educheck.core.ui.CenteredProgress
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.rememberErrorHost

@Composable
fun StudentsScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onEdit: (String) -> Unit,
    vm: StudentsViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown)

    SubScreenScaffold(
        title = R.string.admin_students_title,
        onBack = onBack,
        snackbarHost = snackbar,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.admin_student_new))
            }
        },
    ) { padding ->
        when {
            ui.loading -> CenteredProgress(Modifier.padding(padding))
            ui.students.isEmpty() -> CenteredMessage(R.string.admin_students_empty, Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding)) {
                items(ui.students, key = { it.id }) { student ->
                    ListItem(
                        headlineContent = { Text(student.fullName) },
                        supportingContent = {
                            val guardians = pluralStringResource(
                                R.plurals.guardians_count, student.guardianUids.size, student.guardianUids.size,
                            )
                            val account = stringResource(
                                if (student.studentUid != null) R.string.student_has_account else R.string.student_no_account,
                            )
                            Text(student.studentNumber + " · " + guardians + " · " + account)
                        },
                        trailingContent = {
                            Switch(checked = student.active, onCheckedChange = { vm.onToggleActive(student) })
                        },
                        modifier = Modifier.clickable { onEdit(student.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
