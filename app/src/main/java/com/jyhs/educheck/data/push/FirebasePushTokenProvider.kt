package com.jyhs.educheck.data.push

import com.google.firebase.messaging.FirebaseMessaging
import com.jyhs.educheck.domain.repository.PushTokenProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.tasks.await

@Singleton
class FirebasePushTokenProvider @Inject constructor() : PushTokenProvider {
    override suspend fun currentToken(): String? =
        try {
            FirebaseMessaging.getInstance().token.await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        }
}
