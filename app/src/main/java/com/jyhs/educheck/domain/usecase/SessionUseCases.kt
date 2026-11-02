package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.domain.model.Session
import com.jyhs.educheck.domain.repository.AuthRepository
import com.jyhs.educheck.domain.repository.ConnectivityProvider
import com.jyhs.educheck.domain.repository.PresenceTracker
import com.jyhs.educheck.domain.repository.PushTokenRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveSessionUseCase @Inject constructor(private val repository: AuthRepository) {
    operator fun invoke(): Flow<Session?> = repository.session
}

class RestoreSessionUseCase @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke() = repository.restoreSession()
}

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository,
    private val tracker: PresenceTracker,
    private val pushTokens: PushTokenRepository,
    private val connectivity: ConnectivityProvider,
) {
    suspend operator fun invoke() {
        tracker.stop()
        // Best effort, and only online (it must not delay signing out): this phone stops receiving this user's pushes.
        if (connectivity.isOnline()) pushTokens.clear()
        repository.signOut()
    }
}
