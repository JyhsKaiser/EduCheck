package com.jyhs.educheck.data.firebase

import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.data.local.CacheStore
import com.jyhs.educheck.data.local.prefs.DevicePrefs
import com.jyhs.educheck.domain.model.DeviceStatus
import com.jyhs.educheck.domain.model.MyDevice
import com.jyhs.educheck.domain.model.StudentProfile
import com.jyhs.educheck.domain.repository.MyDeviceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * devices/{installId}_{uid}: one record per student on each phone, so two students that share a phone
 * need their own approval. A student only reads their devices through a query on `userUid`, which is what
 * the rule allows (reading a document that does not exist yet would be denied).
 */
@Singleton
class FirestoreMyDeviceRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefs: DevicePrefs,
    private val cache: CacheStore,
) : MyDeviceRepository {

    private val devices get() = firestore.collection(FirestoreCollections.DEVICES)

    /** Online it asks the server (and refreshes the local cache); offline it returns the last known status. */
    override suspend fun ensureDevice(profile: StudentProfile): AppResult<MyDevice> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.INVALID_CREDENTIALS)
        return when (val result = cache.cached("device:" + uid, MyDevice.serializer()) { fetchDevice(profile, uid) }) {
            is AppResult.Ok -> result.value?.let { AppResult.Ok(it) } ?: AppResult.Error(ErrorReason.UNKNOWN)
            is AppResult.Error -> result
        }
    }

    private suspend fun fetchDevice(profile: StudentProfile, uid: String): AppResult<MyDevice> = firestoreCall {
        val deviceId = prefs.installId() + "_" + uid

        val mine = devices.whereEqualTo(FirestoreFields.USER_UID, uid).limit(MAX_DEVICES).get().await()
        val existing = mine.documents.firstOrNull { it.id == deviceId }
        if (existing != null) {
            val status = existing.getString(FirestoreFields.STATUS)
                ?.let { name -> DeviceStatus.entries.firstOrNull { it.name == name } }
                ?: DeviceStatus.PENDING
            return@firestoreCall AppResult.Ok(MyDevice(deviceId, status))
        }

        // First time on this phone: ask for approval.
        devices.document(deviceId).set(
            mapOf(
                FirestoreFields.USER_UID to uid,
                FirestoreFields.STUDENT_ID to profile.studentId,
                FirestoreFields.ROLE to "STUDENT",
                FirestoreFields.STATUS to DeviceStatus.PENDING.name,
                FirestoreFields.MODEL to Build.MODEL,
                FirestoreFields.CREATED_AT to FieldValue.serverTimestamp(),
            ),
        ).await()
        AppResult.Ok(MyDevice(deviceId, DeviceStatus.PENDING))
    }

    private companion object {
        const val MAX_DEVICES = 20L
    }
}
