package com.jyhs.educheck.domain.repository

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.domain.model.Assignment
import com.jyhs.educheck.domain.model.GroupInput
import com.jyhs.educheck.domain.model.GroupMember
import com.jyhs.educheck.domain.model.NewAssignment
import com.jyhs.educheck.domain.model.Schedule
import com.jyhs.educheck.domain.model.ScheduleDraft
import com.jyhs.educheck.domain.model.SchoolGroup
import com.jyhs.educheck.domain.model.Subject
import com.jyhs.educheck.domain.model.SubjectInput
import kotlinx.coroutines.flow.Flow

interface SubjectRepository {
    fun observeSubjects(): Flow<List<Subject>>

    /** Creates (id == null) or updates. A renamed subject is propagated to its assignments and schedules. */
    suspend fun saveSubject(id: String?, input: SubjectInput): AppResult<String>

    /** Fails with HAS_DEPENDENTS while the subject has assignments. */
    suspend fun deleteSubject(id: String): AppResult<Unit>
}

interface GroupRepository {
    fun observeGroups(): Flow<List<SchoolGroup>>

    /** Creates (id == null) or updates. A renamed group is propagated to its assignments and schedules. */
    suspend fun saveGroup(id: String?, input: GroupInput): AppResult<String>

    /** Fails with HAS_DEPENDENTS while the group has assignments. Clears `groupId` of its students. */
    suspend fun deleteGroup(id: String): AppResult<Unit>

    /**
     * Replaces the student list of the group. A student belongs to one group only: whoever is selected
     * leaves their previous group. Group list and `students.groupId` change in one batch.
     */
    suspend fun setGroupStudents(groupId: String, members: List<GroupMember>): AppResult<Unit>
}

interface AssignmentRepository {
    fun observeAssignments(): Flow<List<Assignment>>

    /** Creates the assignment and adds the teacher to `groups.teacherUids` (one batch). */
    suspend fun createAssignment(new: NewAssignment): AppResult<String>

    /**
     * Deletes the assignment and, if it was the teacher's last one in that group, removes the teacher from
     * `groups.teacherUids`. Fails with HAS_DEPENDENTS while it has schedules.
     */
    suspend fun deleteAssignment(assignment: Assignment): AppResult<Unit>
}

interface ScheduleRepository {
    fun observeSchedules(assignmentId: String): Flow<List<Schedule>>

    /** Every schedule of the given weekdays (used to detect clashes). */
    suspend fun schedulesOn(weekdays: Set<Int>): AppResult<List<Schedule>>

    suspend fun createSchedules(drafts: List<ScheduleDraft>): AppResult<Unit>

    suspend fun deleteSchedule(id: String): AppResult<Unit>
}
