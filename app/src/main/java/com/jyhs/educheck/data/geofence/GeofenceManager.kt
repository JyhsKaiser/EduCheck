package com.jyhs.educheck.data.geofence

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.CampusConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max
import kotlinx.coroutines.tasks.await

/** Registers the campus circle with Play Services; the system wakes [GeofenceReceiver] when the phone enters it. */
@Singleton
class GeofenceManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val client by lazy { LocationServices.getGeofencingClient(context) }

    // FLAG_MUTABLE: Play Services adds the transition data to the intent.
    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, GeofenceReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
    )

    @SuppressLint("MissingPermission")
    suspend fun register(campus: CampusConfig): AppResult<Unit> {
        return try {
            val geofence = Geofence.Builder()
                .setRequestId(REQUEST_ID)
                // Very small circles are unreliable; the exact radius is checked again before writing.
                .setCircularRegion(campus.latitude, campus.longitude, max(campus.radiusMeters.toFloat(), MIN_RADIUS_METERS))
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
                .build()
            val request = GeofencingRequest.Builder()
                // If the phone is already inside when it is registered, report the entry right away.
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(geofence)
                .build()
            client.addGeofences(request, pendingIntent()).await()
            AppResult.Ok(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            AppResult.Error(ErrorReason.PERMISSION_DENIED)
        } catch (e: Exception) {
            AppResult.Error(ErrorReason.LOCATION_UNAVAILABLE)
        }
    }

    fun remove() {
        client.removeGeofences(pendingIntent())
    }

    private companion object {
        const val REQUEST_ID = "campus"
        const val MIN_RADIUS_METERS = 100f
    }
}
