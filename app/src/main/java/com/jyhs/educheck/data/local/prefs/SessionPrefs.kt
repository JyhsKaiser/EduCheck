package com.jyhs.educheck.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.jyhs.educheck.domain.model.Role
import com.jyhs.educheck.domain.model.Session
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.sessionStore: DataStore<Preferences> by preferencesDataStore(name = "session")

/** Stores uid and role (never passwords or tokens) so the app can open offline. */
@Singleton
class SessionPrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val UID = stringPreferencesKey("uid")
        val EMAIL = stringPreferencesKey("email")
        val ROLE = stringPreferencesKey("role")
    }

    val session: Flow<Session?> = context.sessionStore.data.map { p ->
        val uid = p[Keys.UID]
        val role = p[Keys.ROLE]?.let { name -> Role.entries.firstOrNull { it.name == name } }
        if (uid == null || role == null) {
            null
        } else {
            Session(uid = uid, email = p[Keys.EMAIL].orEmpty(), role = role)
        }
    }

    suspend fun save(session: Session) {
        context.sessionStore.edit { p ->
            p[Keys.UID] = session.uid
            p[Keys.EMAIL] = session.email
            p[Keys.ROLE] = session.role.name
        }
    }

    suspend fun clear() {
        context.sessionStore.edit { it.clear() }
    }
}
