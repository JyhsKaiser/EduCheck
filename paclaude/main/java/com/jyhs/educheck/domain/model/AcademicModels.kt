package com.jyhs.educheck.domain.model

data class Subject(val id: String, val code: String, val name: String)

data class SubjectInput(val code: String, val name: String)

enum class Shift { MORNING, AFTERNOON }

/** Student as listed inside a group (copy kept in groups/{id}.students). */
data class GroupMember(val id: String, val name: String, val studentNumber: String)

data class SchoolGroup(
    val id: String,
    val name: String,
    val semester: Int,
    val shift: Shift,
    val period: String,
    val teacherUids: List<String>,
    val members: List<GroupMember>,
)

data class GroupInput(
    val name: String,
    val semester: Int,
    val shift: Shift,
    val period: String,
)

/** Group + subject + teacher. */
data class Assignment(
    val id: String,
    val groupId: String,
    val groupName: String,
    val subjectId: String,
    val subjectName: String,
    val teacherUid: String,
)

data class NewAssignment(
    val group: SchoolGroup,
    val subject: Subject,
    val teacher: AppUser,
)

data class Schedule(
    val id: String,
    val assignmentId: String,
    val groupId: String,
    val subjectId: String,
    val teacherUid: String,
    val subjectName: String,
    val groupName: String,
    val weekday: Int,        // 1 = Monday ... 7 = Sunday
    val startTime: String,   // HH:mm, 24 h
    val endTime: String,
    val room: String,
)

/** A schedule that does not exist yet (no id). */
data class ScheduleDraft(
    val assignmentId: String,
    val groupId: String,
    val subjectId: String,
    val teacherUid: String,
    val subjectName: String,
    val groupName: String,
    val weekday: Int,
    val startTime: String,
    val endTime: String,
    val room: String,
)

/** What the administrator types: one set of hours applied to several weekdays. */
data class ScheduleInput(
    val weekdays: Set<Int>,
    val startTime: String,
    val endTime: String,
    val room: String,
)

const val MAX_GROUP_SIZE = 80
