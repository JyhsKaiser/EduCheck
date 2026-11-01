package com.jyhs.educheck.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.jyhs.educheck.domain.usecase.SyncOutcome
import com.jyhs.educheck.domain.usecase.SyncPresenceUseCase
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Sends the queued entries when there is a connection (RF-A04). WorkManager retries with backoff. */
@HiltWorker
class PresenceSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val syncPresence: SyncPresenceUseCase,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = when (syncPresence()) {
        SyncOutcome.DONE -> Result.success()
        SyncOutcome.RETRY -> Result.retry()
    }

    companion object {
        const val UNIQUE_NAME = "presence-sync"
    }
}
