package com.jyhs.educheck.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.jyhs.educheck.data.local.dao.AsistenciaDao
import com.jyhs.educheck.data.local.entity.AsistenciaEntity

@Database(
    entities = [AsistenciaEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun asistenciaDao(): AsistenciaDao
}
