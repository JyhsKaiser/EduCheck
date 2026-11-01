package com.jyhs.educheck.data.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent

/** Receives the geofence transition and hands the work to WorkManager (a receiver must finish quickly). */
class GeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) {
            Log.w(TAG, "Geofence error: " + event.errorCode)
            return
        }
        if (event.geofenceTransition != Geofence.GEOFENCE_TRANSITION_ENTER) return

        val location = event.triggeringLocation
        val data = workDataOf(
            PresenceWorker.KEY_HAS_FIX to (location != null),
            PresenceWorker.KEY_LAT to (location?.latitude ?: 0.0),
            PresenceWorker.KEY_LON to (location?.longitude ?: 0.0),
            PresenceWorker.KEY_ACCURACY to (location?.accuracy ?: 0f),
            PresenceWorker.KEY_EVENT_MS to System.currentTimeMillis(),
        )
        val request = OneTimeWorkRequestBuilder<PresenceWorker>()
            .setInputData(data)
            .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
            .build()
        // KEEP: several transitions in a row only need one attempt.
        WorkManager.getInstance(context).enqueueUniqueWork(PresenceWorker.UNIQUE_NAME, ExistingWorkPolicy.KEEP, request)
    }

    private companion object {
        const val TAG = "GeofenceReceiver"
    }
}
