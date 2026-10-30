package com.jyhs.educheck.data.firebase

import android.util.Log
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.data.local.prefs.SessionPrefs
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.model.Session
import com.jyhs.educheck.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.tasks.await

/**
 * Sign-in with Firebase Authentication. The role comes from the `role` custom claim of the ID token
 * (set by the `set-role` Edge Function). The `users/{uid}` profile is only used for the `active` flag.
 */
@Singleton
class FirebaseAuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val prefs: SessionPrefs,
) : AuthRepository {

    override val session: Flow<Session?> = prefs.session

    override suspend fun restoreSession() {
        if (auth.currentUser == null) prefs.clear()
    }

    override suspend fun signIn(email: String, password: String): AppResult<Session> {
        return try {
            val user = auth.signInWithEmailAndPassword(email, password).await().user
            if (user == null) {
                AppResult.Error(ErrorReason.INVALID_CREDENTIALS)
            } else {
                // Force a refresh so a role assigned after the last sign-in is picked up.
                val claims = user.getIdToken(true).await().claims
                val role = (claims[CLAIM_ROLE] as? String)?.let { name -> Role.entries.firstOrNull { it.name == name } }
                if (role == null) {
                    auth.signOut()
                    AppResult.Error(ErrorReason.NO_PROFILE)
                } else if (!isProfileActive(user.uid)) {
                    auth.signOut()
                    AppResult.Error(ErrorReason.ACCOUNT_INACTIVE)
                } else {
                    val newSession = Session(
                        uid = user.uid,
                        email = user.email.orEmpty(),
                        role = role,
                    )
                    prefs.save(newSession)
                    AppResult.Ok(newSession)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseNetworkException) {
            AppResult.Error(ErrorReason.NO_NETWORK)
        } catch (e: FirebaseAuthInvalidCredentialsException) {
            AppResult.Error(ErrorReason.INVALID_CREDENTIALS)
        } catch (e: FirebaseAuthInvalidUserException) {
            AppResult.Error(ErrorReason.INVALID_CREDENTIALS)
        } catch (e: FirebaseFirestoreException) {
            Log.w(TAG, "Firestore: " + e.code, e)
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) {
                auth.signOut()
                AppResult.Error(ErrorReason.NO_NETWORK)
            } else {
                AppResult.Error(ErrorReason.UNKNOWN)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error while signing in", e)
            AppResult.Error(ErrorReason.UNKNOWN)
        }
    }

    private suspend fun isProfileActive(uid: String): Boolean {
        val profile = firestore.collection(FirestoreCollections.USERS).document(uid).get().await()
        return profile.exists() && profile.getBoolean(FirestoreFields.ACTIVE) == true
    }

    override suspend fun signOut() {
        auth.signOut()
        prefs.clear()
    }

    private companion object {
        const val TAG = "AuthRepository"
        const val CLAIM_ROLE = "role"
    }
}
