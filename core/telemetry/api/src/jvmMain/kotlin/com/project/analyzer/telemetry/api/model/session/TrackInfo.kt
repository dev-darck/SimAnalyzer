package com.project.analyzer.telemetry.api.model.session

public data class TrackInfo(
    val trackId: String? = null, // Internal track ID
    val trackName: String? = null, // Display name
    val layoutId: String? = null, // Track configuration/layout
    val sectorCount: Int? = null,

    val lengthMeters: Float? = null, // trackSplineLength

    val normalizedLapPosition: Float? = null, // 0..1 position on track
    val distanceTraveled: Float? = null, // meters

    val worldXyz: FloatArray? = null, // [x, y, z] current world position

    val trackStatus: String? = null, // Track status string
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TrackInfo) return false
        if (worldXyz != null) {
            if (other.worldXyz == null) return false
            if (!worldXyz.contentEquals(other.worldXyz)) return false
        } else if (other.worldXyz != null) return false
        return trackId == other.trackId &&
            trackName == other.trackName &&
            layoutId == other.layoutId &&
            sectorCount == other.sectorCount &&
            lengthMeters == other.lengthMeters &&
            normalizedLapPosition == other.normalizedLapPosition &&
            distanceTraveled == other.distanceTraveled &&
            trackStatus == other.trackStatus
    }

    override fun hashCode(): Int {
        var result = trackId?.hashCode() ?: 0
        result = 31 * result + (trackName?.hashCode() ?: 0)
        result = 31 * result + (layoutId?.hashCode() ?: 0)
        result = 31 * result + (sectorCount ?: 0)
        result = 31 * result + (lengthMeters?.hashCode() ?: 0)
        result = 31 * result + (normalizedLapPosition?.hashCode() ?: 0)
        result = 31 * result + (distanceTraveled?.hashCode() ?: 0)
        result = 31 * result + (worldXyz?.contentHashCode() ?: 0)
        result = 31 * result + (trackStatus?.hashCode() ?: 0)
        return result
    }
}
