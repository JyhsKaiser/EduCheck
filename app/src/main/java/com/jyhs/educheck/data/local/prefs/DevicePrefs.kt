package com.jyhs.educheck.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.deviceStore: DataStore<Preferences> by preferencesDataStore(name = "device")

/** Identifier of this app installation (random, not the MAC nor the IMEI). */
@Singleton
class DevicePrefs @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val key = stringPreferencesKey("install_id")

    suspend fun installId(): String {
        context.deviceStore.data.first()[key]?.let { return it }
        val created = UUID.randomUUID().toString()
        context.deviceStore.edit { prefs -> if (prefs[key] == null) prefs[key] = created }
        return context.deviceStore.data.first()[key] ?: created
    }
}
