package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.Assignment
import com.jyhs.educheck.domain.model.NewAssignment
import com.jyhs.educheck.domain.repository.AssignmentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreAssignmentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : AssignmentRepository {

    private val assignments get() = firestore.collection(FirestoreCollections.ASSIGNMENTS)
    private val groups get() = firestore.collection(FirestoreCollections.GROUPS)
    private val schedules get() = firestore.collection(FirestoreCollections.SCHEDULES)

    override fun observeAssignments(): Flow<List<Assignment>> =
        assignments.limit(MAX_ASSIGNMENTS).asFlow()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toAssignment() } }

    override suspend fun createAssignment(new: NewAssignment): AppResult<String> = firestoreCall {
        val duplicate = assignments
            .whereEqualTo(FirestoreFields.GROUP_ID, new.group.id)
            .whereEqualTo(FirestoreFields.SUBJECT_ID, new.subject.id)
            .limit(1).get().await()
        if (!duplicate.isEmpty) return@firestoreCall AppResult.Error(ErrorReason.DUPLICATE)

        val ref = assignments.document()
        val batch = firestore.batch()
        batch.set(
            ref,
            mapOf(
                FirestoreFields.GROUP_ID to new.group.id,
                FirestoreFields.GROUP_NAME to new.group.name,
                FirestoreFields.SUBJECT_ID to new.subject.id,
                FirestoreFields.SUBJECT_NAME to new.subject.name,
                FirestoreFields.TEACHER_UID to new.teacher.uid,
            ),
        )
        // The teacher reads the group (roll call) through this list.
        batch.update(groups.document(new.group.id), FirestoreFields.TEACHER_UIDS, FieldValue.arrayUnion(new.teacher.uid))
        batch.commit().await()
        AppResult.Ok(ref.id)
    }

    override suspend fun deleteAssignment(assignment: Assignment): AppResult<Unit> = firestoreCall {
        val hasSchedules = schedules.whereEqualTo(FirestoreFields.ASSIGNMENT_ID, assignment.id).limit(1).get().await()
        if (!hasSchedules.isEmpty) return@firestoreCall AppResult.Error(ErrorReason.HAS_DEPENDENTS)

        val sameTeacher = assignments
            .whereEqualTo(FirestoreFields.GROUP_ID, assignment.groupId)
            .whereEqualTo(FirestoreFields.TEACHER_UID, assignment.teacherUid)
            .get().await()
        val teacherHasOthers = sameTeacher.documents.any { it.id != assignment.id }

        val batch = firestore.batch()
        batch.delete(assignments.document(assignment.id))
        if (!teacherHasOthers) {
            batch.update(
                groups.document(assignment.groupId),
                FirestoreFields.TEACHER_UIDS,
                FieldValue.arrayRemove(assignment.teacherUid),
            )
        }
        batch.commit().await()
        AppResult.Ok(Unit)
    }

    private fun DocumentSnapshot.toAssignment(): Assignment? {
        val groupId = getString(FirestoreFields.GROUP_ID) ?: return null
        val subjectId = getString(FirestoreFields.SUBJECT_ID) ?: return null
        return Assignment(
            id = id,
            groupId = groupId,
            groupName = getString(FirestoreFields.GROUP_NAME).orEmpty(),
            subjectId = subjectId,
            subjectName = getString(FirestoreFields.SUBJECT_NAME).orEmpty(),
            teacherUid = getString(FirestoreFields.TEACHER_UID).orEmpty(),
        )
    }

    private companion object {
        const val MAX_ASSIGNMENTS = 500L
    }
}
