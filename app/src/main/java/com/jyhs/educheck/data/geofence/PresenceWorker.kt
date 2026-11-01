package com.jyhs.educheck.data.geofence

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.GeoFix
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.repository.LocationProvider
import com.jyhs.educheck.domain.repository.WifiProvider
import com.jyhs.educheck.domain.usecase.ObserveSessionUseCase
import com.jyhs.educheck.domain.usecase.RegisterPresenceUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Registers the entry after a geofence ENTER (RF-A01). The same flow as the "Register now" button. */
@HiltWorker
class PresenceWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val registerPresence: RegisterPresenceUseCase,
    private val location: LocationProvider,
    private val wifi: WifiProvider,
    private val observeSession: ObserveSessionUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        // Only a signed-in student registers presence.
        if (observeSession().first()?.role != Role.STUDENT) return Result.success()

        val fix = if (inputData.getBoolean(KEY_HAS_FIX, false)) {
            GeoFix(
                latitude = inputData.getDouble(KEY_LAT, 0.0),
                longitude = inputData.getDouble(KEY_LON, 0.0),
                accuracyMeters = inputData.getFloat(KEY_ACCURACY, 0f),
            )
        } else {
            // The system did not include the position: ask for it now.
            when (val r = location.currentLocation()) {
                is AppResult.Ok -> r.value
                is AppResult.Error -> return if (r.reason == ErrorReason.PERMISSION_DENIED) Result.failure() else Result.retry()
            }
        }

        val eventMs = inputData.getLong(KEY_EVENT_MS, System.currentTimeMillis())
        return when (val result = registerPresence(fix, wifi.currentBssid(), eventMs)) {
            is AppResult.Ok -> Result.success()
            is AppResult.Error -> when (result.reason) {
                // No connection: try again later with the same data.
                ErrorReason.NO_NETWORK -> if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
                else -> {
                    Log.i(TAG, "Entry not registered: " + result.reason)
                    Result.failure()
                }
            }
        }
    }

    companion object {
        const val UNIQUE_NAME = "presence-entry"
        const val KEY_HAS_FIX = "hasFix"
        const val KEY_LAT = "lat"
        const val KEY_LON = "lon"
        const val KEY_ACCURACY = "accuracy"
        const val KEY_EVENT_MS = "eventMs"
        private const val MAX_ATTEMPTS = 5
        private const val TAG = "PresenceWorker"
    }
}
