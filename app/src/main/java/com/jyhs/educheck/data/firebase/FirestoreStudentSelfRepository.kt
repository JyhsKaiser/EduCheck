package com.jyhs.educheck.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.data.local.CacheStore
import com.jyhs.educheck.domain.model.StudentProfile
import com.jyhs.educheck.domain.repository.StudentSelfRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreStudentSelfRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val cache: CacheStore,
) : StudentSelfRepository {

    /** Online it reads Firestore (and refreshes the local cache); offline it returns the last cached profile. */
    override suspend fun getProfile(): AppResult<StudentProfile> {
        val uid = auth.currentUser?.uid ?: return AppResult.Error(ErrorReason.INVALID_CREDENTIALS)
        return when (val result = cache.cached("profile:" + uid, StudentProfile.serializer()) { fetchProfile(uid) }) {
            is AppResult.Ok -> result.value?.let { AppResult.Ok(it) } ?: AppResult.Error(ErrorReason.NO_PROFILE)
            is AppResult.Error -> result
        }
    }

    private suspend fun fetchProfile(uid: String): AppResult<StudentProfile> = firestoreCall {
        val user = firestore.collection(FirestoreCollections.USERS).document(uid).get().await()
        val studentId = user.getString(FirestoreFields.STUDENT_ID)
            ?: return@firestoreCall AppResult.Error(ErrorReason.NO_PROFILE)
        val student = firestore.collection(FirestoreCollections.STUDENTS).document(studentId).get().await()
        if (!student.exists() || student.getString(FirestoreFields.STUDENT_UID) != uid) {
            return@firestoreCall AppResult.Error(ErrorReason.NO_PROFILE)
        }
        AppResult.Ok(
            StudentProfile(
                studentId = studentId,
                name = (student.getString(FirestoreFields.FIRST_NAMES).orEmpty() + " " +
                    student.getString(FirestoreFields.LAST_NAMES).orEmpty()).trim(),
                studentNumber = student.getString(FirestoreFields.STUDENT_NUMBER).orEmpty(),
                guardianUids = (student.get(FirestoreFields.GUARDIAN_UIDS) as? List<*>)
                    ?.filterIsInstance<String>().orEmpty(),
                active = student.getBoolean(FirestoreFields.ACTIVE) ?: true,
            ),
        )
    }
}
