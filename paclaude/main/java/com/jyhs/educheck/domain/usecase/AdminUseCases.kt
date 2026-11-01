package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.AppUser
import com.jyhs.educheck.domain.model.Device
import com.jyhs.educheck.domain.model.DeviceStatus
import com.jyhs.educheck.domain.model.NewUser
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.model.Student
import com.jyhs.educheck.domain.model.StudentInput
import com.jyhs.educheck.domain.repository.DeviceRepository
import com.jyhs.educheck.domain.repository.StudentRepository
import com.jyhs.educheck.domain.repository.UserAdminRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

// ---- Users
class ObserveUsersUseCase @Inject constructor(private val repository: UserAdminRepository) {
    operator fun invoke(): Flow<List<AppUser>> = repository.observeUsers()
}

class CreateUserUseCase @Inject constructor(private val repository: UserAdminRepository) {
    suspend operator fun invoke(newUser: NewUser): AppResult<String> {
        val email = newUser.email.trim()
        if (newUser.name.isBlank() || !EMAIL.matches(email)) return AppResult.Error(ErrorReason.INVALID_INPUT)
        if (newUser.password.length < MIN_PASSWORD) return AppResult.Error(ErrorReason.WEAK_PASSWORD)
        if (newUser.role == Role.STUDENT) {
            // A student account always has a student record: a new one or an existing one without account.
            if ((newUser.studentId == null) == (newUser.newStudent == null)) {
                return AppResult.Error(ErrorReason.INVALID_INPUT)
            }
            val student = newUser.newStudent
            if (student != null && (student.studentNumber.isBlank() || student.firstNames.isBlank() || student.lastNames.isBlank())) {
                return AppResult.Error(ErrorReason.INVALID_INPUT)
            }
        } else if (newUser.studentId != null || newUser.newStudent != null) {
            return AppResult.Error(ErrorReason.INVALID_INPUT)
        }
        return repository.createUser(
            newUser.copy(
                email = email,
                name = newUser.name.trim(),
                newStudent = newUser.newStudent?.let {
                    StudentInput(it.studentNumber.trim(), it.firstNames.trim(), it.lastNames.trim())
                },
            ),
        )
    }

    private companion object {
        const val MIN_PASSWORD = 6
        val EMAIL = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
    }
}

class SetUserActiveUseCase @Inject constructor(private val repository: UserAdminRepository) {
    suspend operator fun invoke(uid: String, active: Boolean): AppResult<Unit> =
        repository.setUserActive(uid, active)
}

// ---- Students
class ObserveStudentsUseCase @Inject constructor(private val repository: StudentRepository) {
    operator fun invoke(): Flow<List<Student>> = repository.observeStudents()
}

class SaveStudentUseCase @Inject constructor(private val repository: StudentRepository) {
    suspend operator fun invoke(id: String?, input: StudentInput): AppResult<String> {
        if (input.studentNumber.isBlank() || input.firstNames.isBlank() || input.lastNames.isBlank()) {
            return AppResult.Error(ErrorReason.INVALID_INPUT)
        }
        return repository.saveStudent(
            id,
            StudentInput(input.studentNumber.trim(), input.firstNames.trim(), input.lastNames.trim()),
        )
    }
}

class SetGuardiansUseCase @Inject constructor(private val repository: StudentRepository) {
    suspend operator fun invoke(studentId: String, guardianUids: List<String>): AppResult<Unit> =
        repository.setGuardians(studentId, guardianUids)
}

class SetStudentActiveUseCase @Inject constructor(private val repository: StudentRepository) {
    suspend operator fun invoke(studentId: String, active: Boolean): AppResult<Unit> =
        repository.setStudentActive(studentId, active)
}

// ---- Devices
class ObserveDevicesUseCase @Inject constructor(private val repository: DeviceRepository) {
    operator fun invoke(): Flow<List<Device>> = repository.observeDevices()
}

class SetDeviceStatusUseCase @Inject constructor(private val repository: DeviceRepository) {
    suspend operator fun invoke(deviceId: String, status: DeviceStatus): AppResult<Unit> =
        repository.setStatus(deviceId, status)
}
