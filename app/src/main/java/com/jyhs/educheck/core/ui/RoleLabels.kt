package com.jyhs.educheck.core.ui

import androidx.annotation.StringRes
import com.jyhs.educheck.R
import com.jyhs.educheck.domain.model.Role

@StringRes
fun Role.labelRes(): Int = when (this) {
    Role.STUDENT -> R.string.role_student
    Role.GUARDIAN -> R.string.role_guardian
    Role.TEACHER -> R.string.role_teacher
    Role.ADMIN -> R.string.role_admin
}
