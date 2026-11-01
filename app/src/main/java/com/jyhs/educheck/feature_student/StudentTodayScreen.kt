package com.jyhs.educheck.feature_student

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.CenteredProgress
import com.jyhs.educheck.core.ui.rememberErrorHost
import com.jyhs.educheck.domain.model.DeviceStatus
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.core.ui.rejectionReasonRes
import com.jyhs.educheck.domain.model.PresenceMethod
import com.jyhs.educheck.domain.model.SyncState
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Scaffold
import androidx.compose.foundation.layout.WindowInsets

private data class PermissionState(val location: Boolean, val background: Boolean, val notifications: Boolean)

private fun readPermissions(context: Context): PermissionState {
    fun granted(permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    return PermissionState(
        location = granted(Manifest.permission.ACCESS_FINE_LOCATION),
        background = granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
        // Below Android 13 notifications need no runtime permission.
        notifications = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || granted(Manifest.permission.POST_NOTIFICATIONS),
    )
}

@Composable
fun StudentTodayScreen(vm: StudentTodayViewModel = hiltViewModel()) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.messageRes, vm::onMessageShown, ui.messageArg)
    val context = LocalContext.current

    var permissions by remember { mutableStateOf(readPermissions(context)) }
    fun refreshPermissions() {
        permissions = readPermissions(context)
    }
    // Every time the app comes to the foreground (also when coming back from the system settings, where
    // "Allow all the time" is granted): read the permissions and refresh the data.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        refreshPermissions()
        vm.onResume(permissions.location)
    }

    val locationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refreshPermissions()
        vm.onResume(permissions.location || granted)
    }
    val backgroundLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissions() }
    val notificationsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refreshPermissions() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        contentWindowInsets = WindowInsets(0.dp, 0.dp, 0.dp, 0.dp),
    ) { padding ->
        if (ui.loading) {
            CenteredProgress(Modifier.padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(R.string.tab_today), style = MaterialTheme.typography.headlineSmall)
            ui.profile?.let {
                Text(it.name + " · " + it.studentNumber, style = MaterialTheme.typography.bodyMedium)
            }

            // ---- Entry of the day
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.student_entry_title), style = MaterialTheme.typography.titleMedium)
                    val entry = ui.entry
                    val canRegister = !ui.registering && permissions.location && ui.campusConfigured &&
                        ui.deviceStatus != DeviceStatus.PENDING && ui.deviceStatus != DeviceStatus.REVOKED
                    if (entry != null && entry.state != SyncState.REJECTED) {
                        val time = DateTimeFormatter.ofPattern("HH:mm")
                            .withZone(ZoneId.systemDefault())
                            .format(Instant.ofEpochMilli(entry.record.entryTimeMs))
                        val method = stringResource(
                            if (entry.record.method == PresenceMethod.GEOFENCE_WIFI) R.string.entry_method_wifi else R.string.entry_method_gps,
                        )
                        Text(stringResource(R.string.student_entry_registered, time), style = MaterialTheme.typography.bodyLarge)
                        Text(method, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (entry.state == SyncState.PENDING) {
                            Text(
                                stringResource(R.string.student_entry_pending_sync),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                            OutlinedButton(onClick = vm::refresh) { Text(stringResource(R.string.action_send_now)) }
                        } else {
                            Text(
                                stringResource(R.string.student_entry_synced),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    } else {
                        if (entry != null) {
                            // REJECTED: it was discarded; it can be registered again.
                            val time = DateTimeFormatter.ofPattern("HH:mm")
                                .withZone(ZoneId.systemDefault())
                                .format(Instant.ofEpochMilli(entry.record.entryTimeMs))
                            val reason = entry.lastError
                                ?.let { name -> ErrorReason.entries.firstOrNull { it.name == name } }
                                ?.rejectionReasonRes() ?: R.string.reason_unknown
                            Text(
                                stringResource(R.string.student_entry_rejected, time, stringResource(reason)),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                            )
                        } else {
                            Text(stringResource(R.string.student_entry_missing), style = MaterialTheme.typography.bodyLarge)
                        }
                        ui.outsideMeters?.let {
                            Text(
                                stringResource(R.string.error_outside_campus, it),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (!ui.campusConfigured) {
                            Text(stringResource(R.string.error_campus_not_configured), style = MaterialTheme.typography.bodySmall)
                        }
                        Button(
                            onClick = vm::onRegisterNow,
                            enabled = canRegister,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            if (ui.registering) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            } else {
                                Text(stringResource(R.string.action_register_entry))
                            }
                        }
                    }
                }
            }

            // ---- Device approval
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.student_device_title), style = MaterialTheme.typography.titleMedium)
                    val (label, help) = when (ui.deviceStatus) {
                        DeviceStatus.ACTIVE -> R.string.device_status_active to R.string.student_device_active_help
                        DeviceStatus.REVOKED -> R.string.device_status_revoked to R.string.student_device_revoked_help
                        DeviceStatus.PENDING -> R.string.device_status_pending to R.string.student_device_pending_help
                        null -> R.string.device_status_unknown to R.string.student_device_unknown_help
                    }
                    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(help), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = vm::refresh) { Text(stringResource(R.string.action_refresh)) }
                }
            }

            // ---- Permissions
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.student_permissions_title), style = MaterialTheme.typography.titleMedium)
                    PermissionRow(
                        label = R.string.permission_location,
                        granted = permissions.location,
                        onGrant = { locationLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION) },
                    )
                    PermissionRow(
                        label = R.string.permission_background,
                        granted = permissions.background,
                        enabled = permissions.location,
                        onGrant = { backgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) },
                    )
                    if (!permissions.background) {
                        Text(
                            stringResource(R.string.permission_background_help),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    PermissionRow(
                        label = R.string.permission_notifications,
                        granted = permissions.notifications,
                        onGrant = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notificationsLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(label: Int, granted: Boolean, onGrant: () -> Unit, enabled: Boolean = true) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(label), style = MaterialTheme.typography.bodyMedium)
            Text(
                stringResource(if (granted) R.string.permission_granted else R.string.permission_missing),
                style = MaterialTheme.typography.bodySmall,
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
        }
        if (!granted) {
            OutlinedButton(onClick = onGrant, enabled = enabled) { Text(stringResource(R.string.action_grant)) }
        }
    }
}
