package com.jyhs.educheck.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Local outbox of the campus presence (RF-A04). One record per student and day. */
@Entity(
    tableName = "local_presence",
    indices = [
        Index(value = ["syncStatus"]),
        Index(value = ["studentId", "date"], unique = true),
    ],
)
data class PresenceEntity(
    @PrimaryKey val clientUuid: String,
    val studentId: String,
    val date: String,                  // yyyy-MM-dd in the campus time zone
    val eventEpochMs: Long,            // event time (trusted clock)
    val elapsedRealtimeMs: Long,       // SystemClock.elapsedRealtime() when recorded
    val trustedClock: Boolean,
    val method: String,                // GEOFENCE_WIFI | GEOFENCE | ADMIN_MANUAL
    val confidence: Int,               // 0 = LOW, 1 = HIGH
    val latitude: Double?,
    val longitude: Double?,
    val accuracyM: Float?,
    val bssid: String?,
    val syncStatus: String = "PENDING", // PENDING | SYNCING | SYNCED | REJECTED
    val attempts: Int = 0,
    val lastError: String? = null,
    val createdAtMs: Long,
    val syncedAtMs: Long? = null,
)
