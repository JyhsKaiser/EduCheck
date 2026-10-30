package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.domain.model.Device
import com.jyhs.educheck.domain.model.DeviceStatus
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.repository.DeviceRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreDeviceRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : DeviceRepository {

    override fun observeDevices(): Flow<List<Device>> =
        firestore.collection(FirestoreCollections.DEVICES)
            .limit(MAX_DEVICES)
            .asFlow()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toDevice() } }

    override suspend fun setStatus(deviceId: String, status: DeviceStatus): AppResult<Unit> = firestoreCall {
        firestore.collection(FirestoreCollections.DEVICES).document(deviceId)
            .update(FirestoreFields.STATUS, status.name).await()
        AppResult.Ok(Unit)
    }

    private fun DocumentSnapshot.toDevice(): Device? {
        val role = getString(FirestoreFields.ROLE)?.let { name -> Role.entries.firstOrNull { it.name == name } }
            ?: return null
        val status = getString(FirestoreFields.STATUS)?.let { name -> DeviceStatus.entries.firstOrNull { it.name == name } }
            ?: return null
        return Device(
            id = id,
            userUid = getString(FirestoreFields.USER_UID).orEmpty(),
            studentId = getString(FirestoreFields.STUDENT_ID),
            role = role,
            status = status,
            model = getString(FirestoreFields.MODEL).orEmpty(),
        )
    }

    private companion object {
        const val MAX_DEVICES = 200L
    }
}
