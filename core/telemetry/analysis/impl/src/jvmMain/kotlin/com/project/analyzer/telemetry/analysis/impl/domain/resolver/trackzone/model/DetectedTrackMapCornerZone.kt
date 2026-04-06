package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.model

import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorCornerMinimumArcLengthMeters
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorCornerMinimumHeadingDeltaDeg
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorCornerMinimumPeakCurvature
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorCornerMinimumTurnDensityDegPerMeter
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.detectorCornerStrongHeadingDeltaDeg

internal data class DetectedTrackMapCornerZone(
    val startTrackPosition: Float,
    val endTrackPosition: Float,
    val apexTrackPosition: Float,
    val peakCurvature: Float,
    val arcLengthMeters: Float,
    val headingDeltaDeg: Float,
    val orderingTrackPosition: Float,
) {

    fun qualifies(): Boolean {
        val turnDensityDegPerMeter = headingDeltaDeg / arcLengthMeters.coerceAtLeast(1f)
        return arcLengthMeters >= detectorCornerMinimumArcLengthMeters &&
            headingDeltaDeg >= detectorCornerMinimumHeadingDeltaDeg &&
            peakCurvature >= detectorCornerMinimumPeakCurvature &&
            (
                turnDensityDegPerMeter >= detectorCornerMinimumTurnDensityDegPerMeter ||
                    headingDeltaDeg >= detectorCornerStrongHeadingDeltaDeg
                )
    }

    fun toZone(cornerNumber: Int): TrackMapCornerZone = TrackMapCornerZone(
        cornerNumber = cornerNumber,
        startTrackPosition = startTrackPosition,
        endTrackPosition = endTrackPosition,
        apexTrackPosition = apexTrackPosition,
        peakCurvature = peakCurvature,
    )
}
