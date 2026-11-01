package com.jyhs.educheck.domain.repository

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.LocalPresence
import com.jyhs.educheck.domain.model.MyDevice
import com.jyhs.educheck.domain.model.NewPresence
import com.jyhs.educheck.domain.model.PresenceMethod
import com.jyhs.educheck.domain.model.PresenceRecord
import com.jyhs.educheck.domain.model.StudentProfile
import kotlinx.coroutines.flow.Flow

/**
 * Profile, device and campus are cached locally: online they come from Firestore (and refresh the cache);
 * offline, or when the network fails, the last cached value is returned.
 */
interface StudentSelfRepository {
    /** Profile of the signed-in student. Fails with NO_PROFILE if the account is not linked to a student record. */
    suspend fun getProfile(): AppResult<StudentProfile>
}

interface MyDeviceRepository {
    /** Finds this phone's record, or registers it as PENDING (the administrator approves it). */
    suspend fun ensureDevice(profile: StudentProfile): AppResult<MyDevice>
}

/** The server side of presence. The app reads and writes entries through [PresenceOutbox] and syncs them here. */
interface PresenceRepository {
    /** The signed-in student's presence for [date] (yyyy-MM-dd), or null. */
    suspend fun getPresence(date: String): AppResult<PresenceRecord?>

    suspend fun register(presence: NewPresence): AppResult<Unit>
}

/** Local queue of entries (Room). */
interface PresenceOutbox {
    suspend fun find(studentId: String, date: String): LocalPresence?

    fun observe(studentId: String, date: String): Flow<LocalPresence?>

    /** Stores the entry (replacing a REJECTED one of the same day). False if the day already has one. */
    suspend fun enqueue(entry: LocalPresence): Boolean

    suspend fun pending(studentId: String): List<LocalPresence>

    suspend fun markSynced(clientUuid: String)

    suspend fun markRejected(clientUuid: String, reason: ErrorReason)

    /** An attempt failed for a temporary reason; [attempts] is the new count. */
    suspend fun markAttempt(clientUuid: String, reason: ErrorReason, attempts: Int)

    /** The server already has the day's entry: keep it locally as SYNCED (the server wins). */
    suspend fun importRemote(studentId: String, record: PresenceRecord)
}

interface ClockProvider {
    fun nowMs(): Long

    /** True when the phone sets its date and time automatically (so the clock can be trusted). */
    fun isAutomatic(): Boolean
}

interface ConnectivityProvider {
    /** True when the phone has a validated Internet connection. */
    fun isOnline(): Boolean
}

/** Asks for the queue to be sent as soon as there is a connection (retrying with backoff). */
interface SyncScheduler {
    fun requestSync()
}

/** Local notifications for the student (RF-A05). */
interface EntryNotifier {
    fun notifyEntry(entryTimeMs: Long, method: PresenceMethod)

    fun notifyEntryRejected(entryTimeMs: Long, reason: ErrorReason)
}

/** Background detection of the campus geofence. */
interface PresenceTracker {
    /** Registers the geofence (re-registers if it already exists). Needs the location permission. */
    fun start()

    fun stop()
}
