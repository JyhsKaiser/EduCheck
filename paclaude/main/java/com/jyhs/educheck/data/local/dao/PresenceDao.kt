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

    @Query("SELECT * FROM local_presence WHERE syncStatus = 'PENDING' ORDER BY eventEpochMs LIMIT :limit")
    suspend fun pending(limit: Int): List<PresenceEntity>

    @Query("SELECT * FROM local_presence WHERE studentId = :studentId AND date = :date LIMIT 1")
    fun observeForDay(studentId: String, date: String): Flow<PresenceEntity?>

    @Query("SELECT COUNT(*) FROM local_presence WHERE syncStatus IN ('PENDING', 'SYNCING')")
    fun observePendingCount(): Flow<Int>
}
