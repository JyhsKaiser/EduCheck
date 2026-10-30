package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.domain.model.Student
import com.jyhs.educheck.domain.model.StudentInput
import com.jyhs.educheck.domain.repository.StudentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreStudentRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : StudentRepository {

    private val students get() = firestore.collection(FirestoreCollections.STUDENTS)

    override fun observeStudents(): Flow<List<Student>> =
        students
            .orderBy(FirestoreFields.LAST_NAMES)
            .limit(MAX_STUDENTS)
            .asFlow()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toStudent() } }

    override suspend fun saveStudent(id: String?, input: StudentInput): AppResult<String> = firestoreCall {
        val data = mapOf(
            FirestoreFields.STUDENT_NUMBER to input.studentNumber,
            FirestoreFields.FIRST_NAMES to input.firstNames,
            FirestoreFields.LAST_NAMES to input.lastNames,
        )
        if (id == null) {
            val ref = students.document()
            ref.set(
                data + mapOf(
                    FirestoreFields.STUDENT_UID to null,
                    FirestoreFields.GUARDIAN_UIDS to emptyList<String>(),
                    FirestoreFields.ACTIVE to true,
                ),
            ).await()
            AppResult.Ok(ref.id)
        } else {
            val studentRef = students.document(id)
            val groupId = studentRef.get().await().getString(FirestoreFields.GROUP_ID)
            val batch = firestore.batch()
            batch.update(studentRef, data)
            if (groupId != null) {
                // The group keeps a copy of the student's name and number: keep it in sync.
                val groupRef = firestore.collection(FirestoreCollections.GROUPS).document(groupId)
                val members = groupRef.get().await().groupMembers().map { member ->
                    if (member.id == id) {
                        member.copy(
                            name = (input.firstNames + " " + input.lastNames).trim(),
                            studentNumber = input.studentNumber,
                        )
                    } else {
                        member
                    }
                }
                batch.update(groupRef, FirestoreFields.GROUP_STUDENTS, members.map { it.toMap() })
            }
            batch.commit().await()
            AppResult.Ok(id)
        }
    }

    override suspend fun setGuardians(studentId: String, guardianUids: List<String>): AppResult<Unit> =
        firestoreCall {
            students.document(studentId).update(FirestoreFields.GUARDIAN_UIDS, guardianUids).await()
            AppResult.Ok(Unit)
        }

    override suspend fun setStudentActive(studentId: String, active: Boolean): AppResult<Unit> =
        firestoreCall {
            students.document(studentId).update(FirestoreFields.ACTIVE, active).await()
            AppResult.Ok(Unit)
        }

    private fun DocumentSnapshot.toStudent(): Student? {
        val number = getString(FirestoreFields.STUDENT_NUMBER) ?: return null
        val guardians = (get(FirestoreFields.GUARDIAN_UIDS) as? List<*>)?.filterIsInstance<String>().orEmpty()
        return Student(
            id = id,
            studentNumber = number,
            firstNames = getString(FirestoreFields.FIRST_NAMES).orEmpty(),
            lastNames = getString(FirestoreFields.LAST_NAMES).orEmpty(),
            groupId = getString(FirestoreFields.GROUP_ID),
            studentUid = getString(FirestoreFields.STUDENT_UID),
            guardianUids = guardians,
            active = getBoolean(FirestoreFields.ACTIVE) ?: true,
        )
    }

    private companion object {
        const val MAX_STUDENTS = 500L
    }
}
