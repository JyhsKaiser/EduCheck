package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.domain.model.GradeRecord
import com.jyhs.educheck.domain.repository.GradeRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreGradeRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : GradeRepository {

    override suspend fun saveGrades(records: List<GradeRecord>): AppResult<Unit> = firestoreCall {
        val grades = firestore.collection(FirestoreCollections.GRADES)
        // A batch holds at most 500 writes.
        records.chunked(BATCH_SIZE).forEach { chunk ->
            val batch = firestore.batch()
            chunk.forEach { record ->
                batch.set(
                    grades.document(FirestoreIds.grade(record.assignmentId, record.studentId)),
                    mapOf(
                        FirestoreFields.ASSIGNMENT_ID to record.assignmentId,
                        FirestoreFields.STUDENT_ID to record.studentId,
                        FirestoreFields.TEACHER_UID to record.teacherUid,
                        FirestoreFields.PARTIALS to record.partials.mapKeys { it.key.toString() },
                        FirestoreFields.IMPORTED_AT to FieldValue.serverTimestamp(),
                    ),
                )
            }
            batch.commit().await()
        }
        AppResult.Ok(Unit)
    }

    private companion object {
        const val BATCH_SIZE = 400
    }
}
