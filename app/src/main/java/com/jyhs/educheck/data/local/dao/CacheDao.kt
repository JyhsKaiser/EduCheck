package com.jyhs.educheck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jyhs.educheck.data.local.entity.CacheEntryEntity

@Dao
interface CacheDao {
    @Query("SELECT json FROM cache_entry WHERE `key` = :key")
    suspend fun get(key: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun put(entry: CacheEntryEntity)
}
