package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.domain.model.Schedule
import com.jyhs.educheck.domain.model.ScheduleDraft
import com.jyhs.educheck.domain.repository.ScheduleRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreScheduleRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
) : ScheduleRepository {

    private val schedules get() = firestore.collection(FirestoreCollections.SCHEDULES)

    override fun observeSchedules(assignmentId: String): Flow<List<Schedule>> =
        schedules.whereEqualTo(FirestoreFields.ASSIGNMENT_ID, assignmentId).asFlow()
            .map { snapshot ->
                snapshot.documents.mapNotNull { it.toSchedule() }
                    .sortedWith(compareBy({ it.weekday }, { it.startTime }))
            }

    override suspend fun schedulesOn(weekdays: Set<Int>): AppResult<List<Schedule>> = firestoreCall {
        val result = schedules.whereIn(FirestoreFields.WEEKDAY, weekdays.map { it.toLong() }).get().await()
        AppResult.Ok(result.documents.mapNotNull { it.toSchedule() })
    }

    override suspend fun createSchedules(drafts: List<ScheduleDraft>): AppResult<Unit> = firestoreCall {
        val batch = firestore.batch()
        drafts.forEach { d ->
            batch.set(
                schedules.document(),
                mapOf(
                    FirestoreFields.ASSIGNMENT_ID to d.assignmentId,
                    FirestoreFields.GROUP_ID to d.groupId,
                    FirestoreFields.SUBJECT_ID to d.subjectId,
                    FirestoreFields.TEACHER_UID to d.teacherUid,
                    FirestoreFields.SUBJECT_NAME to d.subjectName,
                    FirestoreFields.GROUP_NAME to d.groupName,
                    FirestoreFields.WEEKDAY to d.weekday,
                    FirestoreFields.START_TIME to d.startTime,
                    FirestoreFields.END_TIME to d.endTime,
                    FirestoreFields.ROOM to d.room,
                ),
            )
        }
        batch.commit().await()
        AppResult.Ok(Unit)
    }

    override suspend fun deleteSchedule(id: String): AppResult<Unit> = firestoreCall {
        schedules.document(id).delete().await()
        AppResult.Ok(Unit)
    }

    private fun DocumentSnapshot.toSchedule(): Schedule? {
        val assignmentId = getString(FirestoreFields.ASSIGNMENT_ID) ?: return null
        val weekday = getLong(FirestoreFields.WEEKDAY)?.toInt() ?: return null
        return Schedule(
            id = id,
            assignmentId = assignmentId,
            groupId = getString(FirestoreFields.GROUP_ID).orEmpty(),
            subjectId = getString(FirestoreFields.SUBJECT_ID).orEmpty(),
            teacherUid = getString(FirestoreFields.TEACHER_UID).orEmpty(),
            subjectName = getString(FirestoreFields.SUBJECT_NAME).orEmpty(),
            groupName = getString(FirestoreFields.GROUP_NAME).orEmpty(),
            weekday = weekday,
            startTime = getString(FirestoreFields.START_TIME).orEmpty(),
            endTime = getString(FirestoreFields.END_TIME).orEmpty(),
            room = getString(FirestoreFields.ROOM).orEmpty(),
        )
    }
}
