package com.jyhs.educheck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jyhs.educheck.data.local.entity.PresenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface PresenceDao {

    /** Returns -1 if a record for that student and day already existed. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(entity: PresenceEntity): Long

    @Query("SELECT * FROM local_presence WHERE studentId = :studentId AND date = :date LIMIT 1")
    suspend fun find(studentId: String, date: String): PresenceEntity?

    @Query("SELECT * FROM local_presence WHERE studentId = :studentId AND date = :date LIMIT 1")
    fun observeForDay(studentId: String, date: String): Flow<PresenceEntity?>

    @Query("SELECT * FROM local_presence WHERE studentId = :studentId AND syncStatus = 'PENDING' ORDER BY eventEpochMs")
    suspend fun pendingFor(studentId: String): List<PresenceEntity>

    @Query("SELECT COUNT(*) FROM local_presence WHERE syncStatus = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("DELETE FROM local_presence WHERE studentId = :studentId AND date = :date AND syncStatus = 'REJECTED'")
    suspend fun deleteRejected(studentId: String, date: String)

    @Query("UPDATE local_presence SET syncStatus = 'SYNCED', syncedAtMs = :at, lastError = NULL WHERE clientUuid = :clientUuid")
    suspend fun markSynced(clientUuid: String, at: Long)

    @Query("UPDATE local_presence SET syncStatus = 'REJECTED', lastError = :reason, attempts = attempts + 1 WHERE clientUuid = :clientUuid")
    suspend fun markRejected(clientUuid: String, reason: String)

    @Query("UPDATE local_presence SET attempts = :attempts, lastError = :reason WHERE clientUuid = :clientUuid")
    suspend fun markAttempt(clientUuid: String, reason: String, attempts: Int)
}
