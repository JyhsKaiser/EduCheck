package com.jyhs.educheck.domain.model

/**
 * User levels of the single APK. ADMIN also acts as academic coordinator and covers the
 * prefecture duties (manual attendance registration, justifications, dashboard).
 */
enum class Role { STUDENT, GUARDIAN, TEACHER, ADMIN }
