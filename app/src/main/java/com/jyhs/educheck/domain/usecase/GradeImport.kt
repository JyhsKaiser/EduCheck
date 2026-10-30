package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.Assignment
import com.jyhs.educheck.domain.model.GradeImportError
import com.jyhs.educheck.domain.model.GradeImportErrorType
import com.jyhs.educheck.domain.model.GradeImportPlan
import com.jyhs.educheck.domain.model.GradeRecord
import com.jyhs.educheck.domain.model.SchoolGroup
import com.jyhs.educheck.domain.model.Student
import com.jyhs.educheck.domain.model.Subject
import com.jyhs.educheck.domain.repository.GradeRepository
import com.jyhs.educheck.domain.repository.TextFileReader
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Turns the CSV exported by the other system into grade records.
 *
 * Header (any order, case-insensitive): studentNumber, subjectCode, groupName, partial1, partial2, partial3.
 * Separator: comma or semicolon (decided from the header). Decimals use a dot; with semicolon also a comma.
 * Grades are 0 to 10; a blank partial is simply not stored.
 */
object GradeCsvPlanner {

    const val MAX_ROWS = 2000
    private val REQUIRED = listOf("studentnumber", "subjectcode", "groupname")
    private val PARTIAL_COLUMNS = listOf("partial1", "partial2", "partial3")

    fun plan(
        csv: String,
        students: List<Student>,
        subjects: List<Subject>,
        groups: List<SchoolGroup>,
        assignments: List<Assignment>,
    ): GradeImportPlan {
        val lines = csv.removePrefix("\uFEFF").lines().withIndex().filter { it.value.isNotBlank() }
        if (lines.isEmpty()) {
            return GradeImportPlan(emptyList(), listOf(GradeImportError(1, GradeImportErrorType.HEADER)), 0)
        }

        val headerLine = lines.first().value
        val delimiter = if (headerLine.contains(';') && !headerLine.contains(',')) ';' else ','
        val header = splitLine(headerLine, delimiter).map { it.trim().lowercase() }
        val col = header.withIndex().associate { it.value to it.index }
        if (REQUIRED.any { it !in col } || PARTIAL_COLUMNS.none { it in col }) {
            return GradeImportPlan(emptyList(), listOf(GradeImportError(1, GradeImportErrorType.HEADER)), 0)
        }

        val rows = lines.drop(1)
        if (rows.size > MAX_ROWS) {
            return GradeImportPlan(emptyList(), listOf(GradeImportError(0, GradeImportErrorType.TOO_MANY_ROWS, MAX_ROWS.toString())), rows.size)
        }

        val studentsByNumber = students.associateBy { it.studentNumber.trim().lowercase() }
        val subjectsByCode = subjects.associateBy { it.code.trim().uppercase() }
        val groupsByName = groups.groupBy { it.name.trim().lowercase() }

        val records = LinkedHashMap<String, GradeRecord>()
        val errors = mutableListOf<GradeImportError>()

        for ((index, line) in rows) {
            val lineNo = index + 1
            val cells = splitLine(line, delimiter).map { it.trim() }
            fun cell(name: String): String = col[name]?.let { cells.getOrNull(it) }.orEmpty()

            val studentNumber = cell("studentnumber")
            val subjectCode = cell("subjectcode")
            val groupName = cell("groupname")
            if (studentNumber.isEmpty() || subjectCode.isEmpty() || groupName.isEmpty()) {
                errors += GradeImportError(lineNo, GradeImportErrorType.COLUMNS)
                continue
            }

            val student = studentsByNumber[studentNumber.lowercase()]
            if (student == null) {
                errors += GradeImportError(lineNo, GradeImportErrorType.STUDENT_NOT_FOUND, studentNumber)
                continue
            }
            val subject = subjectsByCode[subjectCode.uppercase()]
            if (subject == null) {
                errors += GradeImportError(lineNo, GradeImportErrorType.SUBJECT_NOT_FOUND, subjectCode)
                continue
            }
            val matchingGroups = groupsByName[groupName.lowercase()].orEmpty()
            if (matchingGroups.isEmpty()) {
                errors += GradeImportError(lineNo, GradeImportErrorType.GROUP_NOT_FOUND, groupName)
                continue
            }
            if (matchingGroups.size > 1) {
                errors += GradeImportError(lineNo, GradeImportErrorType.GROUP_AMBIGUOUS, groupName)
                continue
            }
            val group = matchingGroups.first()
            val assignment = assignments.firstOrNull { it.groupId == group.id && it.subjectId == subject.id }
            if (assignment == null) {
                errors += GradeImportError(lineNo, GradeImportErrorType.NO_ASSIGNMENT, "$subjectCode / $groupName")
                continue
            }
            if (group.members.none { it.id == student.id }) {
                errors += GradeImportError(lineNo, GradeImportErrorType.STUDENT_NOT_IN_GROUP, "$studentNumber / $groupName")
                continue
            }

            val partials = mutableMapOf<Int, Double>()
            var invalid: String? = null
            PARTIAL_COLUMNS.forEachIndexed { i, name ->
                val raw = cell(name)
                if (raw.isNotEmpty()) {
                    val value = raw.replace(',', '.').toDoubleOrNull()
                    if (value == null || value !in 0.0..10.0) invalid = raw else partials[i + 1] = value
                }
            }
            if (invalid != null || partials.isEmpty()) {
                errors += GradeImportError(lineNo, GradeImportErrorType.INVALID_GRADE, invalid.orEmpty())
                continue
            }

            val key = assignment.id + "_" + student.id
            if (key in records) {
                errors += GradeImportError(lineNo, GradeImportErrorType.DUPLICATE_ROW, "$studentNumber / $subjectCode")
                continue
            }
            records[key] = GradeRecord(assignment.id, student.id, assignment.teacherUid, partials)
        }
        return GradeImportPlan(records.values.toList(), errors, rows.size)
    }

    /** Splits one CSV line; a field may be wrapped in double quotes (and contain the delimiter). */
    private fun splitLine(line: String, delimiter: Char): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0
        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && line.getOrNull(i + 1) == '"' -> { current.append('"'); i++ }
                c == '"' -> inQuotes = !inQuotes
                c == delimiter && !inQuotes -> { result += current.toString(); current.clear() }
                else -> current.append(c)
            }
            i++
        }
        result += current.toString()
        return result
    }
}

class PrepareGradeImportUseCase @Inject constructor(
    private val fileReader: TextFileReader,
    private val observeStudents: ObserveStudentsUseCase,
    private val observeSubjects: ObserveSubjectsUseCase,
    private val observeGroups: ObserveGroupsUseCase,
    private val observeAssignments: ObserveAssignmentsUseCase,
) {
    suspend operator fun invoke(uri: String): AppResult<GradeImportPlan> {
        val text = when (val read = fileReader.read(uri, MAX_FILE_BYTES)) {
            is AppResult.Ok -> read.value
            is AppResult.Error -> return read
        }
        return try {
            AppResult.Ok(
                GradeCsvPlanner.plan(
                    csv = text,
                    students = observeStudents().first(),
                    subjects = observeSubjects().first(),
                    groups = observeGroups().first(),
                    assignments = observeAssignments().first(),
                ),
            )
        } catch (e: kotlin.coroutines.cancellation.CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(ErrorReason.UNKNOWN)
        }
    }

    private companion object {
        const val MAX_FILE_BYTES = 1_000_000
    }
}

class ImportGradesUseCase @Inject constructor(private val repository: GradeRepository) {
    suspend operator fun invoke(records: List<GradeRecord>): AppResult<Unit> {
        if (records.isEmpty()) return AppResult.Error(ErrorReason.INVALID_INPUT)
        return repository.saveGrades(records)
    }
}
