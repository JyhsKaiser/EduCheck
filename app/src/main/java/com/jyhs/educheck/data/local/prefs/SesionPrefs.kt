package com.jyhs.educheck.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jyhs.educheck.domain.model.Rol
import com.jyhs.educheck.domain.model.Sesion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sesionStore: DataStore<Preferences> by preferencesDataStore(name = "sesion")

/** Guarda uid y rol (no contraseñas ni tokens) para poder abrir la app sin conexión. */
@Singleton
class SesionPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Claves {
        val UID = stringPreferencesKey("uid")
        val EMAIL = stringPreferencesKey("email")
        val ROL = stringPreferencesKey("rol")
        val PLANTEL = stringPreferencesKey("plantel_id")
    }

    val sesion: Flow<Sesion?> = context.sesionStore.data.map { p ->
        val uid = p[Claves.UID]
        val rol = p[Claves.ROL]?.let { nombre -> Rol.entries.firstOrNull { it.name == nombre } }
        if (uid == null || rol == null) {
            null
        } else {
            Sesion(uid = uid, email = p[Claves.EMAIL].orEmpty(), rol = rol, plantelId = p[Claves.PLANTEL])
        }
    }

    suspend fun guardar(sesion: Sesion) {
        context.sesionStore.edit { p ->
            p[Claves.UID] = sesion.uid
            p[Claves.EMAIL] = sesion.email
            p[Claves.ROL] = sesion.rol.name
            if (sesion.plantelId != null) p[Claves.PLANTEL] = sesion.plantelId else p.remove(Claves.PLANTEL)
        }
    }

    suspend fun limpiar() {
        context.sesionStore.edit { it.clear() }
    }
}
