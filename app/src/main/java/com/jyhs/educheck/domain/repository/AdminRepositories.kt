package com.jyhs.educheck.domain.repository

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.domain.model.AppUser
import com.jyhs.educheck.domain.model.Device
import com.jyhs.educheck.domain.model.DeviceStatus
import com.jyhs.educheck.domain.model.NewUser
import com.jyhs.educheck.domain.model.Student
import com.jyhs.educheck.domain.model.StudentInput
import kotlinx.coroutines.flow.Flow

/** Administration screens work online only: they read and write Firestore directly. */
interface UserAdminRepository {
    fun observeUsers(): Flow<List<AppUser>>

    /** Creates the account through the `create-user` Edge Function. Returns the new uid. */
    suspend fun createUser(newUser: NewUser): AppResult<String>

    suspend fun setUserActive(uid: String, active: Boolean): AppResult<Unit>
}

interface StudentRepository {
    fun observeStudents(): Flow<List<Student>>

    /** Creates (id == null) or updates a student record. Returns its id. */
    suspend fun saveStudent(id: String?, input: StudentInput): AppResult<String>

    suspend fun setGuardians(studentId: String, guardianUids: List<String>): AppResult<Unit>

    suspend fun setStudentActive(studentId: String, active: Boolean): AppResult<Unit>
}

interface DeviceRepository {
    fun observeDevices(): Flow<List<Device>>

    suspend fun setStatus(deviceId: String, status: DeviceStatus): AppResult<Unit>
}
