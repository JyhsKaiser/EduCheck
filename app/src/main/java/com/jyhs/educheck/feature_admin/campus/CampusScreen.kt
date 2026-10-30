package com.jyhs.educheck.feature_admin.campus

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.jyhs.educheck.R
import com.jyhs.educheck.core.ui.CenteredProgress
import com.jyhs.educheck.core.ui.SubScreenScaffold
import com.jyhs.educheck.core.ui.rememberErrorHost

/** What to do once the location permission has been granted. */
private enum class PendingAction { LOCATION, WIFI }

@Composable
fun CampusScreen(
    onBack: () -> Unit,
    vm: CampusViewModel = hiltViewModel(),
) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val snackbar = rememberErrorHost(ui.messageRes, vm::onMessageShown)
    val context = LocalContext.current

    // Location permission, requested the first time the administrator needs GPS or the Wi-Fi BSSID.
    var pending by remember { mutableStateOf<PendingAction?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val action = pending
        pending = null
        if (!granted) {
            vm.onPermissionDenied()
        } else if (action == PendingAction.LOCATION) {
            vm.onUseMyLocation()
        } else if (action == PendingAction.WIFI) {
            vm.onDetectBssid()
        }
    }
    fun withLocationPermission(action: PendingAction, run: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            run()
        } else {
            pending = action
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    SubScreenScaffold(title = R.string.admin_campus_title, onBack = onBack, snackbarHost = snackbar) { padding ->
        if (ui.loading) {
            CenteredProgress(Modifier.padding(padding))
        } else {
            // The map is fixed on top and only the form scrolls: a map inside a scrolling column steals the drag.
            Column(Modifier.padding(padding)) {
                CampusMap(
                    latitude = ui.latitude,
                    longitude = ui.longitude,
                    radiusMeters = ui.radiusMeters.toDoubleOrNull() ?: 0.0,
                    onMapClick = vm::onMapClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(stringResource(R.string.campus_map_hint), style = MaterialTheme.typography.bodyMedium)
                    OutlinedButton(
                        onClick = { withLocationPermission(PendingAction.LOCATION, vm::onUseMyLocation) },
                        enabled = !ui.locating,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (ui.locating) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.action_use_my_location))
                        }
                    }
                    if (ui.latitude != null && ui.longitude != null) {
                        Text(
                            stringResource(R.string.campus_coordinates, ui.latitude!!, ui.longitude!!),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    OutlinedTextField(
                        value = ui.name,
                        onValueChange = vm::onNameChange,
                        label = { Text(stringResource(R.string.field_campus_name)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    NumberField(R.string.field_radius_meters, ui.radiusMeters, vm::onRadiusChange)
                    NumberField(R.string.field_tolerance_meters, ui.toleranceMeters, vm::onToleranceMetersChange)
                    NumberField(R.string.field_tolerance_minutes, ui.toleranceMinutes, vm::onToleranceMinutesChange)
                    OutlinedTextField(
                        value = ui.timeZone,
                        onValueChange = vm::onTimeZoneChange,
                        label = { Text(stringResource(R.string.field_time_zone)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    HorizontalDivider()
                    Text(stringResource(R.string.field_bssids), style = MaterialTheme.typography.titleSmall)
                    Text(
                        stringResource(R.string.field_bssids_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ui.bssids.forEach { bssid ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(bssid, style = MaterialTheme.typography.bodyLarge)
                            IconButton(onClick = { vm.onRemoveBssid(bssid) }) {
                                Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                    }
                    OutlinedButton(
                        onClick = { withLocationPermission(PendingAction.WIFI, vm::onDetectBssid) },
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(stringResource(R.string.action_detect_wifi)) }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = ui.newBssid,
                            onValueChange = vm::onNewBssidChange,
                            label = { Text(stringResource(R.string.field_bssid_manual)) },
                            placeholder = { Text("AA:BB:CC:DD:EE:FF") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedButton(onClick = vm::onAddTypedBssid) { Text(stringResource(R.string.action_add)) }
                    }

                    Button(onClick = vm::save, enabled = !ui.saving, modifier = Modifier.fillMaxWidth()) {
                        if (ui.saving) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(stringResource(R.string.action_save))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NumberField(label: Int, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(stringResource(label)) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun CampusMap(
    latitude: Double?,
    longitude: Double?,
    radiusMeters: Double,
    onMapClick: (Double, Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val center = if (latitude != null && longitude != null) LatLng(latitude, longitude) else null
    val cameraState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center ?: LatLng(DEFAULT_LATITUDE, DEFAULT_LONGITUDE), 16f)
    }
    // Follow the chosen point (map tap, "use my location" or the saved campus).
    LaunchedEffect(latitude, longitude) {
        if (center != null) cameraState.animate(CameraUpdateFactory.newLatLngZoom(center, 17f))
    }

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraState,
        onMapClick = { onMapClick(it.latitude, it.longitude) },
    ) {
        if (center != null) {
            val markerState = remember(center) { MarkerState(position = center) }
            Marker(state = markerState)
            if (radiusMeters > 0.0) {
                Circle(
                    center = center,
                    radius = radiusMeters,
                    strokeColor = Color(0xFF1565C0),
                    strokeWidth = 3f,
                    fillColor = Color(0x331565C0),
                )
            }
        }
    }
}
