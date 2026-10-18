package com.jyhs.educheck.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Outbox local de la presencia en el plantel (RF-A04). Un registro por alumno y día. */
@Entity(
    tableName = "asistencia_local",
    indices = [
        Index(value = ["syncStatus"]),
        Index(value = ["alumnoId", "fecha"], unique = true),
    ],
)
data class AsistenciaEntity(
    @PrimaryKey val clientUuid: String,
    val alumnoId: String,
    val plantelId: String,
    val fecha: String,                 // yyyy-MM-dd en la zona horaria del plantel
    val eventoEpochMs: Long,           // hora del evento (reloj confiable)
    val elapsedRealtimeMs: Long,       // SystemClock.elapsedRealtime() al registrar
    val relojConfiable: Boolean,
    val metodo: String,                // GEOCERCA_WIFI | GEOCERCA | QR_PREFECTURA
    val confianza: Int,                // 0 = BAJA, 1 = ALTA
    val latitud: Double?,
    val longitud: Double?,
    val precisionM: Float?,
    val bssid: String?,
    val syncStatus: String = "PENDING", // PENDING | SYNCING | SYNCED | REJECTED
    val intentos: Int = 0,
    val ultimoError: String? = null,
    val creadoEnMs: Long,
    val sincronizadoEnMs: Long? = null,
)
