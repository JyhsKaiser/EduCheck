package com.jyhs.educheck.feature_admin.campus

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.R
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.CampusConfig
import com.jyhs.educheck.domain.usecase.GetCampusUseCase
import com.jyhs.educheck.domain.usecase.GetCurrentBssidUseCase
import com.jyhs.educheck.domain.usecase.GetCurrentLocationUseCase
import com.jyhs.educheck.domain.usecase.SaveCampusUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Default map position while the campus has not been configured: Toluca, Mexico. */
const val DEFAULT_LATITUDE = 19.2826
const val DEFAULT_LONGITUDE = -99.6557

data class CampusUiState(
    val name: String = "",
    /** Chosen center; null until the administrator taps the map or uses the current location. */
    val latitude: Double? = null,
    val longitude: Double? = null,
    val radiusMeters: String = "150",
    val toleranceMeters: String = "100",
    val toleranceMinutes: String = "60",
    val timeZone: String = "America/Mexico_City",
    val bssids: List<String> = emptyList(),
    val newBssid: String = "",
    val loading: Boolean = true,
    val locating: Boolean = false,
    val saving: Boolean = false,
    /** Snackbar message (an error or a confirmation). */
    @StringRes val messageRes: Int? = null,
)

@HiltViewModel
class CampusViewModel @Inject constructor(
    private val getCampus: GetCampusUseCase,
    private val saveCampus: SaveCampusUseCase,
    private val getCurrentLocation: GetCurrentLocationUseCase,
    private val getCurrentBssid: GetCurrentBssidUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(CampusUiState())
    val ui: StateFlow<CampusUiState> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            when (val result = getCampus()) {
                is AppResult.Ok -> {
                    val config = result.value
                    _ui.update {
                        if (config == null) {
                            it.copy(loading = false)
                        } else {
                            it.copy(
                                name = config.name,
                                latitude = config.latitude,
                                longitude = config.longitude,
                                radiusMeters = config.radiusMeters.toString(),
                                toleranceMeters = config.toleranceMeters.toString(),
                                toleranceMinutes = config.toleranceMinutes.toString(),
                                timeZone = config.timeZone,
                                bssids = config.bssids,
                                loading = false,
                            )
                        }
                    }
                }
                is AppResult.Error -> _ui.update { it.copy(loading = false, messageRes = result.reason.messageRes()) }
            }
        }
    }

    fun onNameChange(value: String) = _ui.update { it.copy(name = value) }
    fun onRadiusChange(value: String) = _ui.update { it.copy(radiusMeters = value.filter(Char::isDigit).take(4)) }
    fun onToleranceMetersChange(value: String) = _ui.update { it.copy(toleranceMeters = value.filter(Char::isDigit).take(4)) }
    fun onToleranceMinutesChange(value: String) = _ui.update { it.copy(toleranceMinutes = value.filter(Char::isDigit).take(3)) }
    fun onTimeZoneChange(value: String) = _ui.update { it.copy(timeZone = value) }
    fun onNewBssidChange(value: String) = _ui.update { it.copy(newBssid = value.uppercase()) }
    fun onMessageShown() = _ui.update { it.copy(messageRes = null) }

    fun onMapClick(latitude: Double, longitude: Double) =
        _ui.update { it.copy(latitude = latitude, longitude = longitude) }

    fun onPermissionDenied() = _ui.update { it.copy(messageRes = ErrorReason.PERMISSION_DENIED.messageRes()) }

    fun onUseMyLocation() {
        if (_ui.value.locating) return
        _ui.update { it.copy(locating = true) }
        viewModelScope.launch {
            when (val result = getCurrentLocation()) {
                is AppResult.Ok -> _ui.update {
                    it.copy(locating = false, latitude = result.value.latitude, longitude = result.value.longitude)
                }
                is AppResult.Error -> _ui.update { it.copy(locating = false, messageRes = result.reason.messageRes()) }
            }
        }
    }

    fun onDetectBssid() {
        when (val result = getCurrentBssid()) {
            is AppResult.Ok -> addBssid(result.value)
            is AppResult.Error -> _ui.update { it.copy(messageRes = result.reason.messageRes()) }
        }
    }

    fun onAddTypedBssid() = addBssid(_ui.value.newBssid.trim())

    private fun addBssid(bssid: String) {
        if (!SaveCampusUseCase.BSSID.matches(bssid)) {
            _ui.update { it.copy(messageRes = R.string.error_invalid_bssid) }
            return
        }
        _ui.update { state ->
            state.copy(bssids = if (bssid in state.bssids) state.bssids else state.bssids + bssid, newBssid = "")
        }
    }

    fun onRemoveBssid(bssid: String) = _ui.update { it.copy(bssids = it.bssids - bssid) }

    fun save() {
        val current = _ui.value
        if (current.saving || current.loading) return
        val latitude = current.latitude
        val longitude = current.longitude
        if (latitude == null || longitude == null) {
            _ui.update { it.copy(messageRes = R.string.error_campus_location_required) }
            return
        }
        _ui.update { it.copy(saving = true) }
        viewModelScope.launch {
            val config = CampusConfig(
                name = current.name,
                latitude = latitude,
                longitude = longitude,
                radiusMeters = current.radiusMeters.toIntOrNull() ?: 0,
                toleranceMeters = current.toleranceMeters.toIntOrNull() ?: -1,
                toleranceMinutes = current.toleranceMinutes.toIntOrNull() ?: -1,
                timeZone = current.timeZone,
                bssids = current.bssids,
            )
            when (val result = saveCampus(config)) {
                is AppResult.Ok -> _ui.update { it.copy(saving = false, messageRes = R.string.info_campus_saved) }
                is AppResult.Error -> _ui.update { it.copy(saving = false, messageRes = result.reason.messageRes()) }
            }
        }
    }
}
