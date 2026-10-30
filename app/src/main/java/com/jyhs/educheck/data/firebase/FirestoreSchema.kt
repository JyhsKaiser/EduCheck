package com.jyhs.educheck.data.firebase

/** Firestore collection names (see docs/MODELO_FIRESTORE.md). */
object FirestoreCollections {
    const val CONFIG = "config"                // holds the single document `config/campus`
    const val USERS = "users"
    const val DEVICES = "devices"
    const val STUDENTS = "students"
    const val PRESENCES = "presences"
    const val ABSENCES = "absences"            // written by the backend
    const val JUSTIFICATIONS = "justifications"
    const val GROUPS = "groups"
    const val SUBJECTS = "subjects"
    const val ASSIGNMENTS = "assignments"
    const val SCHEDULES = "schedules"
    const val CLASS_SESSIONS = "class_sessions"
    const val GRADES = "grades"
    const val EMERGENCIES = "emergencies"
    const val NOTIFICATIONS = "notifications"
}

/** Well-known document ids. */
object FirestoreDocs {
    const val CAMPUS = "campus"                // config/campus: geofence, BSSIDs and tolerances
}

/** Fields used in more than one place. The rest is added together with each module. */
object FirestoreFields {
    // users/{uid}
    const val ROLE = "role"
    const val ACTIVE = "active"
    const val STUDENT_ID = "studentId"
    const val NAME = "name"
    const val EMAIL = "email"

    // students/{id}
    const val STUDENT_NUMBER = "studentNumber"
    const val FIRST_NAMES = "firstNames"
    const val LAST_NAMES = "lastNames"
    const val GROUP_ID = "groupId"
    const val STUDENT_UID = "studentUid"

    // subjects, groups, assignments, schedules
    const val CODE = "code"
    const val SEMESTER = "semester"
    const val SHIFT = "shift"
    const val PERIOD = "period"
    const val TEACHER_UIDS = "teacherUids"
    const val GROUP_STUDENTS = "students"     // groups/{id}.students: [{id, name, studentNumber}]
    const val SUBJECT_ID = "subjectId"
    const val SUBJECT_NAME = "subjectName"
    const val GROUP_NAME = "groupName"
    const val TEACHER_UID = "teacherUid"
    const val ASSIGNMENT_ID = "assignmentId"
    const val WEEKDAY = "weekday"
    const val START_TIME = "startTime"
    const val END_TIME = "endTime"
    const val ROOM = "room"

    // config/campus
    const val CENTER = "center"
    const val RADIUS_METERS = "radiusMeters"
    const val TOLERANCE_METERS = "toleranceMeters"
    const val TOLERANCE_MINUTES = "toleranceMinutes"
    const val TIME_ZONE = "timeZone"
    const val BSSIDS = "bssids"

    // grades/{assignmentId}_{studentId}
    const val PARTIALS = "partials"
    const val IMPORTED_AT = "importedAt"

    // devices/{installId}
    const val USER_UID = "userUid"
    const val MODEL = "model"

    // statuses and methods
    const val STATUS = "status"
    const val DATE = "date"
    const val GUARDIAN_UIDS = "guardianUids"
}

/** Deterministic ids: avoid duplicates (one presence per student and day, one class session per schedule and day). */
object FirestoreIds {
    fun presence(studentId: String, date: String): String = studentId + "_" + date
    fun classSession(scheduleId: String, date: String): String = scheduleId + "_" + date
    fun grade(assignmentId: String, studentId: String): String = assignmentId + "_" + studentId
}
