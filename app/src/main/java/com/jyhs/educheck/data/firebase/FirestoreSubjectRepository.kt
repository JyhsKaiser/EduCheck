package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.Subject
import com.jyhs.educheck.domain.model.SubjectInput
import com.jyhs.educheck.domain.repository.SubjectRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreSubjectRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : SubjectRepository {

    private val subjects get() = firestore.collection(FirestoreCollections.SUBJECTS)
    private val assignments get() = firestore.collection(FirestoreCollections.ASSIGNMENTS)
    private val schedules get() = firestore.collection(FirestoreCollections.SCHEDULES)

    override fun observeSubjects(): Flow<List<Subject>> =
        subjects.orderBy(FirestoreFields.NAME).limit(MAX_SUBJECTS).asFlow()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toSubject() } }

    override suspend fun saveSubject(id: String?, input: SubjectInput): AppResult<String> = firestoreCall {
        val sameCode = subjects.whereEqualTo(FirestoreFields.CODE, input.code).get().await()
        if (sameCode.documents.any { it.id != id }) return@firestoreCall AppResult.Error(ErrorReason.DUPLICATE)

        val data = mapOf(FirestoreFields.CODE to input.code, FirestoreFields.NAME to input.name)
        if (id == null) {
            val ref = subjects.document()
            ref.set(data).await()
            AppResult.Ok(ref.id)
        } else {
            val ref = subjects.document(id)
            val oldName = ref.get().await().getString(FirestoreFields.NAME)
            val batch = firestore.batch()
            batch.update(ref, data)
            if (oldName != input.name) {
                // Denormalized copies of the subject name.
                assignments.whereEqualTo(FirestoreFields.SUBJECT_ID, id).get().await().documents
                    .forEach { batch.update(it.reference, FirestoreFields.SUBJECT_NAME, input.name) }
                schedules.whereEqualTo(FirestoreFields.SUBJECT_ID, id).get().await().documents
                    .forEach { batch.update(it.reference, FirestoreFields.SUBJECT_NAME, input.name) }
            }
            batch.commit().await()
            AppResult.Ok(id)
        }
    }

    override suspend fun deleteSubject(id: String): AppResult<Unit> = firestoreCall {
        val used = assignments.whereEqualTo(FirestoreFields.SUBJECT_ID, id).limit(1).get().await()
        if (!used.isEmpty) return@firestoreCall AppResult.Error(ErrorReason.HAS_DEPENDENTS)
        subjects.document(id).delete().await()
        AppResult.Ok(Unit)
    }

    private fun DocumentSnapshot.toSubject(): Subject? {
        val code = getString(FirestoreFields.CODE) ?: return null
        return Subject(id = id, code = code, name = getString(FirestoreFields.NAME).orEmpty())
    }

    private companion object {
        const val MAX_SUBJECTS = 300L
    }
}
