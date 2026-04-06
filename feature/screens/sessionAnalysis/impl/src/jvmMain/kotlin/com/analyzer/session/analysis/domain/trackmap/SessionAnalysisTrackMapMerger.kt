package com.analyzer.session.analysis.domain.trackmap

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.collections.immutable.toImmutableList

/**
 * Merges recorded telemetry traces with authored map assets into one display-ready track representation.
 */
@Inject
@SingleIn(ScreenScope::class)
internal class SessionAnalysisTrackMapMerger(private val geometry: SessionAnalysisTrackMapGeometry) {

    fun merge(
        authoredTrackMap: SessionAnalysisTrackMap?,
        telemetryTrackMap: SessionAnalysisTrackMap?,
    ): SessionAnalysisTrackMap? {
        if (authoredTrackMap == null) return telemetryTrackMap
        if (telemetryTrackMap == null) return authoredTrackMap

        return when {
            authoredTrackMap.points.size >= 2 -> mergeAuthoredOverlay(
                telemetryTrackMap = telemetryTrackMap,
                authoredTrackMap = authoredTrackMap,
            ) ?: authoredTrackMap

            telemetryTrackMap.points.size >= 2 -> telemetryTrackMap

            else -> authoredTrackMap
        }
    }

    private fun mergeAuthoredOverlay(
        telemetryTrackMap: SessionAnalysisTrackMap,
        authoredTrackMap: SessionAnalysisTrackMap,
    ): SessionAnalysisTrackMap? {
        val transformedAuthored = geometry.transform(
            trackMap = authoredTrackMap,
            targetCenterLine = telemetryTrackMap.points,
        )
        if (transformedAuthored != null) {
            val alignedPoints = alignLoopStartToTelemetry(
                points = transformedAuthored.points,
                telemetryTrackMap = telemetryTrackMap,
            )
            val mergedPoints = alignedPoints.mapIndexed { index, point ->
                val fraction = if (alignedPoints.lastIndex <= 0) {
                    0f
                } else {
                    index.toFloat() / alignedPoints.lastIndex.toFloat()
                }
                val telemetryWidths = geometry.sampleWidths(telemetryTrackMap.points, fraction)
                SessionAnalysisTrackMapPoint(
                    x = point.x,
                    y = point.y,
                    leftWidthMeters = point.leftWidthMeters ?: telemetryWidths?.first,
                    rightWidthMeters = point.rightWidthMeters ?: telemetryWidths?.second,
                )
            }
            return buildTrackMap(
                points = mergedPoints,
                pitPoints = transformedAuthored.pitPoints.takeIf { it.size >= 2 }.orEmpty(),
                idealPoints = resolveMergedIdealPoints(
                    primaryIdealPoints = transformedAuthored.idealPoints,
                    fallbackIdealPoints = telemetryTrackMap.idealPoints,
                    mergedCenterLine = mergedPoints,
                ),
            )
        }

        val mergedPoints = telemetryTrackMap.points.mapIndexed { index, point ->
            val fraction = if (telemetryTrackMap.points.lastIndex <= 0) {
                0f
            } else {
                index.toFloat() / telemetryTrackMap.points.lastIndex.toFloat()
            }
            val authoredWidths = geometry.sampleWidths(authoredTrackMap.points, fraction)
            SessionAnalysisTrackMapPoint(
                x = point.x,
                y = point.y,
                leftWidthMeters = point.leftWidthMeters ?: authoredWidths?.first,
                rightWidthMeters = point.rightWidthMeters ?: authoredWidths?.second,
            )
        }
        val mergedIdealPoints = when {
            authoredTrackMap.idealPoints.size >= 2 -> resolveMergedIdealPoints(
                primaryIdealPoints = geometry.projectOverlay(
                    overlayPoints = authoredTrackMap.idealPoints,
                    sourceCenterLine = telemetryTrackMap.points,
                    authoredCenterLine = authoredTrackMap.points,
                ).ifEmpty {
                    geometry.transformOverlay(
                        overlayPoints = authoredTrackMap.idealPoints,
                        sourceCenterLine = authoredTrackMap.points,
                        targetCenterLine = telemetryTrackMap.points,
                    )
                },
                fallbackIdealPoints = telemetryTrackMap.idealPoints,
                mergedCenterLine = telemetryTrackMap.points,
            )

            telemetryTrackMap.idealPoints.size >= 2 -> telemetryTrackMap.idealPoints

            else -> emptyList()
        }
        val mergedPitPoints = when {
            authoredTrackMap.pitPoints.size >= 2 -> geometry.projectOverlay(
                overlayPoints = authoredTrackMap.pitPoints,
                sourceCenterLine = telemetryTrackMap.points,
                authoredCenterLine = authoredTrackMap.points,
            ).ifEmpty {
                geometry.transformOverlay(
                    overlayPoints = authoredTrackMap.pitPoints,
                    sourceCenterLine = authoredTrackMap.points,
                    targetCenterLine = telemetryTrackMap.points,
                )
            }.ifEmpty { telemetryTrackMap.pitPoints }

            telemetryTrackMap.pitPoints.size >= 2 -> telemetryTrackMap.pitPoints

            else -> emptyList()
        }
        return buildTrackMap(
            points = mergedPoints,
            pitPoints = mergedPitPoints,
            idealPoints = mergedIdealPoints,
        )
    }

    private fun buildTrackMap(
        points: List<SessionAnalysisTrackMapPoint>,
        pitPoints: List<SessionAnalysisTrackMapPoint>,
        idealPoints: List<SessionAnalysisTrackMapPoint>,
    ): SessionAnalysisTrackMap? {
        val allPoints = points + pitPoints + idealPoints
        if (allPoints.size < 2) return null

        val minX = allPoints.minOf(SessionAnalysisTrackMapPoint::x)
        val minY = allPoints.minOf(SessionAnalysisTrackMapPoint::y)
        val maxX = allPoints.maxOf(SessionAnalysisTrackMapPoint::x)
        val maxY = allPoints.maxOf(SessionAnalysisTrackMapPoint::y)
        if (maxX <= minX || maxY <= minY) return null

        return SessionAnalysisTrackMap(
            points = points.toImmutableList(),
            pitPoints = pitPoints.toImmutableList(),
            idealPoints = idealPoints.toImmutableList(),
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
        )
    }

    private fun resolveMergedIdealPoints(
        primaryIdealPoints: List<SessionAnalysisTrackMapPoint>,
        fallbackIdealPoints: List<SessionAnalysisTrackMapPoint>,
        mergedCenterLine: List<SessionAnalysisTrackMapPoint>,
    ): List<SessionAnalysisTrackMapPoint> {
        val resolvedPrimary = primaryIdealPoints.takeIf { it.size >= 2 }
        val resolvedFallback = fallbackIdealPoints.takeIf { it.size >= 2 }
        if (resolvedPrimary != null && geometry.isPlausibleOverlay(resolvedPrimary, mergedCenterLine)) {
            return resolvedPrimary
        }
        if (resolvedFallback != null && geometry.isPlausibleOverlay(resolvedFallback, mergedCenterLine)) {
            return resolvedFallback
        }
        return resolvedPrimary ?: resolvedFallback ?: emptyList()
    }

    private fun alignLoopStartToTelemetry(
        points: List<SessionAnalysisTrackMapPoint>,
        telemetryTrackMap: SessionAnalysisTrackMap,
    ): List<SessionAnalysisTrackMapPoint> {
        if (points.size < 4) return points
        val referencePoint = telemetryTrackMap.points.firstOrNull() ?: return points
        val closureThreshold = points.resolveClosureThreshold()
        val isClosedLoop = points.distanceBetween(points.first(), points.last()) <= closureThreshold
        if (!isClosedLoop) return points

        val hasExplicitClosure = points.lastIndex > 0 &&
            points.distanceBetween(points.first(), points.last()) <= closureThreshold * 0.55f
        val basePoints = if (hasExplicitClosure) points.dropLast(1) else points
        if (basePoints.size < 3) return points

        val startIndex = basePoints.indices.minByOrNull { index ->
            squaredDistance(basePoints[index], referencePoint)
        } ?: return points
        if (startIndex == 0) return points

        val rotated = buildList(basePoints.size) {
            addAll(basePoints.drop(startIndex))
            addAll(basePoints.take(startIndex))
        }
        return if (hasExplicitClosure) rotated + rotated.first() else rotated
    }
}
