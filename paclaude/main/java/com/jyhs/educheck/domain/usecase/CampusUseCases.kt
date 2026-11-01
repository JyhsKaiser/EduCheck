package com.jyhs.educheck.domain.usecase

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.core.common.ErrorReason
import com.jyhs.educheck.domain.model.CampusConfig
import com.jyhs.educheck.domain.model.GeoFix
import com.jyhs.educheck.domain.repository.CampusRepository
import com.jyhs.educheck.domain.repository.LocationProvider
import com.jyhs.educheck.domain.repository.WifiProvider
import java.time.ZoneId
import javax.inject.Inject

class GetCampusUseCase @Inject constructor(private val repository: CampusRepository) {
    suspend operator fun invoke(): AppResult<CampusConfig?> = repository.getCampus()
}

class SaveCampusUseCase @Inject constructor(private val repository: CampusRepository) {
    suspend operator fun invoke(config: CampusConfig): AppResult<Unit> {
        val bssids = config.bssids.map { it.trim().uppercase() }.distinct()
        val valid = config.name.isNotBlank() &&
            config.latitude in -90.0..90.0 && config.longitude in -180.0..180.0 &&
            config.radiusMeters in MIN_RADIUS..MAX_RADIUS &&
            config.toleranceMeters in 0..MAX_TOLERANCE_METERS &&
            config.toleranceMinutes in 0..MAX_TOLERANCE_MINUTES &&
            isValidZone(config.timeZone) &&
            bssids.all { BSSID.matches(it) }
        if (!valid) return AppResult.Error(ErrorReason.INVALID_INPUT)
        return repository.saveCampus(config.copy(name = config.name.trim(), timeZone = config.timeZone.trim(), bssids = bssids))
    }

    private fun isValidZone(zone: String): Boolean =
        try {
            ZoneId.of(zone.trim())
            true
        } catch (_: Exception) {
            false
        }

    companion object {
        const val MIN_RADIUS = 20
        const val MAX_RADIUS = 5000
        const val MAX_TOLERANCE_METERS = 1000
        const val MAX_TOLERANCE_MINUTES = 240

        /** Format the Firestore rules compare against: upper case, colon separated. */
        val BSSID = Regex("^([0-9A-F]{2}:){5}[0-9A-F]{2}$")
    }
}

class GetCurrentLocationUseCase @Inject constructor(private val provider: LocationProvider) {
    suspend operator fun invoke(): AppResult<GeoFix> = provider.currentLocation()
}

class GetCurrentBssidUseCase @Inject constructor(private val provider: WifiProvider) {
    operator fun invoke(): AppResult<String> {
        val bssid = provider.currentBssid()
        return if (bssid != null && SaveCampusUseCase.BSSID.matches(bssid)) {
            AppResult.Ok(bssid)
        } else {
            AppResult.Error(ErrorReason.WIFI_UNAVAILABLE)
        }
    }
}
