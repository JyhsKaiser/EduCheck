package com.jyhs.educheck.data.local

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.data.local.dao.CacheDao
import com.jyhs.educheck.data.local.entity.CacheEntryEntity
import com.jyhs.educheck.domain.repository.ConnectivityProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Read-through cache for the data the student needs while offline (profile, device status, campus).
 * Online: the server value is returned and stored. Offline, or when the network fails: the last stored value.
 * The server always wins; nothing here is ever written back.
 */
@Singleton
class CacheStore @Inject constructor(
    private val dao: CacheDao,
    private val json: Json,
    private val connectivity: ConnectivityProvider,
) {
    suspend fun <T : Any> cached(
        key: String,
        serializer: KSerializer<T>,
        fetch: suspend () -> AppResult<T?>,
    ): AppResult<T?> {
        if (!connectivity.isOnline()) return readOrNoNetwork(key, serializer, AppResult.Error(ErrorReason.NO_NETWORK))
        val result = fetch()
        return when {
            result is AppResult.Ok -> {
                result.value?.let { write(key, serializer, it) }
                result
            }
            result is AppResult.Error && result.reason == ErrorReason.NO_NETWORK -> readOrNoNetwork(key, serializer, result)
            else -> result
        }
    }

    private suspend fun <T : Any> readOrNoNetwork(key: String, serializer: KSerializer<T>, fallback: AppResult.Error): AppResult<T?> {
        val stored = dao.get(key)?.let { text ->
            try {
                json.decodeFromString(serializer, text)
            } catch (_: Exception) {
                null // unreadable (an old format): behave as if there were no cache
            }
        }
        return if (stored != null) AppResult.Ok(stored) else fallback
    }

    private suspend fun <T : Any> write(key: String, serializer: KSerializer<T>, value: T) {
        dao.put(CacheEntryEntity(key, json.encodeToString(serializer, value), System.currentTimeMillis()))
    }
}
