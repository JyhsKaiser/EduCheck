package com.jyhs.educheck.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jyhs.educheck.data.local.dao.PresenceDao
import com.jyhs.educheck.data.local.entity.PresenceEntity

@Database(
    entities = [PresenceEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun presenceDao(): PresenceDao
}
