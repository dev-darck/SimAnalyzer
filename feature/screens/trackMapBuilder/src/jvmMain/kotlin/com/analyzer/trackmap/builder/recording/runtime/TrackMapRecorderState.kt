package com.analyzer.trackmap.builder.recording.runtime

import com.analyzer.trackmap.domain.model.TrackMapSectorMarker
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds

internal data class TrackMapRecorderState(
    val recording: Boolean = false,
    val isSaving: Boolean = false,
    val gameId: String = "",
    val gameLabel: String = "",
    val trackId: String = "",
    val trackName: String = "",
    val layoutId: String? = null,
    val referencePoint: ReferencePoint = ReferencePoint.FRONT_AXLE,
    val points: List<Vec2> = emptyList(),
    val leftWidthsMeters: List<Float> = emptyList(),
    val rightWidthsMeters: List<Float> = emptyList(),
    val pointCount: Int = 0,
    val totalDistanceMeters: Float = 0f,
    val bounds: TrackMapBounds? = null,
    val currentPosition: Vec2? = null,
    val currentSpeedKmh: Float? = null,
    val pitPoints: List<Vec2> = emptyList(),
    val pitPointCount: Int = 0,
    val pitEntryPoint: Vec2? = null,
    val pitExitPoint: Vec2? = null,
    val sectorCount: Int = 0,
    val capturedSectorCount: Int = 0,
    val sectorMarkers: List<TrackMapSectorMarker> = emptyList(),
    val averageTrackWidthMeters: Float = DEFAULT_AVERAGE_TRACK_WIDTH_METERS,
    val leftCoverageRatio: Float = 0f,
    val rightCoverageRatio: Float = 0f,
    val guidanceText: String? = null,
    val minSpacingMeters: Float = DEFAULT_MIN_SPACING_METERS,
    val maxSpacingMeters: Float = DEFAULT_MAX_SPACING_METERS,
    val minAngleDeg: Float = DEFAULT_MIN_ANGLE_DEG,
    val minSpeedKmh: Float = DEFAULT_MIN_SPEED_KMH,
    val fallbackHalfWidthMeters: Float = DEFAULT_FALLBACK_HALF_WIDTH_METERS,
    val lapIndex: Int? = null,
    val lapsRecorded: Int = 0,
    val isInPitLane: Boolean = false,
    val pitOverrideActive: Boolean = false,
    val message: String? = null,
    val lastSavedTrackId: String? = null,
    val lastSavedAtEpochMs: Long? = null,
) {

    companion object {

        const val DEFAULT_MIN_SPACING_METERS = 0.5f
        const val DEFAULT_MAX_SPACING_METERS = 6f
        const val DEFAULT_MIN_ANGLE_DEG = 3f
        const val DEFAULT_MIN_SPEED_KMH = 5f
        const val DEFAULT_FALLBACK_HALF_WIDTH_METERS = 5.5f
        const val DEFAULT_AVERAGE_TRACK_WIDTH_METERS = DEFAULT_FALLBACK_HALF_WIDTH_METERS * 2f
    }
}
