package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.domain.model.CampusConfig
import com.jyhs.educheck.domain.repository.CampusRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreCampusRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : CampusRepository {

    private val campusDoc
        get() = firestore.collection(FirestoreCollections.CONFIG).document(FirestoreDocs.CAMPUS)

    override suspend fun getCampus(): AppResult<CampusConfig?> = firestoreCall {
        val doc = campusDoc.get().await()
        val center = doc.getGeoPoint(FirestoreFields.CENTER)
        if (!doc.exists() || center == null) {
            AppResult.Ok(null)
        } else {
            AppResult.Ok(
                CampusConfig(
                    name = doc.getString(FirestoreFields.NAME).orEmpty(),
                    latitude = center.latitude,
                    longitude = center.longitude,
                    radiusMeters = doc.getLong(FirestoreFields.RADIUS_METERS)?.toInt() ?: 150,
                    toleranceMeters = doc.getLong(FirestoreFields.TOLERANCE_METERS)?.toInt() ?: 100,
                    toleranceMinutes = doc.getLong(FirestoreFields.TOLERANCE_MINUTES)?.toInt() ?: 60,
                    timeZone = doc.getString(FirestoreFields.TIME_ZONE) ?: "America/Mexico_City",
                    bssids = (doc.get(FirestoreFields.BSSIDS) as? List<*>)?.filterIsInstance<String>().orEmpty(),
                ),
            )
        }
    }

    override suspend fun saveCampus(config: CampusConfig): AppResult<Unit> = firestoreCall {
        campusDoc.set(
            mapOf(
                FirestoreFields.NAME to config.name,
                FirestoreFields.CENTER to GeoPoint(config.latitude, config.longitude),
                FirestoreFields.RADIUS_METERS to config.radiusMeters,
                FirestoreFields.TOLERANCE_METERS to config.toleranceMeters,
                FirestoreFields.TOLERANCE_MINUTES to config.toleranceMinutes,
                FirestoreFields.TIME_ZONE to config.timeZone,
                FirestoreFields.BSSIDS to config.bssids,
            ),
        ).await()
        AppResult.Ok(Unit)
    }
}
