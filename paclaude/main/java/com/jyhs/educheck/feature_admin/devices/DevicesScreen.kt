package com.jyhs.educheck.feature_admin.devices

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import com.jyhs.educheck.domain.model.DeviceStatus

@Composable
fun DevicesScreen(
    onBack: () -> Unit,
    vm: DevicesViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.errorRes, vm::onErrorShown)

    SubScreenScaffold(title = R.string.admin_devices_title, onBack = onBack, snackbarHost = snackbar) { padding ->
        when {
            ui.loading -> CenteredProgress(Modifier.padding(padding))
            ui.rows.isEmpty() -> CenteredMessage(R.string.admin_devices_empty, Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(ui.rows, key = { it.device.id }) { row ->
                    val device = row.device
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = row.studentName ?: stringResource(device.role.labelRes()),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(device.model.ifBlank { device.id }, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                text = stringResource(statusLabel(device.status)),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                when (device.status) {
                                    DeviceStatus.PENDING -> {
                                        Button(onClick = { vm.onSetStatus(device, DeviceStatus.ACTIVE) }) {
                                            Text(stringResource(R.string.action_approve))
                                        }
                                        OutlinedButton(onClick = { vm.onSetStatus(device, DeviceStatus.REVOKED) }) {
                                            Text(stringResource(R.string.action_reject))
                                        }
                                    }
                                    DeviceStatus.ACTIVE -> OutlinedButton(
                                        onClick = { vm.onSetStatus(device, DeviceStatus.REVOKED) },
                                    ) { Text(stringResource(R.string.action_revoke)) }
                                    DeviceStatus.REVOKED -> OutlinedButton(
                                        onClick = { vm.onSetStatus(device, DeviceStatus.ACTIVE) },
                                    ) { Text(stringResource(R.string.action_approve)) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun statusLabel(status: DeviceStatus): Int = when (status) {
    DeviceStatus.PENDING -> R.string.device_status_pending
    DeviceStatus.ACTIVE -> R.string.device_status_active
    DeviceStatus.REVOKED -> R.string.device_status_revoked
}
