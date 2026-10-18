package com.jyhs.educheck.domain.repository

import com.jyhs.educheck.core.common.Resultado
import com.jyhs.educheck.domain.model.Sesion
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    /** Sesión local (uid y rol). Permite abrir la app sin conexión tras el primer ingreso. */
    val sesion: Flow<Sesion?>

    /** Descarta la sesión local si Firebase ya no tiene un usuario autenticado. */
    suspend fun restaurarSesion()

    suspend fun iniciarSesion(email: String, clave: String): Resultado<Sesion>

    suspend fun cerrarSesion()
}
