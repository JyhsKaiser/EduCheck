package com.jyhs.educheck.data.geofence

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.usecase.GetCampusUseCase
import com.jyhs.educheck.domain.usecase.ObserveSessionUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/** Reads the campus configuration and (re)registers the geofence. Runs at app start, after a reboot or an update. */
@HiltWorker
class GeofenceSetupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val getCampus: GetCampusUseCase,
    private val manager: GeofenceManager,
    private val observeSession: ObserveSessionUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        if (observeSession().first()?.role != Role.STUDENT) return Result.success()

        val campus = when (val r = getCampus()) {
            is AppResult.Ok -> r.value ?: return Result.success() // not configured yet: nothing to watch
            is AppResult.Error -> return if (r.reason == ErrorReason.NO_NETWORK) Result.retry() else Result.failure()
        }
        return when (val r = manager.register(campus)) {
            is AppResult.Ok -> Result.success()
            is AppResult.Error -> if (r.reason == ErrorReason.PERMISSION_DENIED) Result.failure() else Result.retry()
        }
    }

    companion object {
        const val UNIQUE_NAME = "geofence-setup"
    }
}
