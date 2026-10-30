package com.jyhs.educheck.feature_admin

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.jyhs.educheck.R

/** Entry point of the administration tools. Later sprints add subjects, groups, schedules, campus and grades. */
@Composable
fun AdminManagementScreen(
    onOpenUsers: () -> Unit,
    onOpenStudents: () -> Unit,
    onOpenDevices: () -> Unit,
    onOpenSubjects: () -> Unit,
    onOpenGroups: () -> Unit,
    onOpenCampus: () -> Unit,
    onOpenGradesImport: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.tab_management),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(16.dp),
        )
        ManagementItem(R.string.admin_users_title, R.string.admin_users_subtitle, onOpenUsers)
        ManagementItem(R.string.admin_students_title, R.string.admin_students_subtitle, onOpenStudents)
        ManagementItem(R.string.admin_subjects_title, R.string.admin_subjects_subtitle, onOpenSubjects)
        ManagementItem(R.string.admin_groups_title, R.string.admin_groups_subtitle, onOpenGroups)
        ManagementItem(R.string.admin_devices_title, R.string.admin_devices_subtitle, onOpenDevices)
        ManagementItem(R.string.admin_campus_title, R.string.admin_campus_subtitle, onOpenCampus)
        ManagementItem(R.string.admin_grades_title, R.string.admin_grades_subtitle, onOpenGradesImport)
    }
}

@Composable
private fun ManagementItem(title: Int, subtitle: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(subtitle)) },
        modifier = Modifier.clickable(onClick = onClick),
    )
    HorizontalDivider()
}
