package com.analyzer.session.analysis.domain.trackmap

import com.analyzer.session.analysis.domain.trackmap.extension.halfWidthMeters
import com.analyzer.session.analysis.domain.trackmap.extension.nearestPathProjectionTo
import com.analyzer.session.analysis.domain.trackmap.extension.polylineLengthMeters
import com.analyzer.session.analysis.domain.trackmap.extension.sampleHalfWidthAt
import com.analyzer.session.analysis.domain.trackmap.extension.sampleNormalAtPath
import com.analyzer.session.analysis.domain.trackmap.extension.samplePositionAt
import com.analyzer.session.analysis.domain.trackmap.extension.samplePositionAtPath
import com.analyzer.session.analysis.domain.trackmap.extension.withPathFractions
import com.analyzer.session.analysis.domain.trackmap.model.SimilarityTransform
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.math.sqrt

/**
 * Aligns authored and recorded track geometry so every map overlay, cursor lookup, and trace builder
 * can work in one consistent coordinate space.
 */
@Inject
@SingleIn(ScreenScope::class)
internal class SessionAnalysisTrackMapGeometry {

    /**
     * Transforms an authored track map into the current telemetry center line while rejecting implausible fits.
     */
    fun transform(
        trackMap: SessionAnalysisTrackMap,
        targetCenterLine: List<SessionAnalysisTrackMapPoint>,
    ): SessionAnalysisTrackMap? {
        val transform = resolveTrackPointTransform(
            sourcePoints = trackMap.points,
            targetPoints = targetCenterLine,
        ) ?: return null

        val transformedPoints = trackMap.points.map(transform::mapPoint)
        if (!isPlausibleAgainst(transformedPoints, targetCenterLine)) return null

        return SessionAnalysisTrackMap(
            points = transformedPoints,
            pitPoints = trackMap.pitPoints.map(transform::mapPoint),
            idealPoints = trackMap.idealPoints.map(transform::mapPoint),
            minX = transformedPoints.minOf(SessionAnalysisTrackMapPoint::x),
            minY = transformedPoints.minOf(SessionAnalysisTrackMapPoint::y),
            maxX = transformedPoints.maxOf(SessionAnalysisTrackMapPoint::x),
            maxY = transformedPoints.maxOf(SessionAnalysisTrackMapPoint::y),
        )
    }

    fun sampleWidths(points: List<SessionAnalysisTrackMapPoint>, fraction: Float): Pair<Float, Float>? {
        if (points.isEmpty()) return null
        val clampedFraction = fraction.coerceIn(0f, 1f)
        val scaledIndex = clampedFraction * points.lastIndex.coerceAtLeast(1).toFloat()
        val lowerIndex = scaledIndex.toInt().coerceIn(0, points.lastIndex)
        val upperIndex = (lowerIndex + 1).coerceAtMost(points.lastIndex)
        val localFraction = (scaledIndex - lowerIndex.toFloat()).coerceIn(0f, 1f)
        val lowerLeft = points[lowerIndex].leftWidthMeters ?: points[upperIndex].leftWidthMeters
        val upperLeft = points[upperIndex].leftWidthMeters ?: lowerLeft
        val lowerRight = points[lowerIndex].rightWidthMeters ?: points[upperIndex].rightWidthMeters
        val upperRight = points[upperIndex].rightWidthMeters ?: lowerRight
        if (lowerLeft == null || upperLeft == null || lowerRight == null || upperRight == null) return null
        return lerp(lowerLeft, upperLeft, localFraction) to lerp(lowerRight, upperRight, localFraction)
    }

    fun isPlausibleOverlay(
        overlayPoints: List<SessionAnalysisTrackMapPoint>,
        sourceCenterLine: List<SessionAnalysisTrackMapPoint>,
        sourceHalfWidthMeters: Float = medianHalfWidth(sourceCenterLine) ?: 8f,
    ): Boolean {
        if (overlayPoints.size < 2 || sourceCenterLine.size < 2) return false

        val overlayLengthMeters = overlayPoints.polylineLengthMeters()
        val sourceLengthMeters = sourceCenterLine.polylineLengthMeters()
        if (!overlayLengthMeters.isFinite() || !sourceLengthMeters.isFinite() || sourceLengthMeters <= 0.1f) {
            return false
        }

        val lengthRatio = overlayLengthMeters / sourceLengthMeters
        val minimumOverlayLengthMeters = maxOf(sourceHalfWidthMeters.coerceAtLeast(6f) * 2.2f, 14f)
        if (overlayLengthMeters < minimumOverlayLengthMeters) return false
        if (lengthRatio > 1.35f) return false

        val sourcePathPoints = sourceCenterLine.withPathFractions()
        val allowedOffsetMeters = sourceHalfWidthMeters.coerceAtLeast(6f) * 1.9f
        val sampleCount = minOf(48, maxOf(18, minOf(overlayPoints.size, sourceCenterLine.size)))
        var outlierCount = 0
        repeat(sampleCount) { index ->
            val fraction = if (sampleCount <= 1) 0f else index.toFloat() / (sampleCount - 1).toFloat()
            val overlayPosition = overlayPoints.samplePositionAt(fraction)
            val nearestSourceMatch = sourcePathPoints.nearestPathProjectionTo(
                x = overlayPosition.first,
                y = overlayPosition.second,
                preferredFraction = null,
                searchWindowFraction = 1f,
            )
            if (nearestSourceMatch == null || sqrt(nearestSourceMatch.distanceSquared) > allowedOffsetMeters) {
                outlierCount += 1
            }
        }
        return outlierCount <= sampleCount / 5
    }

    fun projectOverlay(
        overlayPoints: List<SessionAnalysisTrackMapPoint>,
        sourceCenterLine: List<SessionAnalysisTrackMapPoint>,
        authoredCenterLine: List<SessionAnalysisTrackMapPoint>,
    ): List<SessionAnalysisTrackMapPoint> {
        if (overlayPoints.size < 2 || sourceCenterLine.size < 2 || authoredCenterLine.size < 2) {
            return emptyList()
        }

        val authoredMedianHalfWidth = medianHalfWidth(authoredCenterLine) ?: 8f
        val sourceMedianHalfWidth = medianHalfWidth(sourceCenterLine) ?: authoredMedianHalfWidth
        val authoredPathPoints = authoredCenterLine.withPathFractions()
        val sourcePathPoints = sourceCenterLine.withPathFractions()
        val projected = overlayPoints.mapIndexedNotNull { index, overlayPoint ->
            val baselineFraction =
                if (overlayPoints.lastIndex <= 0) 0f else index.toFloat() / overlayPoints.lastIndex.toFloat()
            val fraction = resolveOverlayProjectionFraction(
                overlayPoint = overlayPoint,
                authoredPathPoints = authoredPathPoints,
                authoredMedianHalfWidth = authoredMedianHalfWidth,
                baselineFraction = baselineFraction,
            )
            val authoredCenter = authoredPathPoints.samplePositionAtPath(fraction)
            val authoredNormal = authoredPathPoints.sampleNormalAtPath(fraction) ?: return@mapIndexedNotNull null
            val sourceCenter = sourcePathPoints.samplePositionAtPath(fraction)
            val sourceNormal = sourcePathPoints.sampleNormalAtPath(fraction) ?: return@mapIndexedNotNull null
            val authoredHalfWidth = authoredPathPoints.sampleHalfWidthAt(fraction) ?: authoredMedianHalfWidth
            val sourceHalfWidth = sourcePathPoints.sampleHalfWidthAt(fraction) ?: sourceMedianHalfWidth

            val deltaX = overlayPoint.x - authoredCenter.first
            val deltaY = overlayPoint.y - authoredCenter.second
            val authoredOffset = deltaX * authoredNormal.first + deltaY * authoredNormal.second
            val scaledOffset = if (authoredHalfWidth > 0.001f) {
                authoredOffset * (sourceHalfWidth / authoredHalfWidth)
            } else {
                authoredOffset
            }

            SessionAnalysisTrackMapPoint(
                x = sourceCenter.first + sourceNormal.first * scaledOffset,
                y = sourceCenter.second + sourceNormal.second * scaledOffset,
            )
        }
        return projected.takeIf {
            isPlausibleOverlay(
                overlayPoints = it,
                sourceCenterLine = sourceCenterLine,
                sourceHalfWidthMeters = sourceMedianHalfWidth,
            )
        }.orEmpty()
    }

    fun transformOverlay(
        overlayPoints: List<SessionAnalysisTrackMapPoint>,
        sourceCenterLine: List<SessionAnalysisTrackMapPoint>,
        targetCenterLine: List<SessionAnalysisTrackMapPoint>,
    ): List<SessionAnalysisTrackMapPoint> {
        if (overlayPoints.size < 2 || sourceCenterLine.size < 2 || targetCenterLine.size < 2) {
            return emptyList()
        }

        val transform = resolveTrackPointTransform(
            sourcePoints = sourceCenterLine,
            targetPoints = targetCenterLine,
        ) ?: return emptyList()
        val transformedSourceCenterLine = sourceCenterLine.map(transform::mapPoint)
        if (!isPlausibleAgainst(transformedSourceCenterLine, targetCenterLine)) return emptyList()
        return overlayPoints.map(transform::mapPoint)
    }

    fun medianHalfWidth(points: List<SessionAnalysisTrackMapPoint>): Float? {
        val widths = points.mapNotNull(SessionAnalysisTrackMapPoint::halfWidthMeters)
            .filter { width -> width.isFinite() && width > 0.05f }
            .sorted()
        if (widths.isEmpty()) return null
        val middle = widths.size / 2
        return if (widths.size % 2 == 0) {
            (widths[middle - 1] + widths[middle]) * 0.5f
        } else {
            widths[middle]
        }
    }

    fun sampleHalfWidth(points: List<SessionAnalysisTrackMapPoint>, fraction: Float): Float? {
        if (points.isEmpty()) return null
        val clampedFraction = fraction.coerceIn(0f, 1f)
        val scaledIndex = clampedFraction * points.lastIndex.coerceAtLeast(1).toFloat()
        val lowerIndex = scaledIndex.toInt().coerceIn(0, points.lastIndex)
        val upperIndex = (lowerIndex + 1).coerceAtMost(points.lastIndex)
        val localFraction = (scaledIndex - lowerIndex.toFloat()).coerceIn(0f, 1f)
        val lowerWidth = points[lowerIndex].halfWidthMeters() ?: return points[upperIndex].halfWidthMeters()
        val upperWidth = points[upperIndex].halfWidthMeters() ?: lowerWidth
        return lerp(lowerWidth, upperWidth, localFraction)
    }

    private fun resolveOverlayProjectionFraction(
        overlayPoint: SessionAnalysisTrackMapPoint,
        authoredPathPoints: List<com.analyzer.session.analysis.domain.trackmap.model.TrackPathPoint>,
        authoredMedianHalfWidth: Float,
        baselineFraction: Float,
    ): Float {
        val localMatch = authoredPathPoints.nearestPathProjectionTo(
            x = overlayPoint.x,
            y = overlayPoint.y,
            preferredFraction = baselineFraction,
            searchWindowFraction = 0.08f,
        )
        if (
            localMatch != null &&
            isUsableOverlayProjectionMatch(
                overlayPoint = overlayPoint,
                authoredPathPoints = authoredPathPoints,
                fraction = localMatch.fraction,
                authoredMedianHalfWidth = authoredMedianHalfWidth,
            )
        ) {
            return localMatch.fraction
        }

        return authoredPathPoints.nearestPathProjectionTo(
            x = overlayPoint.x,
            y = overlayPoint.y,
            preferredFraction = null,
            searchWindowFraction = 1f,
        )?.fraction ?: localMatch?.fraction ?: baselineFraction
    }

    private fun isUsableOverlayProjectionMatch(
        overlayPoint: SessionAnalysisTrackMapPoint,
        authoredPathPoints: List<com.analyzer.session.analysis.domain.trackmap.model.TrackPathPoint>,
        fraction: Float,
        authoredMedianHalfWidth: Float,
    ): Boolean {
        val authoredCenter = authoredPathPoints.samplePositionAtPath(fraction)
        val dx = overlayPoint.x - authoredCenter.first
        val dy = overlayPoint.y - authoredCenter.second
        val distanceToCenter = sqrt(dx * dx + dy * dy)
        val halfWidth = authoredPathPoints.sampleHalfWidthAt(fraction) ?: authoredMedianHalfWidth
        val maxDistanceToCenter = maxOf(halfWidth * 2.4f, authoredMedianHalfWidth * 1.8f, 8f)
        return distanceToCenter.isFinite() && distanceToCenter <= maxDistanceToCenter
    }

    private fun isPlausibleAgainst(
        sourcePoints: List<SessionAnalysisTrackMapPoint>,
        targetCenterLine: List<SessionAnalysisTrackMapPoint>,
    ): Boolean {
        if (sourcePoints.size < 2 || targetCenterLine.size < 2) return false

        val sampleCount = minOf(72, maxOf(24, minOf(sourcePoints.size, targetCenterLine.size)))
        var errorSum = 0f
        repeat(sampleCount) { index ->
            val fraction = index.toFloat() / sampleCount.toFloat()
            val sourcePoint = sourcePoints.samplePositionAt(fraction)
            val targetPoint = targetCenterLine.samplePositionAt(fraction)
            val dx = sourcePoint.first - targetPoint.first
            val dy = sourcePoint.second - targetPoint.second
            errorSum += sqrt(dx * dx + dy * dy)
        }
        val avgErrorMeters = errorSum / sampleCount.toFloat()
        val targetHalfWidth = medianHalfWidth(targetCenterLine) ?: 8f
        return avgErrorMeters.isFinite() && avgErrorMeters <= maxOf(10f, targetHalfWidth * 1.2f)
    }

    private fun resolveTrackPointTransform(
        sourcePoints: List<SessionAnalysisTrackMapPoint>,
        targetPoints: List<SessionAnalysisTrackMapPoint>,
    ): SimilarityTransform? {
        if (sourcePoints.size < 2 || targetPoints.size < 2) return null

        val pairCount = minOf(96, maxOf(24, minOf(sourcePoints.size, targetPoints.size)))
        val sourcePairs = ArrayList<Pair<Float, Float>>(pairCount)
        repeat(pairCount) { index ->
            val fraction = index.toFloat() / pairCount.toFloat()
            sourcePairs += sourcePoints.samplePositionAt(fraction)
        }

        var bestTransform: SimilarityTransform? = null
        var bestError = Float.POSITIVE_INFINITY
        repeat(pairCount) { shiftIndex ->
            val shift = shiftIndex.toFloat() / pairCount.toFloat()
            val targetPairs = ArrayList<Pair<Float, Float>>(pairCount)
            repeat(pairCount) { index ->
                val fraction = (index.toFloat() / pairCount.toFloat() + shift) % 1f
                targetPairs += targetPoints.samplePositionAt(fraction)
            }
            val candidate = resolvePairTransform(sourcePairs, targetPairs) ?: return@repeat
            val candidateError = estimateError(candidate, sourcePairs, targetPairs)
            if (candidateError < bestError) {
                bestError = candidateError
                bestTransform = candidate
            }
        }
        return bestTransform
    }

    private fun resolvePairTransform(
        sourcePairs: List<Pair<Float, Float>>,
        targetPairs: List<Pair<Float, Float>>,
    ): SimilarityTransform? {
        val sourceCenterX = sourcePairs.sumOf { it.first.toDouble() }.toFloat() / sourcePairs.size.toFloat()
        val sourceCenterY = sourcePairs.sumOf { it.second.toDouble() }.toFloat() / sourcePairs.size.toFloat()
        val targetCenterX = targetPairs.sumOf { it.first.toDouble() }.toFloat() / targetPairs.size.toFloat()
        val targetCenterY = targetPairs.sumOf { it.second.toDouble() }.toFloat() / targetPairs.size.toFloat()

        var covarianceA = 0f
        var covarianceB = 0f
        var sourceNorm = 0f
        sourcePairs.indices.forEach { index ->
            val sourceX = sourcePairs[index].first - sourceCenterX
            val sourceY = sourcePairs[index].second - sourceCenterY
            val targetX = targetPairs[index].first - targetCenterX
            val targetY = targetPairs[index].second - targetCenterY
            covarianceA += sourceX * targetX + sourceY * targetY
            covarianceB += sourceX * targetY - sourceY * targetX
            sourceNorm += sourceX * sourceX + sourceY * sourceY
        }

        if (!sourceNorm.isFinite() || sourceNorm <= 0.0001f) return null
        val covarianceNorm = sqrt(covarianceA * covarianceA + covarianceB * covarianceB)
        if (!covarianceNorm.isFinite() || covarianceNorm <= 0.0001f) return null

        val scale = covarianceNorm / sourceNorm
        val rotationCos = covarianceA / covarianceNorm
        val rotationSin = covarianceB / covarianceNorm
        if (!scale.isFinite() || scale <= 0.0001f) return null
        if (!rotationCos.isFinite() || !rotationSin.isFinite()) return null

        val rotatedCenterX = rotationCos * sourceCenterX - rotationSin * sourceCenterY
        val rotatedCenterY = rotationSin * sourceCenterX + rotationCos * sourceCenterY
        return SimilarityTransform(
            scale = scale,
            rotationCos = rotationCos,
            rotationSin = rotationSin,
            translateX = targetCenterX - rotatedCenterX * scale,
            translateY = targetCenterY - rotatedCenterY * scale,
        )
    }

    private fun estimateError(
        transform: SimilarityTransform,
        sourcePairs: List<Pair<Float, Float>>,
        targetPairs: List<Pair<Float, Float>>,
    ): Float {
        var error = 0f
        sourcePairs.indices.forEach { index ->
            val mapped = transform.map(
                x = sourcePairs[index].first,
                y = sourcePairs[index].second,
            )
            val dx = mapped.first - targetPairs[index].first
            val dy = mapped.second - targetPairs[index].second
            error += dx * dx + dy * dy
        }
        return error / sourcePairs.size.toFloat()
    }
}
