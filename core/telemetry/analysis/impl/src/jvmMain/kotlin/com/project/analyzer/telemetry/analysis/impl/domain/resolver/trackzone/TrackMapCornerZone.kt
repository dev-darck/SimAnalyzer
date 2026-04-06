package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone

import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.normalizeTrackPosition
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.extension.trackPositionDistance

internal data class TrackMapCornerZone(
    val cornerNumber: Int,
    val startTrackPosition: Float,
    val endTrackPosition: Float,
    val apexTrackPosition: Float,
    val peakCurvature: Float,
) {

    val wrapsAroundStartFinish: Boolean
        get() = endTrackPosition < startTrackPosition

    fun contains(trackPosition: Float): Boolean {
        val normalizedTrackPosition = trackPosition.normalizeTrackPosition()
        val normalizedStart = startTrackPosition.normalizeTrackPosition()
        val normalizedEnd = endTrackPosition.normalizeTrackPosition()
        return if (wrapsAroundStartFinish) {
            normalizedTrackPosition >= normalizedStart || normalizedTrackPosition <= normalizedEnd
        } else {
            normalizedTrackPosition in normalizedStart..normalizedEnd
        }
    }

    fun spanFraction(): Float = trackPositionDistance(startTrackPosition, endTrackPosition)
}
