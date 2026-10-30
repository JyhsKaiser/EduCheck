package com.jyhs.educheck.domain.model

data class AppUser(
    val uid: String,
    val name: String,
    val email: String,
    val role: Role,
    val active: Boolean,
    val studentId: String?,
)

data class NewUser(
    val email: String,
    val password: String,
    val name: String,
    val role: Role,
    val studentId: String?,
)

data class Student(
    val id: String,
    val studentNumber: String,
    val firstNames: String,
    val lastNames: String,
    val groupId: String?,
    val studentUid: String?,
    val guardianUids: List<String>,
    val active: Boolean,
) {
    val fullName: String get() = "$firstNames $lastNames".trim()
}

data class StudentInput(
    val studentNumber: String,
    val firstNames: String,
    val lastNames: String,
)

enum class DeviceStatus { PENDING, ACTIVE, REVOKED }

data class Device(
    val id: String,
    val userUid: String,
    val studentId: String?,
    val role: Role,
    val status: DeviceStatus,
    val model: String,
)
