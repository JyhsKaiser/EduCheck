package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.repository.MyDeviceRepository
import com.jyhs.educheck.domain.repository.PushTokenProvider
import com.jyhs.educheck.domain.repository.PushTokenRepository
import com.jyhs.educheck.domain.repository.PushTokenScheduler
import com.jyhs.educheck.domain.repository.StudentSelfRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * Registers this phone's FCM token for the signed-in user (RF-T02 needs the guardians' tokens).
 * A student phone first goes through the normal device registration (PENDING until the administrator approves it),
 * because the backend only pushes to ACTIVE devices.
 */
class RegisterPushTokenUseCase @Inject constructor(
    private val observeSession: ObserveSessionUseCase,
    private val student: StudentSelfRepository,
    private val device: MyDeviceRepository,
    private val provider: PushTokenProvider,
    private val tokens: PushTokenRepository,
) {
    suspend operator fun invoke(): AppResult<Unit> {
        val role: Role = observeSession().first()?.role ?: return AppResult.Ok(Unit) // not signed in: nothing to do
        val token = provider.currentToken() ?: return AppResult.Error(ErrorReason.UNKNOWN)

        if (role == Role.STUDENT) {
            val profile = when (val r = student.getProfile()) {
                is AppResult.Ok -> r.value
                is AppResult.Error -> return r
            }
            when (val r = device.ensureDevice(profile)) {
                is AppResult.Ok -> Unit
                is AppResult.Error -> return r
            }
        }
        return tokens.save(role, token)
    }
}

class RequestPushTokenRegistrationUseCase @Inject constructor(private val scheduler: PushTokenScheduler) {
    operator fun invoke() = scheduler.requestRegistration()
}
