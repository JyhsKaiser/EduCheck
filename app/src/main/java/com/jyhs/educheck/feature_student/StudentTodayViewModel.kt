package com.jyhs.educheck.feature_student

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.core.ui.messageRes
import com.jyhs.educheck.domain.model.DeviceStatus
import com.jyhs.educheck.domain.model.LocalPresence
import com.jyhs.educheck.domain.model.StudentProfile
import com.jyhs.educheck.domain.usecase.LoadStudentTodayUseCase
import com.jyhs.educheck.domain.usecase.ObserveTodayPresenceUseCase
import com.jyhs.educheck.domain.usecase.RegisterPresenceNowUseCase
import com.jyhs.educheck.domain.usecase.StartPresenceTrackingUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudentTodayUiState(
    val loading: Boolean = true,
    val profile: StudentProfile? = null,
    /** Null when it cannot be known (no connection and never fetched). */
    val deviceStatus: DeviceStatus? = null,
    val campusConfigured: Boolean = true,
    /** Today's entry as kept in the local database (the source of truth of this screen). */
    val entry: LocalPresence? = null,
    val registering: Boolean = false,
    /** Distance to the campus center when the last attempt was outside the campus. */
    val outsideMeters: String? = null,
    @StringRes val messageRes: Int? = null,
    val messageArg: String? = null,
)

@HiltViewModel
class StudentTodayViewModel @Inject constructor(
    private val loadToday: LoadStudentTodayUseCase,
    private val observeToday: ObserveTodayPresenceUseCase,
    private val registerNow: RegisterPresenceNowUseCase,
    private val startTracking: StartPresenceTrackingUseCase,
) : ViewModel() {

    private val _ui = MutableStateFlow(StudentTodayUiState())
    val ui: StateFlow<StudentTodayUiState> = _ui.asStateFlow()

    private var locationGranted = false
    private var observeJob: Job? = null
    private var observedKey: String? = null

    /**
     * Called by the screen every time it comes to the foreground, with the current location permission.
     * It refreshes the data (which also asks for the queued entries to be sent) and, with the permission,
     * registers the geofence and tries today's entry if it is still missing (the fallback for phones that
     * do not allow background location).
     */
    fun onResume(locationGranted: Boolean) {
        val becameGranted = locationGranted && !this.locationGranted
        this.locationGranted = locationGranted
        if (becameGranted) startTracking()
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            when (val result = loadToday()) {
                is AppResult.Ok -> {
                    val today = result.value
                    _ui.update {
                        it.copy(
                            loading = false,
                            profile = today.profile,
                            deviceStatus = today.device?.status,
                            campusConfigured = today.campusConfigured,
                        )
                    }
                    observe(today.profile.studentId, today.date)
                    autoRegisterIfPossible()
                }
                is AppResult.Error -> _ui.update {
                    it.copy(loading = false, messageRes = result.reason.messageRes(), messageArg = result.detail)
                }
            }
        }
    }

    fun onRegisterNow() = register(silent = false)

    fun onMessageShown() = _ui.update { it.copy(messageRes = null, messageArg = null) }

    /** The screen shows what the local database holds; a change there (sync finished, entry saved) updates it. */
    private fun observe(studentId: String, date: String) {
        val key = "$studentId|$date"
        if (key == observedKey) return
        observedKey = key
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            observeToday(studentId, date).collect { entry -> _ui.update { it.copy(entry = entry) } }
        }
    }

    private fun autoRegisterIfPossible() {
        val state = _ui.value
        val blocked = state.deviceStatus == DeviceStatus.PENDING || state.deviceStatus == DeviceStatus.REVOKED
        if (locationGranted && !blocked && state.campusConfigured && state.entry == null) {
            register(silent = true)
        }
    }

    /** [silent]: the automatic attempt does not bother the student when they are simply not at school. */
    private fun register(silent: Boolean) {
        if (_ui.value.registering) return
        _ui.update { it.copy(registering = true, outsideMeters = null) }
        viewModelScope.launch {
            when (val result = registerNow()) {
                // The entry shows up through the local database.
                is AppResult.Ok -> _ui.update { it.copy(registering = false) }
                is AppResult.Error -> {
                    val reason = result.reason
                    val quiet = silent && reason in QUIET_REASONS
                    _ui.update {
                        it.copy(
                            registering = false,
                            outsideMeters = if (reason == ErrorReason.OUTSIDE_CAMPUS) result.detail else null,
                            messageRes = if (quiet) null else reason.messageRes(),
                            messageArg = if (quiet) null else result.detail,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        val QUIET_REASONS = setOf(
            ErrorReason.OUTSIDE_CAMPUS,
            ErrorReason.PERMISSION_DENIED,
            ErrorReason.LOCATION_UNAVAILABLE,
            ErrorReason.NO_NETWORK,
        )
    }
}
