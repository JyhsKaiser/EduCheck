package com.jyhs.educheck.data.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.GeoFix
import com.jyhs.educheck.domain.repository.LocationProvider
import com.jyhs.educheck.domain.repository.TextFileReader
import com.jyhs.educheck.domain.repository.WifiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

private fun Context.hasFineLocation(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED

/** Current position with the fused provider (GPS + network). */
@Singleton
class FusedLocationProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : LocationProvider {

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): AppResult<GeoFix> {
        if (!context.hasFineLocation()) return AppResult.Error(ErrorReason.PERMISSION_DENIED)
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val location = withTimeoutOrNull(LOCATION_TIMEOUT_MS) {
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null).await()
            }
            if (location == null) {
                AppResult.Error(ErrorReason.LOCATION_UNAVAILABLE)
            } else {
                AppResult.Ok(GeoFix(location.latitude, location.longitude, location.accuracy))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppResult.Error(ErrorReason.LOCATION_UNAVAILABLE)
        }
    }

    private companion object {
        const val LOCATION_TIMEOUT_MS = 15_000L
    }
}

/** BSSID of the connected access point. Android hides it without the location permission and location services. */
@Singleton
class AndroidWifiProvider @Inject constructor(
    @ApplicationContext private val context: Context,
) : WifiProvider {

    @Suppress("DEPRECATION")
    override fun currentBssid(): String? {
        if (!context.hasFineLocation()) return null
        val wifi = context.applicationContext.getSystemService(WifiManager::class.java) ?: return null
        val bssid = wifi.connectionInfo?.bssid?.uppercase() ?: return null
        return bssid.takeIf { it != UNKNOWN_BSSID && it != EMPTY_BSSID }
    }

    private companion object {
        const val UNKNOWN_BSSID = "02:00:00:00:00:00"
        const val EMPTY_BSSID = "00:00:00:00:00:00"
    }
}

/** Reads the text of a file picked with the system document picker. */
@Singleton
class ContentResolverTextFileReader @Inject constructor(
    @ApplicationContext private val context: Context,
) : TextFileReader {

    override suspend fun read(uri: String, maxBytes: Int): AppResult<String> = withContext(Dispatchers.IO) {
        try {
            val stream = context.contentResolver.openInputStream(android.net.Uri.parse(uri))
                ?: return@withContext AppResult.Error(ErrorReason.FILE_UNREADABLE)
            stream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(BUFFER_SIZE)
                var total = 0
                while (true) {
                    val count = input.read(buffer)
                    if (count < 0) break
                    total += count
                    if (total > maxBytes) return@withContext AppResult.Error(ErrorReason.FILE_UNREADABLE)
                    output.write(buffer, 0, count)
                }
                AppResult.Ok(String(output.toByteArray(), Charsets.UTF_8))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            AppResult.Error(ErrorReason.FILE_UNREADABLE)
        } catch (e: SecurityException) {
            AppResult.Error(ErrorReason.FILE_UNREADABLE)
        }
    }

    private companion object {
        const val BUFFER_SIZE = 8192
    }
}
