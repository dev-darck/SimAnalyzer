package com.analyzer.session.analysis.presentation.builder.track.line

import com.analyzer.session.analysis.presentation.builder.track.TRACK_DEFAULT_HALF_WIDTH_METERS
import com.analyzer.session.analysis.presentation.builder.track.TRACK_DEFAULT_SURFACE_WIDTH_METERS
import com.analyzer.session.analysis.presentation.builder.track.TRACK_EDGE_MAX_POINTS
import com.analyzer.session.analysis.presentation.builder.track.TRACK_MIN_VALID_WIDTH_METERS
import com.analyzer.session.analysis.presentation.builder.track.TrackEdgeGeometry
import com.analyzer.session.analysis.presentation.builder.track.TrackEdgeSide
import com.analyzer.session.analysis.presentation.builder.track.TrackLoopMetrics
import com.analyzer.session.analysis.presentation.builder.track.support.SessionAnalysisTrackPointSampler
import com.analyzer.session.analysis.presentation.model.SessionAnalysisTrackPointUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlin.math.hypot

internal class SessionAnalysisTrackLinePreparer(
    private val pointSampler: SessionAnalysisTrackPointSampler = SessionAnalysisTrackPointSampler(),
) {

    fun prepareTrackLine(
        points: List<SessionAnalysisTrackPointUi>,
        maxPoints: Int,
    ): ImmutableList<SessionAnalysisFractionPointUi> {
        val selectedPoints = pointSampler.sampleEvenlyByPathDistance(
            points = points,
            maxPoints = maxPoints,
            xSelector = SessionAnalysisTrackPointUi::x,
            ySelector = SessionAnalysisTrackPointUi::y,
        )
        if (selectedPoints.size < 2) return persistentListOf()

        val fractions = resolveFractions(selectedPoints)
        return selectedPoints.mapIndexed { index, point ->
            SessionAnalysisFractionPointUi(
                fraction = fractions[index],
                x = point.x,
                y = point.y,
            )
        }.toImmutableList()
    }

    fun buildTrackEdges(
        points: List<SessionAnalysisTrackPointUi>,
    ): Pair<ImmutableList<SessionAnalysisFractionPointUi>, ImmutableList<SessionAnalysisFractionPointUi>> {
        val geometry = toTrackEdgeGeometry(points)
            ?: return persistentListOf<SessionAnalysisFractionPointUi>() to persistentListOf()
        return buildTrackEdge(geometry = geometry, side = TrackEdgeSide.LEFT) to
            buildTrackEdge(geometry = geometry, side = TrackEdgeSide.RIGHT)
    }

    fun resolveSurfaceWidth(points: List<SessionAnalysisTrackPointUi>): Float {
        val widths = points.mapNotNull(::surfaceWidthMeters)
        return pointSampler.median(widths) ?: TRACK_DEFAULT_SURFACE_WIDTH_METERS
    }

    private fun toTrackEdgeGeometry(points: List<SessionAnalysisTrackPointUi>): TrackEdgeGeometry? {
        val selectedPoints = trimLoopClosureDuplicate(
            pointSampler.sampleEvenlyByPathDistance(
                points = points,
                maxPoints = TRACK_EDGE_MAX_POINTS,
                xSelector = SessionAnalysisTrackPointUi::x,
                ySelector = SessionAnalysisTrackPointUi::y,
            ),
        )
        if (selectedPoints.size < 3) return null

        return TrackEdgeGeometry(
            points = selectedPoints,
            fractions = resolveFractions(selectedPoints),
            defaultHalfWidth = resolveMedianHalfWidth(selectedPoints),
            isClosedLoop = isClosedLoop(selectedPoints),
        )
    }

    private fun buildTrackEdge(
        geometry: TrackEdgeGeometry,
        side: TrackEdgeSide,
    ): ImmutableList<SessionAnalysisFractionPointUi> {
        val edgePoints = ArrayList<SessionAnalysisFractionPointUi>(geometry.points.size)
        var lastResolvedNormal = geometry.points.indices
            .asSequence()
            .mapNotNull { index -> normalAt(geometry, index) }
            .firstOrNull()
            ?: return persistentListOf()

        geometry.points.forEachIndexed { index, point ->
            val normal = normalAt(geometry, index) ?: lastResolvedNormal
            lastResolvedNormal = normal
            val width = when (side) {
                TrackEdgeSide.LEFT -> validLeftWidth(point) ?: geometry.defaultHalfWidth
                TrackEdgeSide.RIGHT -> validRightWidth(point) ?: geometry.defaultHalfWidth
            }
            val direction = when (side) {
                TrackEdgeSide.LEFT -> 1f
                TrackEdgeSide.RIGHT -> -1f
            }

            edgePoints += SessionAnalysisFractionPointUi(
                fraction = geometry.fractions[index],
                x = point.x + normal.first * width * direction,
                y = point.y + normal.second * width * direction,
            )
        }

        return edgePoints.toImmutableList()
    }

    private fun normalAt(geometry: TrackEdgeGeometry, index: Int): Pair<Float, Float>? {
        val current = geometry.points.getOrNull(index) ?: return null
        val previous = findDistinctPoint(geometry = geometry, index = index, step = -1) ?: current
        val next = findDistinctPoint(geometry = geometry, index = index, step = 1) ?: current
        val tangentX = next.x - previous.x
        val tangentY = next.y - previous.y
        val tangentLength = hypot(tangentX, tangentY)
        if (!tangentLength.isFinite() || tangentLength <= 0.0001f) return null

        return (-tangentY / tangentLength) to (tangentX / tangentLength)
    }

    private fun findDistinctPoint(geometry: TrackEdgeGeometry, index: Int, step: Int): SessionAnalysisTrackPointUi? {
        if (geometry.points.isEmpty()) return null
        val anchor = geometry.points.getOrNull(index) ?: return null
        var currentIndex = index
        repeat(geometry.points.lastIndex) {
            currentIndex += step
            if (geometry.isClosedLoop) {
                currentIndex = when {
                    currentIndex < 0 -> geometry.points.lastIndex
                    currentIndex > geometry.points.lastIndex -> 0
                    else -> currentIndex
                }
            } else if (currentIndex !in geometry.points.indices) {
                return null
            }

            val candidate = geometry.points[currentIndex]
            if (hypot(candidate.x - anchor.x, candidate.y - anchor.y) > 0.0001f) {
                return candidate
            }
        }
        return null
    }

    private fun resolveMedianHalfWidth(points: List<SessionAnalysisTrackPointUi>): Float {
        val widths = points.mapNotNull(::halfWidthMeters)
        return pointSampler.median(widths) ?: TRACK_DEFAULT_HALF_WIDTH_METERS
    }

    private fun resolveFractions(points: List<SessionAnalysisTrackPointUi>): List<Float> {
        if (points.size < 2) return points.indices.map { 0f }

        val cumulativeDistances = cumulativePathDistances(points)
        val totalDistance = cumulativeDistances.lastOrNull() ?: 0f
        if (totalDistance <= 0.0001f) {
            val lastPointIndex = points.lastIndex.coerceAtLeast(1)
            return points.indices.map { index -> index.toFloat() / lastPointIndex.toFloat() }
        }

        return cumulativeDistances.map { distance -> (distance / totalDistance).coerceIn(0f, 1f) }
    }

    private fun cumulativePathDistances(points: List<SessionAnalysisTrackPointUi>): FloatArray {
        val distances = FloatArray(points.size)
        var totalDistance = 0f

        for (index in 1 until points.size) {
            val previous = points[index - 1]
            val current = points[index]
            val segmentLength = hypot(current.x - previous.x, current.y - previous.y)
            if (segmentLength.isFinite() && segmentLength > 0.0001f) {
                totalDistance += segmentLength
            }
            distances[index] = totalDistance
        }

        return distances
    }

    private fun trimLoopClosureDuplicate(points: List<SessionAnalysisTrackPointUi>): List<SessionAnalysisTrackPointUi> {
        if (points.size < 4) return points

        val metrics = loopMetrics(points) ?: return points
        return if (metrics.closureDistance <= maxOf(1f, metrics.medianSegmentLength * 1.5f)) {
            points.dropLast(1)
        } else {
            points
        }
    }

    private fun isClosedLoop(points: List<SessionAnalysisTrackPointUi>): Boolean {
        if (points.size < 3) return false

        val metrics = loopMetrics(points) ?: return false
        return metrics.closureDistance <= maxOf(4f, metrics.medianSegmentLength * 6f)
    }

    private fun loopMetrics(points: List<SessionAnalysisTrackPointUi>): TrackLoopMetrics? {
        val segmentLengths = buildList {
            for (index in 1 until points.size) {
                val previous = points[index - 1]
                val current = points[index]
                val segmentLength = hypot(current.x - previous.x, current.y - previous.y)
                if (segmentLength.isFinite() && segmentLength > 0.0001f) add(segmentLength)
            }
        }
        val medianSegmentLength = pointSampler.median(segmentLengths) ?: return null
        val closureDistance = hypot(points.last().x - points.first().x, points.last().y - points.first().y)

        return TrackLoopMetrics(
            closureDistance = closureDistance,
            medianSegmentLength = medianSegmentLength,
        )
    }

    private fun validLeftWidth(point: SessionAnalysisTrackPointUi): Float? =
        point.leftWidthMeters?.takeIf { value -> value.isFinite() && value > TRACK_MIN_VALID_WIDTH_METERS }

    private fun validRightWidth(point: SessionAnalysisTrackPointUi): Float? =
        point.rightWidthMeters?.takeIf { value -> value.isFinite() && value > TRACK_MIN_VALID_WIDTH_METERS }

    private fun surfaceWidthMeters(point: SessionAnalysisTrackPointUi): Float? {
        val leftWidth = validLeftWidth(point)
        val rightWidth = validRightWidth(point)
        return when {
            leftWidth != null && rightWidth != null -> leftWidth + rightWidth
            leftWidth != null -> leftWidth * 2f
            rightWidth != null -> rightWidth * 2f
            else -> null
        }
    }

    private fun halfWidthMeters(point: SessionAnalysisTrackPointUi): Float? {
        val leftWidth = validLeftWidth(point)
        val rightWidth = validRightWidth(point)
        return when {
            leftWidth != null && rightWidth != null -> (leftWidth + rightWidth) * 0.5f
            leftWidth != null -> leftWidth
            rightWidth != null -> rightWidth
            else -> null
        }
    }
}
