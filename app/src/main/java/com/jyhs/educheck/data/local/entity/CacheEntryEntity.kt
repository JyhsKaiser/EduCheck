package com.jyhs.educheck.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Last value fetched from the server (profile, device status, campus), stored as JSON for offline use. */
@Entity(tableName = "cache_entry")
data class CacheEntryEntity(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAtMs: Long,
)
