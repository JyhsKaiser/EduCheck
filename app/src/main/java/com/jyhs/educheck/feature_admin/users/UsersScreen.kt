package com.jyhs.educheck.feature_admin.users

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
fun UsersScreen(
    onBack: () -> Unit,
    onCreate: () -> Unit,
    vm: UsersViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown)

    SubScreenScaffold(
        title = R.string.admin_users_title,
        onBack = onBack,
        snackbarHost = snackbar,
        floatingActionButton = {
            FloatingActionButton(onClick = onCreate) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.admin_user_new))
            }
        },
    ) { padding ->
        when {
            ui.loading -> CenteredProgress(Modifier.padding(padding))
            ui.users.isEmpty() -> CenteredMessage(R.string.admin_users_empty, Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding)) {
                items(ui.users, key = { it.uid }) { user ->
                    ListItem(
                        headlineContent = { Text(user.name) },
                        supportingContent = {
                            Text(user.email + " · " + stringResource(user.role.labelRes()))
                        },
                        trailingContent = {
                            Switch(checked = user.active, onCheckedChange = { vm.onToggleActive(user) })
                        },
                    )
                    HorizontalDivider()
                }
            }
        }
    }
}
