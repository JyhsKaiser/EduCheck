package com.jyhs.educheck.data.geofence

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.jyhs.educheck.domain.repository.PresenceTracker
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WorkManagerPresenceTracker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val manager: GeofenceManager,
) : PresenceTracker {

    override fun start() = enqueueSetup(context)

    override fun stop() {
        WorkManager.getInstance(context).cancelUniqueWork(GeofenceSetupWorker.UNIQUE_NAME)
        WorkManager.getInstance(context).cancelUniqueWork(PresenceWorker.UNIQUE_NAME)
        manager.remove()
    }

    companion object {
        /** The campus configuration comes from the local cache when there is no connection. */
        fun enqueueSetup(context: Context) {
            val request = OneTimeWorkRequestBuilder<GeofenceSetupWorker>().build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(GeofenceSetupWorker.UNIQUE_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
