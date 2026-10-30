package com.jyhs.educheck.domain.model

/** Single campus: geofence (center + radius), school Wi-Fi and tolerances (config/campus). */
data class CampusConfig(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val toleranceMeters: Int,
    val toleranceMinutes: Int,
    val timeZone: String,
    val bssids: List<String>,
)

data class GeoFix(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float,
)
