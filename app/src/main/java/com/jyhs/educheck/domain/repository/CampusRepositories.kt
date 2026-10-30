package com.jyhs.educheck.domain.repository

import com.jyhs.educheck.core.common.AppResult
import com.jyhs.educheck.domain.model.CampusConfig
import com.jyhs.educheck.domain.model.GeoFix
import com.jyhs.educheck.domain.model.GradeRecord

interface CampusRepository {
    /** The configuration, or null if the administrator has not saved it yet. */
    suspend fun getCampus(): AppResult<CampusConfig?>

    suspend fun saveCampus(config: CampusConfig): AppResult<Unit>
}

interface GradeRepository {
    /** Writes (replaces) `grades/{assignmentId}_{studentId}` for every record. */
    suspend fun saveGrades(records: List<GradeRecord>): AppResult<Unit>
}

/** Device GPS. Needs the location permission, which the screen asks for. */
interface LocationProvider {
    suspend fun currentLocation(): AppResult<GeoFix>
}

/** Wi-Fi the phone is connected to. Needs the location permission and location services on. */
interface WifiProvider {
    /** BSSID in upper case (AA:BB:CC:DD:EE:FF), or null if not connected or not readable. */
    fun currentBssid(): String?
}

/** Reads a text file the user picked (content URI as a string). */
interface TextFileReader {
    suspend fun read(uri: String, maxBytes: Int): AppResult<String>
}
