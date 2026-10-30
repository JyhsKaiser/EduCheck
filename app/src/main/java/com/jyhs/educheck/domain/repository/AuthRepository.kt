package com.jyhs.educheck.domain.repository

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.domain.model.Session
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Local session (uid and role). Lets the app open offline after the first sign-in. */
    val session: Flow<Session?>

    /** Discards the local session if Firebase no longer has an authenticated user. */
    suspend fun restoreSession()

    suspend fun signIn(email: String, password: String): AppResult<Session>

    suspend fun signOut()
}
