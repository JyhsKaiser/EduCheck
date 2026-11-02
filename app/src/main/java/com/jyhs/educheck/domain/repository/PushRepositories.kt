package com.jyhs.educheck.domain.repository

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.domain.model.Role

/** The FCM registration token of this installation. */
interface PushTokenProvider {
    /** Null when Google Play services / FCM are not available. */
    suspend fun currentToken(): String?
}

/** Where the token is kept so the backend can reach this phone: devices/{installId}_{uid}.fcmToken. */
interface PushTokenRepository {
    /**
     * Stores [token] in this phone's record of the signed-in user. A guardian, teacher or administrator phone is
     * registered here (ACTIVE); a student phone is registered by the student flow, which needs the administrator's
     * approval, so for a student it fails with NOT_FOUND until that record exists.
     */
    suspend fun save(role: Role, token: String): AppResult<Unit>

    /** Blanks the token of this phone for the signed-in user (used when signing out). */
    suspend fun clear(): AppResult<Unit>
}

/** Asks for the token to be registered as soon as there is a connection (retrying with backoff). */
interface PushTokenScheduler {
    fun requestRegistration()
}
