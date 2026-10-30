package com.jyhs.educheck.data.firebase

import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withTimeoutOrNull

/** Live results of a query. A listener error (for example missing permission) ends the flow with that error. */
fun Query.asFlow(): Flow<QuerySnapshot> = callbackFlow {
    val registration = addSnapshotListener { snapshot, error ->
        if (error != null) {
            close(error)
        } else if (snapshot != null) {
            trySend(snapshot)
        }
    }
    awaitClose { registration.remove() }
}

private const val WRITE_TIMEOUT_MS = 15_000L

/**
 * Runs a Firestore write and maps its failures. Firestore only completes a write once the server
 * acknowledges it, so without a connection it would wait forever: after the timeout the call is
 * reported as NO_NETWORK (the write stays queued and is applied when the connection returns).
 */
suspend fun <T> firestoreCall(block: suspend () -> AppResult<T>): AppResult<T> {
    return try {
        withTimeoutOrNull(WRITE_TIMEOUT_MS) { block() } ?: AppResult.Error(ErrorReason.NO_NETWORK)
    } catch (e: CancellationException) {
        throw e
    } catch (e: FirebaseFirestoreException) {
        AppResult.Error(
            when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> ErrorReason.FORBIDDEN
                FirebaseFirestoreException.Code.UNAVAILABLE -> ErrorReason.NO_NETWORK
                FirebaseFirestoreException.Code.NOT_FOUND -> ErrorReason.NOT_FOUND
                else -> ErrorReason.UNKNOWN
            },
        )
    } catch (e: Exception) {
        AppResult.Error(ErrorReason.UNKNOWN)
    }
}
