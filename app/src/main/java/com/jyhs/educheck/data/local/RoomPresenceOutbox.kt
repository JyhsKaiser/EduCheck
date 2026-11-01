package com.jyhs.educheck.data.local

import android.os.SystemClock
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.data.local.dao.PresenceDao
import com.jyhs.educheck.data.local.entity.PresenceEntity
import com.jyhs.educheck.domain.model.GeoFix
import com.jyhs.educheck.domain.model.LocalPresence
import com.jyhs.educheck.domain.model.PresenceMethod
import com.jyhs.educheck.domain.model.PresenceRecord
import com.jyhs.educheck.domain.model.SyncState
import com.jyhs.educheck.domain.repository.PresenceOutbox
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** The local queue of entries, kept in Room (table local_presence). */
@Singleton
class RoomPresenceOutbox @Inject constructor(
    private val dao: PresenceDao,
) : PresenceOutbox {

    override suspend fun find(studentId: String, date: String): LocalPresence? =
        dao.find(studentId, date)?.toDomain()

    override fun observe(studentId: String, date: String): Flow<LocalPresence?> =
        dao.observeForDay(studentId, date).map { it?.toDomain() }

    override suspend fun enqueue(entry: LocalPresence): Boolean {
        // A rejected entry can be registered again: it makes room for the new one.
        dao.deleteRejected(entry.studentId, entry.record.date)
        return dao.insert(entry.toEntity()) != -1L
    }

    override suspend fun pending(studentId: String): List<LocalPresence> =
        dao.pendingFor(studentId).map { it.toDomain() }

    override suspend fun markSynced(clientUuid: String) = dao.markSynced(clientUuid, System.currentTimeMillis())

    override suspend fun markRejected(clientUuid: String, reason: ErrorReason) = dao.markRejected(clientUuid, reason.name)

    override suspend fun markAttempt(clientUuid: String, reason: ErrorReason, attempts: Int) =
        dao.markAttempt(clientUuid, reason.name, attempts)

    override suspend fun importRemote(studentId: String, record: PresenceRecord) {
        val local = dao.find(studentId, record.date)
        if (local == null) {
            val now = System.currentTimeMillis()
            dao.insert(
                PresenceEntity(
                    clientUuid = "remote-" + UUID.randomUUID(),
                    studentId = studentId,
                    date = record.date,
                    eventEpochMs = record.entryTimeMs,
                    elapsedRealtimeMs = SystemClock.elapsedRealtime(),
                    trustedClock = true,
                    method = record.method.name,
                    confidence = record.confidence,
                    latitude = null,
                    longitude = null,
                    accuracyM = null,
                    bssid = null,
                    syncStatus = SyncState.SYNCED.name,
                    attempts = 0,
                    lastError = null,
                    createdAtMs = now,
                    syncedAtMs = now,
                ),
            )
        } else if (local.syncStatus != SyncState.SYNCED.name) {
            dao.markSynced(local.clientUuid, System.currentTimeMillis()) // the server wins
        }
    }

    private fun LocalPresence.toEntity() = PresenceEntity(
        clientUuid = clientUuid,
        studentId = studentId,
        date = record.date,
        eventEpochMs = record.entryTimeMs,
        elapsedRealtimeMs = SystemClock.elapsedRealtime(),
        trustedClock = trustedClock,
        method = record.method.name,
        confidence = record.confidence,
        latitude = fix?.latitude,
        longitude = fix?.longitude,
        accuracyM = fix?.accuracyMeters,
        bssid = bssid,
        syncStatus = state.name,
        attempts = attempts,
        lastError = lastError,
        createdAtMs = System.currentTimeMillis(),
        syncedAtMs = null,
    )

    private fun PresenceEntity.toDomain() = LocalPresence(
        clientUuid = clientUuid,
        studentId = studentId,
        record = PresenceRecord(
            date = date,
            entryTimeMs = eventEpochMs,
            method = PresenceMethod.entries.firstOrNull { it.name == method } ?: PresenceMethod.GEOFENCE,
            confidence = confidence,
        ),
        fix = if (latitude != null && longitude != null) GeoFix(latitude, longitude, accuracyM ?: 0f) else null,
        bssid = bssid,
        trustedClock = trustedClock,
        state = SyncState.entries.firstOrNull { it.name == syncStatus } ?: SyncState.PENDING,
        attempts = attempts,
        lastError = lastError,
    )
}
