package com.analyzer.session.analysis.presentation.builder.track.trace

import com.analyzer.session.analysis.domain.trackmap.SessionAnalysisTrackMapGeometry
import com.analyzer.session.analysis.presentation.builder.track.support.SessionAnalysisTrackPointSampler
import com.analyzer.session.analysis.presentation.builder.track.support.TrackMapFractionLookup
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Handles geometric projection between telemetry traces and the currently displayed track map.
 */
internal class SessionAnalysisTrackCanvasTraceGeometry(
    private val pointSampler: SessionAnalysisTrackPointSampler,
    private val trackMapGeometry: SessionAnalysisTrackMapGeometry,
) {

    fun shouldProjectBetweenMaps(
        sourceTrackMap: SessionAnalysisTrackMap?,
        displayTrackMap: SessionAnalysisTrackMap?,
    ): Boolean {
        if (sourceTrackMap == null || displayTrackMap == null) return false
        if (sourceTrackMap === displayTrackMap) return false
        return !mapsShareWorldCoordinates(sourceTrackMap, displayTrackMap)
    }

    fun traceAlreadyFitsTrackWorld(
        rawTrace: List<SessionAnalysisTrackMapPoint>,
        displayTrackMap: SessionAnalysisTrackMap,
    ): Boolean {
        if (rawTrace.size < 2 || displayTrackMap.points.size < 2) return false
        val displayLookup = TrackMapFractionLookup.create(displayTrackMap.points) ?: return false
        val sampledTrace = pointSampler.sampleEvenlyByPathDistance(
            points = rawTrace,
            maxPoints = minOf(96, rawTrace.size),
            xSelector = SessionAnalysisTrackMapPoint::x,
            ySelector = SessionAnalysisTrackMapPoint::y,
        )
        if (sampledTrace.size < 2) return false

        val halfWidth = trackMapGeometry.medianHalfWidth(displayTrackMap.points)?.coerceAtLeast(6f) ?: 8f
        val maxAverageDistance = halfWidth * 0.95f
        val maxPeakDistance = halfWidth * 1.75f
        var totalDistance = 0f
        var maxDistance = 0f
        var outlierCount = 0
        sampledTrace.forEach { point ->
            val projection = displayLookup.nearestProjectionTo(
                x = point.x,
                y = point.y,
                preferredFraction = null,
                fractionWindow = null,
            ) ?: return false
            val distance = sqrt(projection.distanceSquared)
            if (!distance.isFinite()) return false
            totalDistance += distance
            maxDistance = maxOf(maxDistance, distance)
            if (distance > maxPeakDistance) outlierCount += 1
        }
        val averageDistance = totalDistance / sampledTrace.size.toFloat()
        return averageDistance <= maxAverageDistance &&
            maxDistance <= maxPeakDistance &&
            outlierCount <= sampledTrace.size / 6
    }

    fun overlayAlreadyFitsTrackWorld(
        overlayPoints: List<SessionAnalysisTrackMapPoint>,
        displayTrackMap: SessionAnalysisTrackMap,
    ): Boolean = trackMapGeometry.isPlausibleOverlay(
        overlayPoints = overlayPoints,
        sourceCenterLine = displayTrackMap.points,
    )

    fun projectPointsByFraction(
        points: List<SessionAnalysisTrackMapPoint>,
        fractions: List<Float>,
        sourceTrackMap: SessionAnalysisTrackMap,
        displayTrackMap: SessionAnalysisTrackMap,
    ): List<SessionAnalysisTrackMapPoint> {
        if (points.size < 2 || points.size != fractions.size) return emptyList()
        val sourceLookup = TrackMapFractionLookup.create(sourceTrackMap.points) ?: return emptyList()
        val displayLookup = TrackMapFractionLookup.create(displayTrackMap.points) ?: return emptyList()
        val sourceMedianHalfWidth = trackMapGeometry.medianHalfWidth(sourceTrackMap.points) ?: 8f
        val displayMedianHalfWidth = trackMapGeometry.medianHalfWidth(displayTrackMap.points) ?: sourceMedianHalfWidth

        return points.mapIndexedNotNull { index, point ->
            val fraction = fractions[index].coerceIn(0f, 1f)
            val sourceCenter = sourceLookup.samplePositionAt(fraction) ?: return@mapIndexedNotNull null
            val sourceNormal = sourceLookup.sampleNormalAt(fraction) ?: return@mapIndexedNotNull null
            val displayCenter = displayLookup.samplePositionAt(fraction) ?: return@mapIndexedNotNull null
            val displayNormal = displayLookup.sampleNormalAt(fraction) ?: return@mapIndexedNotNull null
            val sourceHalfWidth =
                trackMapGeometry.sampleHalfWidth(sourceTrackMap.points, fraction) ?: sourceMedianHalfWidth
            val displayHalfWidth =
                trackMapGeometry.sampleHalfWidth(displayTrackMap.points, fraction) ?: displayMedianHalfWidth
            val scaledOffset = point.resolveProjectedLateralOffset(
                sourceCenter = sourceCenter,
                sourceNormal = sourceNormal,
                sourceHalfWidth = sourceHalfWidth,
                displayHalfWidth = displayHalfWidth,
            )
            SessionAnalysisTrackMapPoint(
                x = displayCenter.first + displayNormal.first * scaledOffset,
                y = displayCenter.second + displayNormal.second * scaledOffset,
            )
        }
    }

    fun resolveMedianHalfWidth(
        primaryTrackMap: SessionAnalysisTrackMap?,
        fallbackTrackMap: SessionAnalysisTrackMap? = null,
    ): Float {
        val points = primaryTrackMap?.points ?: fallbackTrackMap?.points.orEmpty()
        return trackMapGeometry.medianHalfWidth(points) ?: 6f
    }

    private fun mapsShareWorldCoordinates(
        sourceTrackMap: SessionAnalysisTrackMap,
        displayTrackMap: SessionAnalysisTrackMap,
    ): Boolean {
        val sourceLookup = TrackMapFractionLookup.create(sourceTrackMap.points) ?: return false
        val displayLookup = TrackMapFractionLookup.create(displayTrackMap.points) ?: return false
        val sourceHalfWidth = trackMapGeometry.medianHalfWidth(sourceTrackMap.points) ?: 8f
        val displayHalfWidth = trackMapGeometry.medianHalfWidth(displayTrackMap.points) ?: sourceHalfWidth
        val referenceHalfWidth = minOf(sourceHalfWidth, displayHalfWidth).coerceAtLeast(6f)
        val sampleCount = minOf(72, maxOf(24, minOf(sourceTrackMap.points.size, displayTrackMap.points.size)))
        var maxDistance = 0f
        var totalDistance = 0f
        repeat(sampleCount) { index ->
            val fraction = if (sampleCount <= 1) 0f else index.toFloat() / (sampleCount - 1).toFloat()
            val sourcePoint = sourceLookup.samplePositionAt(fraction) ?: return false
            val displayPoint = displayLookup.samplePositionAt(fraction) ?: return false
            val distance = hypot(displayPoint.first - sourcePoint.first, displayPoint.second - sourcePoint.second)
            if (!distance.isFinite()) return false
            maxDistance = maxOf(maxDistance, distance)
            totalDistance += distance
        }
        val averageDistance = totalDistance / sampleCount.toFloat()
        return averageDistance <= referenceHalfWidth * 0.32f &&
            maxDistance <= referenceHalfWidth * 0.72f
    }

    private fun SessionAnalysisTrackMapPoint.resolveProjectedLateralOffset(
        sourceCenter: Pair<Float, Float>,
        sourceNormal: Pair<Float, Float>,
        sourceHalfWidth: Float,
        displayHalfWidth: Float,
    ): Float {
        val deltaX = x - sourceCenter.first
        val deltaY = y - sourceCenter.second
        val sourceOffset = deltaX * sourceNormal.first + deltaY * sourceNormal.second
        return if (sourceHalfWidth > 0.001f) {
            sourceOffset * (displayHalfWidth / sourceHalfWidth)
        } else {
            sourceOffset
        }
    }
}
