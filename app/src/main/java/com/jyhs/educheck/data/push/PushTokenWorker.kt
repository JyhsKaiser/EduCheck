package com.jyhs.educheck.data.push

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.usecase.RegisterPushTokenUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Registers the FCM token of this phone. Waits for a connection and retries with backoff. */
@HiltWorker
class PushTokenWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val registerToken: RegisterPushTokenUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (val result = registerToken()) {
        is AppResult.Ok -> Result.success()
        is AppResult.Error -> when (result.reason) {
            // Temporary problems: try again later.
            ErrorReason.NO_NETWORK, ErrorReason.UNKNOWN -> if (runAttemptCount < MAX_ATTEMPTS) Result.retry() else Result.failure()
            // Not signed in as a student with a record yet, or no permission: the next app start tries again.
            else -> Result.failure()
        }
    }

    companion object {
        const val UNIQUE_NAME = "push-token"
        private const val MAX_ATTEMPTS = 5
    }
}
