package com.jyhs.educheck.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.jyhs.educheck.data.local.entity.AsistenciaEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AsistenciaDao {

    /** Devuelve -1 si ya existía un registro del alumno para ese día. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertar(entidad: AsistenciaEntity): Long

    @Query("SELECT * FROM asistencia_local WHERE syncStatus = 'PENDING' ORDER BY eventoEpochMs LIMIT :limite")
    suspend fun pendientes(limite: Int): List<AsistenciaEntity>

    @Query("SELECT * FROM asistencia_local WHERE alumnoId = :alumnoId AND fecha = :fecha LIMIT 1")
    fun observarDelDia(alumnoId: String, fecha: String): Flow<AsistenciaEntity?>

    @Query("SELECT COUNT(*) FROM asistencia_local WHERE syncStatus IN ('PENDING', 'SYNCING')")
    fun observarPendientes(): Flow<Int>
}
