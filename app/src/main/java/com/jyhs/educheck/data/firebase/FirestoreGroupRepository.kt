package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.GroupInput
import com.jyhs.educheck.domain.model.GroupMember
import com.jyhs.educheck.domain.model.MAX_GROUP_SIZE
import com.jyhs.educheck.domain.model.SchoolGroup
import com.jyhs.educheck.domain.model.Shift
import com.jyhs.educheck.domain.repository.GroupRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreGroupRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : GroupRepository {

    private val groups get() = firestore.collection(FirestoreCollections.GROUPS)
    private val students get() = firestore.collection(FirestoreCollections.STUDENTS)
    private val assignments get() = firestore.collection(FirestoreCollections.ASSIGNMENTS)
    private val schedules get() = firestore.collection(FirestoreCollections.SCHEDULES)

    override fun observeGroups(): Flow<List<SchoolGroup>> =
        groups.orderBy(FirestoreFields.NAME).limit(MAX_GROUPS).asFlow()
            .map { snapshot -> snapshot.documents.mapNotNull { it.toGroup() } }

    override suspend fun saveGroup(id: String?, input: GroupInput): AppResult<String> = firestoreCall {
        val data = mapOf(
            FirestoreFields.NAME to input.name,
            FirestoreFields.SEMESTER to input.semester,
            FirestoreFields.SHIFT to input.shift.name,
            FirestoreFields.PERIOD to input.period,
        )
        if (id == null) {
            val ref = groups.document()
            ref.set(
                data + mapOf(
                    FirestoreFields.ACTIVE to true,
                    FirestoreFields.TEACHER_UIDS to emptyList<String>(),
                    FirestoreFields.GROUP_STUDENTS to emptyList<Map<String, Any>>(),
                ),
            ).await()
            AppResult.Ok(ref.id)
        } else {
            val ref = groups.document(id)
            val oldName = ref.get().await().getString(FirestoreFields.NAME)
            val batch = firestore.batch()
            batch.update(ref, data)
            if (oldName != input.name) {
                // Denormalized copies of the group name.
                assignments.whereEqualTo(FirestoreFields.GROUP_ID, id).get().await().documents
                    .forEach { batch.update(it.reference, FirestoreFields.GROUP_NAME, input.name) }
                schedules.whereEqualTo(FirestoreFields.GROUP_ID, id).get().await().documents
                    .forEach { batch.update(it.reference, FirestoreFields.GROUP_NAME, input.name) }
            }
            batch.commit().await()
            AppResult.Ok(id)
        }
    }

    override suspend fun deleteGroup(id: String): AppResult<Unit> = firestoreCall {
        val used = assignments.whereEqualTo(FirestoreFields.GROUP_ID, id).limit(1).get().await()
        if (!used.isEmpty) return@firestoreCall AppResult.Error(ErrorReason.HAS_DEPENDENTS)

        val ref = groups.document(id)
        val current = ref.get().await()
        val batch = firestore.batch()
        current.groupMembers().forEach { member ->
            batch.update(students.document(member.id), FirestoreFields.GROUP_ID, FieldValue.delete())
        }
        batch.delete(ref)
        batch.commit().await()
        AppResult.Ok(Unit)
    }

    override suspend fun setGroupStudents(groupId: String, members: List<GroupMember>): AppResult<Unit> =
        firestoreCall {
            if (members.size > MAX_GROUP_SIZE) return@firestoreCall AppResult.Error(ErrorReason.LIMIT_EXCEEDED)

            val groupRef = groups.document(groupId)
            val current = groupRef.get().await()
            if (!current.exists()) return@firestoreCall AppResult.Error(ErrorReason.NOT_FOUND)

            val previousIds = current.groupMembers().map { it.id }.toSet()
            val selectedIds = members.map { it.id }.toSet()
            val batch = firestore.batch()

            // A student belongs to one group only: take the selected ones out of any other group.
            groups.get().await().documents.filter { it.id != groupId }.forEach { other ->
                val list = other.groupMembers()
                val kept = list.filter { it.id !in selectedIds }
                if (kept.size != list.size) {
                    batch.update(other.reference, FirestoreFields.GROUP_STUDENTS, kept.map { it.toMap() })
                }
            }

            batch.update(groupRef, FirestoreFields.GROUP_STUDENTS, members.sortedBy { it.name }.map { it.toMap() })
            members.forEach { batch.update(students.document(it.id), FirestoreFields.GROUP_ID, groupId) }
            (previousIds - selectedIds).forEach {
                batch.update(students.document(it), FirestoreFields.GROUP_ID, FieldValue.delete())
            }
            batch.commit().await()
            AppResult.Ok(Unit)
        }

    private fun DocumentSnapshot.toGroup(): SchoolGroup? {
        val name = getString(FirestoreFields.NAME) ?: return null
        val shift = getString(FirestoreFields.SHIFT)?.let { s -> Shift.entries.firstOrNull { it.name == s } }
            ?: Shift.MORNING
        return SchoolGroup(
            id = id,
            name = name,
            semester = getLong(FirestoreFields.SEMESTER)?.toInt() ?: 1,
            shift = shift,
            period = getString(FirestoreFields.PERIOD).orEmpty(),
            teacherUids = (get(FirestoreFields.TEACHER_UIDS) as? List<*>)?.filterIsInstance<String>().orEmpty(),
            members = groupMembers(),
        )
    }

    private companion object {
        const val MAX_GROUPS = 100L
    }
}
