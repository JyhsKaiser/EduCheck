package com.jyhs.educheck.data.firebase

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.NewPresence
import com.jyhs.educheck.domain.model.PresenceMethod
import com.jyhs.educheck.domain.model.PresenceRecord
import com.jyhs.educheck.domain.repository.PresenceRepository
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirestorePresenceRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
) : PresenceRepository {

    private val presences get() = firestore.collection(FirestoreCollections.PRESENCES)

    override suspend fun getPresence(date: String): AppResult<PresenceRecord?> = firestoreCall {
        val uid = auth.currentUser?.uid ?: return@firestoreCall AppResult.Error(ErrorReason.INVALID_CREDENTIALS)
        // A query (not a get by id): the rule lets a student read documents whose studentUid is theirs.
        val result = presences
            .whereEqualTo(FirestoreFields.STUDENT_UID, uid)
            .whereEqualTo(FirestoreFields.DATE, date)
            .limit(1).get().await()
        val doc = result.documents.firstOrNull() ?: return@firestoreCall AppResult.Ok(null)
        val method = doc.getString(FirestoreFields.METHOD)
            ?.let { name -> PresenceMethod.entries.firstOrNull { it.name == name } }
            ?: PresenceMethod.GEOFENCE
        AppResult.Ok(
            PresenceRecord(
                date = date,
                entryTimeMs = doc.getTimestamp(FirestoreFields.ENTRY_TIME)?.toDate()?.time ?: 0L,
                method = method,
                confidence = doc.getLong(FirestoreFields.CONFIDENCE)?.toInt() ?: 0,
            ),
        )
    }

    override suspend fun register(presence: NewPresence): AppResult<Unit> = firestoreCall {
        val uid = auth.currentUser?.uid ?: return@firestoreCall AppResult.Error(ErrorReason.INVALID_CREDENTIALS)
        val data = mutableMapOf<String, Any>(
            FirestoreFields.STUDENT_ID to presence.profile.studentId,
            FirestoreFields.STUDENT_UID to uid,
            FirestoreFields.DATE to presence.date,
            FirestoreFields.STATUS to "PRESENT",
            FirestoreFields.METHOD to presence.method.name,
            FirestoreFields.CONFIDENCE to presence.confidence,
            FirestoreFields.LOCATION to GeoPoint(presence.fix.latitude, presence.fix.longitude),
            FirestoreFields.ENTRY_TIME to Timestamp(Date(presence.entryTimeMs)),
            FirestoreFields.TRUSTED_CLOCK to presence.trustedClock,
            FirestoreFields.GUARDIAN_UIDS to presence.profile.guardianUids,
            FirestoreFields.DEVICE_ID to presence.deviceId,
            FirestoreFields.CLIENT_UUID to presence.clientUuid,
            FirestoreFields.CREATED_AT to FieldValue.serverTimestamp(),
        )
        presence.bssid?.let { data[FirestoreFields.BSSID] = it }
        presences.document(FirestoreIds.presence(presence.profile.studentId, presence.date)).set(data).await()
        AppResult.Ok(Unit)
    }
}
