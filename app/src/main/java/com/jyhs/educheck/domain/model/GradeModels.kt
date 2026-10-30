package com.jyhs.educheck.domain.model

/** Grades of one student in one assignment. Keys of [partials] are 1, 2 and 3. */
data class GradeRecord(
    val assignmentId: String,
    val studentId: String,
    val teacherUid: String,
    val partials: Map<Int, Double>,
)

enum class GradeImportErrorType {
    HEADER,
    COLUMNS,
    STUDENT_NOT_FOUND,
    SUBJECT_NOT_FOUND,
    GROUP_NOT_FOUND,
    GROUP_AMBIGUOUS,
    NO_ASSIGNMENT,
    STUDENT_NOT_IN_GROUP,
    INVALID_GRADE,
    DUPLICATE_ROW,
    TOO_MANY_ROWS,
}

/** [line] is the 1-based line of the file (the header is line 1). [detail] is the offending value. */
data class GradeImportError(val line: Int, val type: GradeImportErrorType, val detail: String = "")

data class GradeImportPlan(
    val records: List<GradeRecord>,
    val errors: List<GradeImportError>,
    val totalRows: Int,
)
