package com.jyhs.educheck.domain.model

import kotlinx.serialization.Serializable

/** The signed-in student's own record (students/{id}), reached through users/{uid}.studentId. */
@Serializable
data class StudentProfile(
    val studentId: String,
    val name: String,
    val studentNumber: String,
    val guardianUids: List<String>,
    val active: Boolean,
)

/** This phone for this student (devices/{installId}_{uid}). */
@Serializable
data class MyDevice(val id: String, val status: DeviceStatus)

enum class PresenceMethod { GEOFENCE, GEOFENCE_WIFI }

data class PresenceRecord(
    val date: String,
    val entryTimeMs: Long,
    val method: PresenceMethod,
    val confidence: Int,
)

/** Everything the Firestore rules ask for when a student registers an entry. */
data class NewPresence(
    val profile: StudentProfile,
    val deviceId: String,
    val clientUuid: String,
    val date: String,
    val entryTimeMs: Long,
    val method: PresenceMethod,
    val confidence: Int,
    val fix: GeoFix,
    val bssid: String?,
    val trustedClock: Boolean,
)

/** [isNew] is false when the entry of the day had already been registered. */
data class PresenceResult(val record: PresenceRecord, val isNew: Boolean)

/** PENDING: saved on the phone, not sent yet. SYNCED: the server has it. REJECTED: it could not be sent. */
enum class SyncState { PENDING, SYNCED, REJECTED }

/** An entry as kept in the local database (Room), the source of truth of the screen. */
data class LocalPresence(
    val clientUuid: String,
    val studentId: String,
    val record: PresenceRecord,
    val fix: GeoFix?,
    val bssid: String?,
    val trustedClock: Boolean,
    val state: SyncState,
    val attempts: Int,
    /** Name of the ErrorReason that rejected it or made the last attempt fail. */
    val lastError: String?,
)

/** What the "Hoy" screen needs besides the entry itself (which comes from the local database). */
data class StudentToday(
    val profile: StudentProfile,
    /** Null when the approval status cannot be known (no connection and never fetched). */
    val device: MyDevice?,
    val campusConfigured: Boolean,
    /** Day of the entry (yyyy-MM-dd, campus time zone). */
    val date: String,
)
