package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.Session
import com.jyhs.educheck.domain.repository.AuthRepository
import javax.inject.Inject

class SignInUseCase @Inject constructor(
    private val repository: AuthRepository,
) {
    suspend operator fun invoke(email: String, password: String): AppResult<Session> {
        if (email.isBlank() || password.isBlank()) {
            return AppResult.Error(ErrorReason.INVALID_CREDENTIALS)
        }
        return repository.signIn(email.trim(), password)
    }
}
