package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.domain.model.Session
import com.jyhs.educheck.domain.repository.AuthRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSessionUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): Flow<Session?> = repository.session
}

class RestoreSessionUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.restoreSession()
}

class SignOutUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.signOut()
}
