package com.jyhs.educheck.feature_admin.groups

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
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
import com.jyhs.educheck.core.ui.CenteredMessage
import com.jyhs.educheck.core.ui.CenteredProgress
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.labelRes
import com.jyhs.educheck.core.ui.rememberErrorHost

@Composable
fun GroupDetailScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onStudents: () -> Unit,
    onAddSubject: () -> Unit,
    onOpenAssignment: (String) -> Unit,
    vm: GroupDetailViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown)
    val group = ui.group

    SubScreenScaffold(title = R.string.admin_group_detail, onBack = onBack, snackbarHost = snackbar) { padding ->
        when {
            ui.loading -> CenteredProgress(Modifier.padding(padding))
            group == null -> CenteredMessage(R.string.error_not_found, Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding)) {
                item {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group.name, style = MaterialTheme.typography.headlineSmall)
                        Text(
                            stringResource(
                                R.string.group_summary,
                                group.semester,
                                stringResource(group.shift.labelRes()),
                                group.period,
                                group.members.size,
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = onEdit) { Text(stringResource(R.string.action_edit_group)) }
                            Button(onClick = onStudents) {
                                Text(stringResource(R.string.action_group_students, group.members.size))
                            }
                        }
                        Text(
                            stringResource(R.string.group_subjects_title),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                        OutlinedButton(onClick = onAddSubject, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.action_add_subject_to_group))
                        }
                    }
                }
                if (ui.rows.isEmpty()) {
                    item {
                        Text(
                            stringResource(R.string.group_no_subjects),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp),
                        )
                    }
                }
                items(ui.rows, key = { it.assignment.id }) { row ->
                    ListItem(
                        headlineContent = { Text(row.assignment.subjectName) },
                        supportingContent = { Text(row.teacherName) },
                        modifier = Modifier.clickable { onOpenAssignment(row.assignment.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
