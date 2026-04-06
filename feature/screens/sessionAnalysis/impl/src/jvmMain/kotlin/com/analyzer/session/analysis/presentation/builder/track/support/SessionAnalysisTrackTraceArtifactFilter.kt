package com.analyzer.session.analysis.presentation.builder.track.support

import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMapPoint
import kotlin.math.abs
import kotlin.math.hypot

internal class SessionAnalysisTrackTraceArtifactFilter(
    private val polylineSmoother: SessionAnalysisTrackPolylineSmoother = SessionAnalysisTrackPolylineSmoother(),
) {

    fun toFractionTrace(points: List<SessionAnalysisTrackMapPoint>): List<SessionAnalysisFractionPointUi> {
        if (points.size < 2) return emptyList()
        val fractions = FloatArray(points.size)
        var totalDistance = 0f
        for (index in 1 until points.size) {
            totalDistance += hypot(
                points[index].x - points[index - 1].x,
                points[index].y - points[index - 1].y,
            )
            fractions[index] = totalDistance
        }
        if (!totalDistance.isFinite() || totalDistance <= 0.0001f) {
            for (index in fractions.indices) {
                fractions[index] = if (points.lastIndex <= 0) 0f else index.toFloat() / points.lastIndex.toFloat()
            }
        } else {
            for (index in fractions.indices) {
                fractions[index] = (fractions[index] / totalDistance).coerceIn(0f, 1f)
            }
        }
        return points.mapIndexed { index, point ->
            SessionAnalysisFractionPointUi(
                fraction = fractions[index],
                x = point.x,
                y = point.y,
            )
        }
    }

    fun stabilizeTraceProgression(
        points: List<SessionAnalysisFractionPointUi>,
        fallbackPoints: List<SessionAnalysisFractionPointUi>,
        guideLookup: TrackMapFractionLookup?,
    ): List<SessionAnalysisFractionPointUi> {
        if (points.size < 3 || fallbackPoints.size != points.size || guideLookup == null) return points

        val stabilized = ArrayList<SessionAnalysisFractionPointUi>(points.size)
        stabilized += points.first()
        for (index in 1 until points.lastIndex) {
            val previous = stabilized.last()
            val candidate = points[index]
            val next = points[index + 1]
            val fallback = fallbackPoints[index]
            stabilized += if (breaksTraceProgression(previous, candidate, next, guideLookup)) {
                fallback
            } else {
                candidate
            }
        }
        stabilized += points.last()
        return stabilized
    }

    fun removeNearDuplicateTracePoints(
        points: List<SessionAnalysisFractionPointUi>,
        minDistance: Float,
    ): List<SessionAnalysisFractionPointUi> {
        if (points.size < 3) return points
        val result = ArrayList<SessionAnalysisFractionPointUi>(points.size)
        var lastKept: SessionAnalysisFractionPointUi? = null
        points.forEachIndexed { index, point ->
            if (index == 0 || index == points.lastIndex) {
                result += point
                lastKept = point
                return@forEachIndexed
            }
            val previous = lastKept ?: point
            val distance = hypot(point.x - previous.x, point.y - previous.y)
            if (distance >= minDistance) {
                result += point
                lastKept = point
            }
        }
        return if (result.size >= 2) result else points
    }

    fun suppressLocalFoldArtifacts(
        points: List<SessionAnalysisFractionPointUi>,
    ): List<SessionAnalysisFractionPointUi> {
        if (points.size < 4) return points
        val result = ArrayList<SessionAnalysisFractionPointUi>(points.size)
        result += points.first()
        for (index in 1 until points.lastIndex) {
            val previous = result.last()
            val current = points[index]
            val next = points[index + 1]
            if (!isLocalFoldArtifact(previous, current, next)) {
                result += current
            }
        }
        result += points.last()
        return if (result.size >= 2) result else points
    }

    fun smoothMinorTraceJitter(
        points: List<SessionAnalysisFractionPointUi>,
        maxLateralDrift: Float,
        guideLookup: TrackMapFractionLookup? = null,
    ): List<SessionAnalysisFractionPointUi> {
        if (points.size < 5 || !maxLateralDrift.isFinite() || maxLateralDrift <= 0.02f) return points
        val smoothed = polylineSmoother.smooth(points = points, closed = false)
        if (smoothed.size != points.size) return points

        val result = ArrayList<SessionAnalysisFractionPointUi>(points.size)
        result += points.first()
        for (index in 1 until points.lastIndex) {
            val previous = result.last()
            val current = points[index]
            val next = points[index + 1]
            val candidate = smoothed[index]
            result += if (
                isMinorJitterArtifact(
                    previous = previous,
                    current = current,
                    next = next,
                    smoothed = candidate,
                    maxLateralDrift = maxLateralDrift,
                    guideLookup = guideLookup,
                )
            ) {
                candidate
            } else {
                current
            }
        }
        result += points.last()
        return result
    }

    fun smoothTrace(points: List<SessionAnalysisFractionPointUi>): List<SessionAnalysisFractionPointUi> =
        polylineSmoother.smooth(
            points = points,
            closed = false,
        )

    private fun breaksTraceProgression(
        previous: SessionAnalysisFractionPointUi,
        candidate: SessionAnalysisFractionPointUi,
        next: SessionAnalysisFractionPointUi,
        guideLookup: TrackMapFractionLookup,
    ): Boolean {
        val incomingX = candidate.x - previous.x
        val incomingY = candidate.y - previous.y
        val outgoingX = next.x - candidate.x
        val outgoingY = next.y - candidate.y
        val incomingLength = hypot(incomingX, incomingY)
        val outgoingLength = hypot(outgoingX, outgoingY)
        if (!incomingLength.isFinite() || !outgoingLength.isFinite()) return true
        if (incomingLength <= 0.0001f || outgoingLength <= 0.0001f) return true

        val guideStart = guideLookup.samplePositionAt(previous.fraction) ?: return false
        val guideEnd = guideLookup.samplePositionAt(next.fraction) ?: return false
        val guideX = guideEnd.first - guideStart.first
        val guideY = guideEnd.second - guideStart.second
        val guideLength = hypot(guideX, guideY)
        if (!guideLength.isFinite() || guideLength <= 0.0001f) return false

        val forwardIntoCorner = incomingX * guideX + incomingY * guideY
        val forwardOutOfCorner = outgoingX * guideX + outgoingY * guideY
        if (forwardIntoCorner <= 0f || forwardOutOfCorner <= 0f) return true

        val localTurnCosine = (incomingX * outgoingX + incomingY * outgoingY) / (incomingLength * outgoingLength)
        return localTurnCosine < -0.72f
    }

    private fun isLocalFoldArtifact(
        previous: SessionAnalysisFractionPointUi,
        current: SessionAnalysisFractionPointUi,
        next: SessionAnalysisFractionPointUi,
    ): Boolean {
        val incomingX = current.x - previous.x
        val incomingY = current.y - previous.y
        val outgoingX = next.x - current.x
        val outgoingY = next.y - current.y
        val chordX = next.x - previous.x
        val chordY = next.y - previous.y
        val incomingLength = hypot(incomingX, incomingY)
        val outgoingLength = hypot(outgoingX, outgoingY)
        val chordLength = hypot(chordX, chordY)
        if (!incomingLength.isFinite() || !outgoingLength.isFinite() || !chordLength.isFinite()) return false
        if (incomingLength <= 0.0001f || outgoingLength <= 0.0001f || chordLength <= 0.0001f) return true

        val localTurnCosine = (incomingX * outgoingX + incomingY * outgoingY) / (incomingLength * outgoingLength)
        val pathInflation = (incomingLength + outgoingLength) / chordLength
        if (pathInflation < 1.32f) return false

        val chordUnitX = chordX / chordLength
        val chordUnitY = chordY / chordLength
        val incomingProjection = incomingX * chordUnitX + incomingY * chordUnitY
        val outgoingProjection = outgoingX * chordUnitX + outgoingY * chordUnitY
        val deviationFromChord = perpendicularDistanceToChord(current, previous, next)

        return deviationFromChord > maxOf(0.4f, chordLength * 0.22f) &&
            (
                incomingProjection <= 0f ||
                    outgoingProjection <= 0f ||
                    localTurnCosine < -0.18f ||
                    pathInflation > 1.85f
                )
    }

    private fun isMinorJitterArtifact(
        previous: SessionAnalysisFractionPointUi,
        current: SessionAnalysisFractionPointUi,
        next: SessionAnalysisFractionPointUi,
        smoothed: SessionAnalysisFractionPointUi,
        maxLateralDrift: Float,
        guideLookup: TrackMapFractionLookup?,
    ): Boolean {
        val incomingX = current.x - previous.x
        val incomingY = current.y - previous.y
        val outgoingX = next.x - current.x
        val outgoingY = next.y - current.y
        val chordX = next.x - previous.x
        val chordY = next.y - previous.y
        val incomingLength = hypot(incomingX, incomingY)
        val outgoingLength = hypot(outgoingX, outgoingY)
        val chordLength = hypot(chordX, chordY)
        if (!incomingLength.isFinite() || !outgoingLength.isFinite() || !chordLength.isFinite()) return false
        if (incomingLength <= 0.0001f || outgoingLength <= 0.0001f || chordLength <= 0.0001f) return false

        val smoothingDelta = hypot(current.x - smoothed.x, current.y - smoothed.y)
        if (!smoothingDelta.isFinite() || smoothingDelta < 0.015f || smoothingDelta > maxLateralDrift) return false

        val localTurnCosine = (incomingX * outgoingX + incomingY * outgoingY) / (incomingLength * outgoingLength)
        val pathInflation = (incomingLength + outgoingLength) / chordLength
        val incomingProjection = (incomingX * chordX + incomingY * chordY) / chordLength
        val outgoingProjection = (outgoingX * chordX + outgoingY * chordY) / chordLength
        val deviationFromChord = perpendicularDistanceToChord(current, previous, next)

        if (incomingProjection <= 0f || outgoingProjection <= 0f) return false
        if (deviationFromChord > maxLateralDrift * 1.35f) return false
        if (guideSuggestsActualCorner(previous, current, next, guideLookup, maxLateralDrift)) return false

        return deviationFromChord <= maxLateralDrift &&
            (
                localTurnCosine < 0.9995f ||
                    pathInflation > 1.01f
                )
    }

    private fun guideSuggestsActualCorner(
        previous: SessionAnalysisFractionPointUi,
        current: SessionAnalysisFractionPointUi,
        next: SessionAnalysisFractionPointUi,
        guideLookup: TrackMapFractionLookup?,
        maxLateralDrift: Float,
    ): Boolean {
        guideLookup ?: return false
        val guidePrevious = guideLookup.samplePositionAt(previous.fraction) ?: return false
        val guideCurrent = guideLookup.samplePositionAt(current.fraction) ?: return false
        val guideNext = guideLookup.samplePositionAt(next.fraction) ?: return false
        val incomingX = guideCurrent.first - guidePrevious.first
        val incomingY = guideCurrent.second - guidePrevious.second
        val outgoingX = guideNext.first - guideCurrent.first
        val outgoingY = guideNext.second - guideCurrent.second
        val incomingLength = hypot(incomingX, incomingY)
        val outgoingLength = hypot(outgoingX, outgoingY)
        if (!incomingLength.isFinite() || !outgoingLength.isFinite()) return false
        if (incomingLength <= 0.0001f || outgoingLength <= 0.0001f) return false

        val guideTurnCosine = (incomingX * outgoingX + incomingY * outgoingY) / (incomingLength * outgoingLength)
        val guideDeviation = perpendicularDistanceToChord(
            pointX = guideCurrent.first,
            pointY = guideCurrent.second,
            chordStartX = guidePrevious.first,
            chordStartY = guidePrevious.second,
            chordEndX = guideNext.first,
            chordEndY = guideNext.second,
        )

        return guideTurnCosine < GUIDE_CORNER_TURN_COSINE &&
            guideDeviation > maxLateralDrift * GUIDE_CORNER_DRIFT_RATIO
    }

    private fun perpendicularDistanceToChord(
        point: SessionAnalysisFractionPointUi,
        chordStart: SessionAnalysisFractionPointUi,
        chordEnd: SessionAnalysisFractionPointUi,
    ): Float = perpendicularDistanceToChord(
        pointX = point.x,
        pointY = point.y,
        chordStartX = chordStart.x,
        chordStartY = chordStart.y,
        chordEndX = chordEnd.x,
        chordEndY = chordEnd.y,
    )

    private fun perpendicularDistanceToChord(
        pointX: Float,
        pointY: Float,
        chordStartX: Float,
        chordStartY: Float,
        chordEndX: Float,
        chordEndY: Float,
    ): Float {
        val chordX = chordEndX - chordStartX
        val chordY = chordEndY - chordStartY
        val chordLength = hypot(chordX, chordY)
        if (!chordLength.isFinite() || chordLength <= 0.0001f) return 0f
        val areaTwice = abs(
            (pointX - chordStartX) * chordY - (pointY - chordStartY) * chordX,
        )
        return areaTwice / chordLength
    }

    private companion object {

        private const val GUIDE_CORNER_TURN_COSINE: Float = 0.9985f
        private const val GUIDE_CORNER_DRIFT_RATIO: Float = 0.35f
    }
}
