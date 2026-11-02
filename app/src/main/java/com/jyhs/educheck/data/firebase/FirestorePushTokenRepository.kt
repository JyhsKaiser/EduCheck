package com.jyhs.educheck.data.firebase

import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.data.local.prefs.DevicePrefs
import com.jyhs.educheck.domain.model.DeviceStatus
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.repository.PushTokenRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

/**
 * The token lives in devices/{installId}_{uid}.fcmToken, the same record the student flow uses. The rules let the
 * owner change only that field. As with the student device, the record is found with a query on `userUid`
 * (reading a document that does not exist yet would be denied) and created if it is missing.
 */
@Singleton
class FirestorePushTokenRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefs: DevicePrefs,
) : PushTokenRepository {

    private val devices get() = firestore.collection(FirestoreCollections.DEVICES)

    override suspend fun save(role: Role, token: String): AppResult<Unit> = firestoreCall {
        val uid = auth.currentUser?.uid ?: return@firestoreCall AppResult.Error(ErrorReason.INVALID_CREDENTIALS)
        val deviceId = prefs.installId() + "_" + uid

        val existing = devices.whereEqualTo(FirestoreFields.USER_UID, uid).limit(MAX_DEVICES).get().await()
            .documents.firstOrNull { it.id == deviceId }
        when {
            existing != null -> {
                if (existing.getString(FIELD_TOKEN) != token) devices.document(deviceId).update(FIELD_TOKEN, token).await()
            }
            // A student phone is registered (PENDING) by the student flow, not here.
            role == Role.STUDENT -> return@firestoreCall AppResult.Error(ErrorReason.NOT_FOUND)
            else -> devices.document(deviceId).set(
                mapOf(
                    FirestoreFields.USER_UID to uid,
                    FirestoreFields.ROLE to role.name,
                    FirestoreFields.STATUS to DeviceStatus.ACTIVE.name,
                    FirestoreFields.MODEL to Build.MODEL,
                    FIELD_TOKEN to token,
                    FirestoreFields.CREATED_AT to FieldValue.serverTimestamp(),
                ),
            ).await()
        }
        AppResult.Ok(Unit)
    }

    override suspend fun clear(): AppResult<Unit> = firestoreCall {
        val uid = auth.currentUser?.uid ?: return@firestoreCall AppResult.Ok(Unit)
        val deviceId = prefs.installId() + "_" + uid
        val existing = devices.whereEqualTo(FirestoreFields.USER_UID, uid).limit(MAX_DEVICES).get().await()
            .documents.firstOrNull { it.id == deviceId }
        // The rules only accept a string here, so the token is blanked rather than deleted.
        if (existing != null && !existing.getString(FIELD_TOKEN).isNullOrEmpty()) {
            devices.document(deviceId).update(FIELD_TOKEN, "").await()
        }
        AppResult.Ok(Unit)
    }

    private companion object {
        const val FIELD_TOKEN = "fcmToken"
        const val MAX_DEVICES = 20L
    }
}
