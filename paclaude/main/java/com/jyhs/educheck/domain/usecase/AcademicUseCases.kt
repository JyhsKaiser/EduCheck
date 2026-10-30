package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.Assignment
import com.jyhs.educheck.domain.model.GroupInput
import com.jyhs.educheck.domain.model.GroupMember
import com.jyhs.educheck.domain.model.MAX_GROUP_SIZE
import com.jyhs.educheck.domain.model.NewAssignment
import com.jyhs.educheck.domain.model.Schedule
import com.jyhs.educheck.domain.model.ScheduleDraft
import com.jyhs.educheck.domain.model.ScheduleInput
import com.jyhs.educheck.domain.model.SchoolGroup
import com.jyhs.educheck.domain.model.Subject
import com.jyhs.educheck.domain.model.SubjectInput
import com.jyhs.educheck.domain.repository.AssignmentRepository
import com.jyhs.educheck.domain.repository.GroupRepository
import com.jyhs.educheck.domain.repository.ScheduleRepository
import com.jyhs.educheck.domain.repository.SubjectRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

// ---- Subjects
class ObserveSubjectsUseCase @Inject constructor(private val repository: SubjectRepository) {
    operator fun invoke(): Flow<List<Subject>> = repository.observeSubjects()
}

class SaveSubjectUseCase @Inject constructor(private val repository: SubjectRepository) {
    suspend operator fun invoke(id: String?, input: SubjectInput): AppResult<String> {
        if (input.code.isBlank() || input.name.isBlank()) return AppResult.Error(ErrorReason.INVALID_INPUT)
        return repository.saveSubject(id, SubjectInput(input.code.trim().uppercase(), input.name.trim()))
    }
}

class DeleteSubjectUseCase @Inject constructor(private val repository: SubjectRepository) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.deleteSubject(id)
}

// ---- Groups
class ObserveGroupsUseCase @Inject constructor(private val repository: GroupRepository) {
    operator fun invoke(): Flow<List<SchoolGroup>> = repository.observeGroups()
}

class SaveGroupUseCase @Inject constructor(private val repository: GroupRepository) {
    suspend operator fun invoke(id: String?, input: GroupInput): AppResult<String> {
        if (input.name.isBlank() || input.period.isBlank() || input.semester !in 1..12) {
            return AppResult.Error(ErrorReason.INVALID_INPUT)
        }
        return repository.saveGroup(id, input.copy(name = input.name.trim(), period = input.period.trim()))
    }
}

class DeleteGroupUseCase @Inject constructor(private val repository: GroupRepository) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.deleteGroup(id)
}

class SetGroupStudentsUseCase @Inject constructor(private val repository: GroupRepository) {
    suspend operator fun invoke(groupId: String, members: List<GroupMember>): AppResult<Unit> {
        if (members.size > MAX_GROUP_SIZE) return AppResult.Error(ErrorReason.LIMIT_EXCEEDED)
        return repository.setGroupStudents(groupId, members)
    }
}

// ---- Assignments
class ObserveAssignmentsUseCase @Inject constructor(private val repository: AssignmentRepository) {
    operator fun invoke(): Flow<List<Assignment>> = repository.observeAssignments()
}

class CreateAssignmentUseCase @Inject constructor(private val repository: AssignmentRepository) {
    suspend operator fun invoke(new: NewAssignment): AppResult<String> = repository.createAssignment(new)
}

class DeleteAssignmentUseCase @Inject constructor(private val repository: AssignmentRepository) {
    suspend operator fun invoke(assignment: Assignment): AppResult<Unit> = repository.deleteAssignment(assignment)
}

// ---- Schedules
class ObserveSchedulesUseCase @Inject constructor(private val repository: ScheduleRepository) {
    operator fun invoke(assignmentId: String): Flow<List<Schedule>> = repository.observeSchedules(assignmentId)
}

class DeleteScheduleUseCase @Inject constructor(private val repository: ScheduleRepository) {
    suspend operator fun invoke(id: String): AppResult<Unit> = repository.deleteSchedule(id)
}

/**
 * Creates one schedule per selected weekday. It is rejected if, on any of those days, another class
 * overlaps in time AND shares the teacher, the group or the room.
 */
class CreateSchedulesUseCase @Inject constructor(private val repository: ScheduleRepository) {

    suspend operator fun invoke(assignment: Assignment, input: ScheduleInput): AppResult<Unit> {
        val start = input.startTime.trim()
        val end = input.endTime.trim()
        val room = input.room.trim()
        if (input.weekdays.isEmpty() || input.weekdays.any { it !in 1..7 } || room.isEmpty()) {
            return AppResult.Error(ErrorReason.INVALID_INPUT)
        }
        if (!TIME.matches(start) || !TIME.matches(end) || start >= end) {
            return AppResult.Error(ErrorReason.INVALID_TIME)
        }

        val drafts = input.weekdays.sorted().map { day ->
            ScheduleDraft(
                assignmentId = assignment.id,
                groupId = assignment.groupId,
                subjectId = assignment.subjectId,
                teacherUid = assignment.teacherUid,
                subjectName = assignment.subjectName,
                groupName = assignment.groupName,
                weekday = day,
                startTime = start,
                endTime = end,
                room = room,
            )
        }

        val existing = when (val result = repository.schedulesOn(input.weekdays)) {
            is AppResult.Ok -> result.value
            is AppResult.Error -> return result
        }
        for (draft in drafts) {
            val clash = findClash(draft, existing)
            if (clash != null) {
                val detail = clash.subjectName + " (" + clash.groupName + ") " + clash.startTime + "-" + clash.endTime
                return AppResult.Error(ErrorReason.SCHEDULE_CONFLICT, detail)
            }
        }
        return repository.createSchedules(drafts)
    }

    private companion object {
        /** HH:mm, 24 h, zero padded, so string comparison orders times correctly. */
        val TIME = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")
    }
}

/** Same weekday, overlapping time, and the same teacher, group or room. */
fun findClash(draft: ScheduleDraft, existing: List<Schedule>): Schedule? = existing.firstOrNull { other ->
    other.weekday == draft.weekday &&
        draft.startTime < other.endTime && other.startTime < draft.endTime &&
        (other.teacherUid == draft.teacherUid ||
            other.groupId == draft.groupId ||
            other.room.equals(draft.room, ignoreCase = true))
}
