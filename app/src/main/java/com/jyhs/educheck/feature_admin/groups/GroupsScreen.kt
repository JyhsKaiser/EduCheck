package com.jyhs.educheck.feature_admin.groups

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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.CenteredMessage
import com.jyhs.educheck.core.ui.CenteredProgress
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.labelRes
import com.jyhs.educheck.core.ui.rememberErrorHost

@Composable
fun GroupsScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    onOpen: (String) -> Unit,
    vm: GroupsViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown)

    SubScreenScaffold(
        title = R.string.admin_groups_title,
        onBack = onBack,
        snackbarHost = snackbar,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.admin_group_new))
            }
        },
    ) { padding ->
        when {
            ui.loading -> CenteredProgress(Modifier.padding(padding))
            ui.groups.isEmpty() -> CenteredMessage(R.string.admin_groups_empty, Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding)) {
                items(ui.groups, key = { it.id }) { group ->
                    ListItem(
                        headlineContent = { Text(group.name) },
                        supportingContent = {
                            Text(
                                stringResource(
                                    R.string.group_summary,
                                    group.semester,
                                    stringResource(group.shift.labelRes()),
                                    group.period,
                                    group.members.size,
                                ),
                            )
                        },
                        modifier = Modifier.clickable { onOpen(group.id) },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
