package com.jyhs.educheck.feature_admin.devices

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.Device
import com.jyhs.educheck.domain.model.DeviceStatus
import com.jyhs.educheck.domain.usecase.ObserveDevicesUseCase
import com.jyhs.educheck.domain.usecase.ObserveStudentsUseCase
import com.jyhs.educheck.domain.usecase.SetDeviceStatusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceRow(val device: Device, val studentName: String?)

data class DevicesUiState(
    val rows: List<DeviceRow> = emptyList(),
    val loading: Boolean = true,
    @StringRes val errorRes: Int? = null,
)

@HiltViewModel
class DevicesViewModel @Inject constructor(
    observeDevices: ObserveDevicesUseCase,
    observeStudents: ObserveStudentsUseCase,
    private val setDeviceStatus: SetDeviceStatusUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(DevicesUiState())
    val ui: StateFlow<DevicesUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            combine(observeDevices(), observeStudents()) { devices, students ->
                val names = students.associate { it.id to it.fullName }
                devices
                    // Pending requests first.
                    .sortedBy { if (it.status == DeviceStatus.PENDING) 0 else 1 }
                    .map { DeviceRow(it, it.studentId?.let(names::get)) }
            }
                .catch { _ui.update { it.copy(loading = false, errorRes = R.string.error_unknown) } }
                .collect { rows -> _ui.update { it.copy(rows = rows, loading = false) } }
        }
    }

    fun onSetStatus(device: Device, status: DeviceStatus) {
        viewModelScope.launch {
            val result = setDeviceStatus(device.id, status)
            if (result is AppResult.Error) _ui.update { it.copy(errorRes = result.reason.messageRes()) }
        }
    }

    fun onErrorShown() = _ui.update { it.copy(errorRes = null) }
}
