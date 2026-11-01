package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.DeviceStatus
import com.jyhs.educheck.domain.model.GeoFix
import com.jyhs.educheck.domain.model.LocalPresence
import com.jyhs.educheck.domain.model.NewPresence
import com.jyhs.educheck.domain.model.PresenceMethod
import com.jyhs.educheck.domain.model.PresenceRecord
import com.jyhs.educheck.domain.model.PresenceResult
import com.jyhs.educheck.domain.model.StudentToday
import com.jyhs.educheck.domain.model.SyncState
import com.jyhs.educheck.domain.repository.CampusRepository
import com.jyhs.educheck.domain.repository.ClockProvider
import com.jyhs.educheck.domain.repository.ConnectivityProvider
import com.jyhs.educheck.domain.repository.EntryNotifier
import com.jyhs.educheck.domain.repository.LocationProvider
import com.jyhs.educheck.domain.repository.MyDeviceRepository
import com.jyhs.educheck.domain.repository.PresenceOutbox
import com.jyhs.educheck.domain.repository.PresenceRepository
import com.jyhs.educheck.domain.repository.PresenceTracker
import com.jyhs.educheck.domain.repository.StudentSelfRepository
import com.jyhs.educheck.domain.repository.SyncScheduler
import com.jyhs.educheck.domain.repository.WifiProvider
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.flow.Flow

object GeoMath {
    private const val EARTH_RADIUS_METERS = 6_371_008.8

    /** Great-circle (haversine) distance in meters, the same measure the Firestore rules use. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
    }
}

/** yyyy-MM-dd of the instant in the campus time zone (the document id of the day's presence). */
fun campusDate(epochMs: Long, timeZone: String): String =
    LocalDate.ofInstant(Instant.ofEpochMilli(epochMs), ZoneId.of(timeZone)).toString()

/**
 * Data policy of the entry (RA-09):
 * - The local database is the source of truth of the screen. An entry is saved there first.
 * - Profile, device status and campus come from the server when online and from the local cache otherwise.
 * - The server wins: if it already has the day's entry, the local copy is just marked as synced.
 * - An entry is never changed after it is created; a rejected one can be registered again.
 */
class LoadStudentTodayUseCase @Inject constructor(
    private val student: StudentSelfRepository,
    private val device: MyDeviceRepository,
    private val campus: CampusRepository,
    private val presence: PresenceRepository,
    private val outbox: PresenceOutbox,
    private val connectivity: ConnectivityProvider,
    private val clock: ClockProvider,
    private val scheduler: SyncScheduler,
) {
    suspend operator fun invoke(): AppResult<StudentToday> {
        val profile = when (val r = student.getProfile()) {
            is AppResult.Ok -> r.value
            is AppResult.Error -> return r
        }
        val config = when (val r = campus.getCampus()) {
            is AppResult.Ok -> r.value
            is AppResult.Error -> return r
        }
        // Offline and never fetched: the approval status is simply unknown.
        val myDevice = when (val r = device.ensureDevice(profile)) {
            is AppResult.Ok -> r.value
            is AppResult.Error -> if (r.reason == ErrorReason.NO_NETWORK) null else return r
        }
        val date = campusDate(clock.nowMs(), config?.timeZone ?: ZoneId.systemDefault().id)

        // Incoming change: an entry made from another phone (or already synced) is kept locally.
        if (connectivity.isOnline()) {
            val remote = presence.getPresence(date)
            if (remote is AppResult.Ok && remote.value != null) outbox.importRemote(profile.studentId, remote.value)
        }
        scheduler.requestSync()
        return AppResult.Ok(StudentToday(profile, myDevice, config != null, date))
    }
}

class ObserveTodayPresenceUseCase @Inject constructor(private val outbox: PresenceOutbox) {
    operator fun invoke(studentId: String, date: String): Flow<LocalPresence?> = outbox.observe(studentId, date)
}

/**
 * Registers the entry of the day (RF-A01, RF-A02, RF-A04, RF-A05) from a location fix. It works without connection:
 * the entry is saved in the local queue with the real time of the event and sent later by [SyncPresenceUseCase].
 *
 * - Inside radius + tolerance and on the school's Wi-Fi: GEOFENCE_WIFI, confidence 1.
 * - Inside but not on the school's Wi-Fi: GEOFENCE, confidence 0 (counts as present all the same).
 * - Outside: OUTSIDE_CAMPUS, with the distance in `detail`.
 * - Online, the phone must be approved. Offline the approval cannot be checked: the entry is queued and the
 *   check is made when it is sent (if the phone turns out not to be approved, the entry is discarded with a notice).
 * - One entry per student per day: if it already exists nothing is written and `isNew` is false.
 */
class RegisterPresenceUseCase @Inject constructor(
    private val student: StudentSelfRepository,
    private val device: MyDeviceRepository,
    private val campus: CampusRepository,
    private val presence: PresenceRepository,
    private val outbox: PresenceOutbox,
    private val connectivity: ConnectivityProvider,
    private val clock: ClockProvider,
    private val notifier: EntryNotifier,
    private val scheduler: SyncScheduler,
) {
    suspend operator fun invoke(fix: GeoFix, bssid: String?, eventTimeMs: Long): AppResult<PresenceResult> {
        val profile = when (val r = student.getProfile()) {
            is AppResult.Ok -> r.value
            is AppResult.Error -> return r
        }
        if (!profile.active) return AppResult.Error(ErrorReason.ACCOUNT_INACTIVE)

        val config = when (val r = campus.getCampus()) {
            is AppResult.Ok -> r.value ?: return AppResult.Error(ErrorReason.CAMPUS_NOT_CONFIGURED)
            is AppResult.Error -> return r
        }
        val online = connectivity.isOnline()
        if (online) {
            val myDevice = when (val r = device.ensureDevice(profile)) {
                is AppResult.Ok -> r.value
                is AppResult.Error -> if (r.reason == ErrorReason.NO_NETWORK) null else return r
            }
            if (myDevice != null && myDevice.status != DeviceStatus.ACTIVE) {
                return AppResult.Error(ErrorReason.DEVICE_NOT_APPROVED)
            }
        }

        // The event time can come from a queue, but never from the future nor from more than a day ago.
        val now = clock.nowMs()
        val entryMs = eventTimeMs.coerceIn(now - MAX_AGE_MS, now)
        val date = campusDate(entryMs, config.timeZone)

        val local = outbox.find(profile.studentId, date)
        if (local != null && local.state != SyncState.REJECTED) return AppResult.Ok(PresenceResult(local.record, isNew = false))
        if (online) {
            val remote = presence.getPresence(date)
            if (remote is AppResult.Ok && remote.value != null) {
                outbox.importRemote(profile.studentId, remote.value)
                return AppResult.Ok(PresenceResult(remote.value, isNew = false))
            }
        }

        val distance = GeoMath.distanceMeters(fix.latitude, fix.longitude, config.latitude, config.longitude)
        if (distance > config.radiusMeters + config.toleranceMeters) {
            return AppResult.Error(ErrorReason.OUTSIDE_CAMPUS, distance.toInt().toString())
        }

        val onSchoolWifi = bssid != null && config.bssids.any { it.equals(bssid, ignoreCase = true) }
        val method = if (onSchoolWifi) PresenceMethod.GEOFENCE_WIFI else PresenceMethod.GEOFENCE
        val record = PresenceRecord(date, entryMs, method, if (onSchoolWifi) 1 else 0)

        val saved = outbox.enqueue(
            LocalPresence(
                clientUuid = UUID.randomUUID().toString(),
                studentId = profile.studentId,
                record = record,
                fix = fix,
                bssid = if (onSchoolWifi) bssid?.uppercase() else null,
                trustedClock = clock.isAutomatic(),
                state = SyncState.PENDING,
                attempts = 0,
                lastError = null,
            ),
        )
        if (!saved) {
            val existing = outbox.find(profile.studentId, date)?.record ?: record
            return AppResult.Ok(PresenceResult(existing, isNew = false))
        }
        notifier.notifyEntry(entryMs, method)
        scheduler.requestSync()
        return AppResult.Ok(PresenceResult(record, isNew = true))
    }

    private companion object {
        /** The rules reject an entry time older than 24 h; stay safely inside. */
        const val MAX_AGE_MS = 23L * 60 * 60 * 1000
    }
}

enum class SyncOutcome { DONE, RETRY }

/**
 * Sends the queued entries of the signed-in student (RF-A04). Everything is checked again with fresh data:
 *
 * - Not approved phone -> the entry is discarded (REJECTED) and the student is told.
 * - The server already has the day's entry -> the local copy becomes SYNCED (the server wins).
 * - Older than 23.5 h -> rejected (the rules would refuse it).
 * - Outside the campus with the current configuration -> rejected.
 * - The school Wi-Fi is no longer in the list -> sent as GEOFENCE (confidence 0) instead of being rejected.
 * - No connection or a temporary error -> stays PENDING and WorkManager tries again with backoff (8 attempts).
 */
class SyncPresenceUseCase @Inject constructor(
    private val student: StudentSelfRepository,
    private val device: MyDeviceRepository,
    private val campus: CampusRepository,
    private val presence: PresenceRepository,
    private val outbox: PresenceOutbox,
    private val connectivity: ConnectivityProvider,
    private val clock: ClockProvider,
    private val notifier: EntryNotifier,
) {
    suspend operator fun invoke(): SyncOutcome {
        if (!connectivity.isOnline()) return SyncOutcome.RETRY

        val profile = when (val r = student.getProfile()) {
            is AppResult.Ok -> r.value
            // Not signed in (or not a student): nothing to send for now.
            is AppResult.Error -> return if (r.reason == ErrorReason.NO_NETWORK) SyncOutcome.RETRY else SyncOutcome.DONE
        }
        val queue = outbox.pending(profile.studentId)
        if (queue.isEmpty()) return SyncOutcome.DONE

        val myDevice = when (val r = device.ensureDevice(profile)) {
            is AppResult.Ok -> r.value
            is AppResult.Error -> return SyncOutcome.RETRY
        }
        if (myDevice.status != DeviceStatus.ACTIVE) {
            queue.forEach { reject(it, ErrorReason.DEVICE_NOT_APPROVED) }
            return SyncOutcome.DONE
        }
        val config = when (val r = campus.getCampus()) {
            is AppResult.Ok -> r.value ?: return SyncOutcome.DONE // not configured: they wait
            is AppResult.Error -> return if (r.reason == ErrorReason.NO_NETWORK) SyncOutcome.RETRY else SyncOutcome.DONE
        }

        var retry = false
        for (entry in queue) {
            val now = clock.nowMs()
            if (now - entry.record.entryTimeMs > MAX_SYNC_AGE_MS) {
                reject(entry, ErrorReason.ENTRY_EXPIRED)
                continue
            }
            when (val remote = presence.getPresence(entry.record.date)) {
                is AppResult.Error -> {
                    retry = true
                    continue
                }
                is AppResult.Ok -> if (remote.value != null) {
                    outbox.markSynced(entry.clientUuid) // the server wins
                    continue
                }
            }
            val fix = entry.fix
            if (fix == null) {
                reject(entry, ErrorReason.INVALID_INPUT)
                continue
            }
            val distance = GeoMath.distanceMeters(fix.latitude, fix.longitude, config.latitude, config.longitude)
            if (distance > config.radiusMeters + config.toleranceMeters) {
                reject(entry, ErrorReason.OUTSIDE_CAMPUS)
                continue
            }
            val onSchoolWifi = entry.bssid != null && config.bssids.any { it.equals(entry.bssid, ignoreCase = true) }

            val write = NewPresence(
                profile = profile,
                deviceId = myDevice.id,
                clientUuid = entry.clientUuid,
                date = entry.record.date,
                entryTimeMs = entry.record.entryTimeMs,
                method = if (onSchoolWifi) PresenceMethod.GEOFENCE_WIFI else PresenceMethod.GEOFENCE,
                confidence = if (onSchoolWifi) 1 else 0,
                fix = fix,
                bssid = if (onSchoolWifi) entry.bssid?.uppercase() else null,
                trustedClock = entry.trustedClock,
            )
            when (val r = presence.register(write)) {
                is AppResult.Ok -> outbox.markSynced(entry.clientUuid)
                is AppResult.Error -> when (r.reason) {
                    ErrorReason.FORBIDDEN -> reject(entry, ErrorReason.FORBIDDEN)
                    else -> {
                        val attempts = entry.attempts + 1
                        if (attempts >= MAX_ATTEMPTS && r.reason != ErrorReason.NO_NETWORK) {
                            reject(entry, ErrorReason.UNKNOWN)
                        } else {
                            outbox.markAttempt(entry.clientUuid, r.reason, attempts)
                            retry = true
                        }
                    }
                }
            }
        }
        return if (retry) SyncOutcome.RETRY else SyncOutcome.DONE
    }

    private suspend fun reject(entry: LocalPresence, reason: ErrorReason) {
        outbox.markRejected(entry.clientUuid, reason)
        notifier.notifyEntryRejected(entry.record.entryTimeMs, reason)
    }

    private companion object {
        const val MAX_ATTEMPTS = 8
        const val MAX_SYNC_AGE_MS = 23L * 60 * 60 * 1000 + 30L * 60 * 1000
    }
}

/** "Register my entry now": current GPS fix + connected Wi-Fi, then the same flow as the geofence. */
class RegisterPresenceNowUseCase @Inject constructor(
    private val location: LocationProvider,
    private val wifi: WifiProvider,
    private val register: RegisterPresenceUseCase,
    private val clock: ClockProvider,
) {
    suspend operator fun invoke(): AppResult<PresenceResult> {
        val fix = when (val r = location.currentLocation()) {
            is AppResult.Ok -> r.value
            is AppResult.Error -> return r
        }
        return register(fix, wifi.currentBssid(), clock.nowMs())
    }
}

class StartPresenceTrackingUseCase @Inject constructor(private val tracker: PresenceTracker) {
    operator fun invoke() = tracker.start()
}
