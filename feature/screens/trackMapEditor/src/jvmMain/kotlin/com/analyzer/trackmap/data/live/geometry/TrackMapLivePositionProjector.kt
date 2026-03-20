package com.analyzer.trackmap.data.live.geometry

import com.analyzer.trackmap.data.live.model.TrackMapLivePositionCandidates
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.math.Vec2
import com.project.analyzer.math.Vec3
import com.project.analyzer.math.toVec2XZIfValid
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import kotlin.math.sqrt

internal class TrackMapLivePositionProjector(
    private val item: TrackMapLibraryItem,
) {

    fun buildCandidates(
        frame: TelemetryFrame,
        lastVisiblePosition: Vec2?,
        speedKmh: Float,
    ): TrackMapLivePositionCandidates {
        val worldPosition = frame.car?.worldPosition?.toTrackMapVec2()
        val wheelPositions = extractWheelPositions(frame)
        val wheelReferencePosition = wheelPositions?.positionFor(item.map.referencePoint)
        val rawPosition = selectRawPosition(
            item = item,
            lastVisiblePosition = lastVisiblePosition,
            worldPosition = worldPosition,
            wheelReferencePosition = wheelReferencePosition,
        )
        val rawProjection = rawPosition?.let { point ->
            projectOntoTrackResult(
                point = point,
                trackPoints = item.points,
            )
        }
        val acceptedPosition = rawProjection
            ?.point
            ?.let { projectedPosition ->
                acceptProjectedPosition(
                    projectedPosition = projectedPosition,
                    lastVisiblePosition = lastVisiblePosition,
                    speedKmh = speedKmh,
                )
            }
        val lapProgressPosition = positionFromNormalizedLapPosition(
            trackPoints = item.points,
            normalizedLapPosition = frame.session?.track?.normalizedLapPosition,
        )
        val geometryFallbackPosition = rawProjection
            ?.takeIf(::canUseGeometryFallback)
            ?.point

        return TrackMapLivePositionCandidates(
            worldPosition = worldPosition,
            wheelReferencePosition = wheelReferencePosition,
            rawPosition = rawPosition,
            rawProjection = rawProjection,
            acceptedPosition = acceptedPosition,
            lapProgressPosition = lapProgressPosition,
            geometryFallbackPosition = geometryFallbackPosition,
        )
    }

    fun isNearTrack(projection: TrackMapLivePositionProjection): Boolean =
        projection.distanceToTrack <= TRACK_REANCHOR_DISTANCE_METERS

    private fun canUseGeometryFallback(projection: TrackMapLivePositionProjection): Boolean =
        projection.distanceToTrack <= TRACK_GEOMETRY_MATCH_DISTANCE_METERS

    private fun extractWheelPositions(frame: TelemetryFrame): WheelPositions? {
        val wheels = frame.wheels ?: return null
        val wheelPositions = WheelPositions(
            fl = wheels.fl?.contactPoint?.toTrackMapVec2(),
            fr = wheels.fr?.contactPoint?.toTrackMapVec2(),
            rl = wheels.rl?.contactPoint?.toTrackMapVec2(),
            rr = wheels.rr?.contactPoint?.toTrackMapVec2(),
        )
        return wheelPositions.takeIf(WheelPositions::hasAnyContactPoint)
    }

    private fun selectRawPosition(
        item: TrackMapLibraryItem,
        lastVisiblePosition: Vec2?,
        worldPosition: Vec2?,
        wheelReferencePosition: Vec2?,
    ): Vec2? {
        val candidates = buildList {
            wheelReferencePosition?.let(::add)
            worldPosition?.let(::add)
        }.distinctBy { candidate -> "${candidate.x}:${candidate.y}" }
        if (candidates.isEmpty()) return null
        if (candidates.size == 1) return candidates.first()

        return candidates.minByOrNull { candidate ->
            scoreCandidate(
                candidate = candidate,
                trackPoints = item.points,
                lastVisiblePosition = lastVisiblePosition,
            )
        }
    }

    private fun scoreCandidate(candidate: Vec2, trackPoints: List<Vec2>, lastVisiblePosition: Vec2?): Float {
        val trackProjection = projectOntoTrack(
            point = candidate,
            trackPoints = trackPoints,
        )
        val trackDistance = candidate.distanceTo(trackProjection)
        val continuityPenalty = lastVisiblePosition?.distanceTo(trackProjection)?.times(0.35f) ?: 0f
        return trackDistance + continuityPenalty
    }

    private fun acceptProjectedPosition(
        projectedPosition: Vec2,
        lastVisiblePosition: Vec2?,
        speedKmh: Float,
    ): Vec2? {
        val previous = lastVisiblePosition ?: return projectedPosition
        val jumpDistance = projectedPosition.distanceTo(previous)
        val maxAllowedJump = ((speedKmh.coerceAtLeast(20f) / 3.6f) * 0.05f * 4f + 20f)
            .coerceIn(20f, MAX_ALLOWED_JUMP_METERS)
        return projectedPosition.takeIf { jumpDistance <= maxAllowedJump }
    }

    private fun positionFromNormalizedLapPosition(trackPoints: List<Vec2>, normalizedLapPosition: Float?): Vec2? {
        val clampedProgress = normalizedLapPosition
            ?.takeIf(Float::isFinite)
            ?.coerceIn(0f, 1f)
            ?: return null
        if (trackPoints.isEmpty()) return null
        if (trackPoints.size == 1) return trackPoints.first()

        val totalLength = polylineLength(trackPoints)
        if (totalLength <= 0.001f) return trackPoints.first()

        val targetDistance = totalLength * clampedProgress
        var traversed = 0f
        for (index in 1 until trackPoints.size) {
            val start = trackPoints[index - 1]
            val end = trackPoints[index]
            val segmentLength = start.distanceTo(end)
            if (segmentLength <= 0.0001f) continue
            if (traversed + segmentLength >= targetDistance) {
                val t = ((targetDistance - traversed) / segmentLength).coerceIn(0f, 1f)
                return start + (end - start) * t
            }
            traversed += segmentLength
        }
        return trackPoints.last()
    }

    private fun polylineLength(trackPoints: List<Vec2>): Float {
        var totalLength = 0f
        for (index in 1 until trackPoints.size) {
            totalLength += trackPoints[index - 1].distanceTo(trackPoints[index])
        }
        return totalLength
    }

    private fun projectOntoTrack(point: Vec2, trackPoints: List<Vec2>): Vec2 = projectOntoTrackResult(
        point = point,
        trackPoints = trackPoints,
    ).point

    private fun projectOntoTrackResult(point: Vec2, trackPoints: List<Vec2>): TrackMapLivePositionProjection {
        if (trackPoints.isEmpty()) {
            return TrackMapLivePositionProjection(
                point = point,
                distanceToTrack = 0f,
            )
        }
        if (trackPoints.size == 1) {
            val trackPoint = trackPoints.first()
            return TrackMapLivePositionProjection(
                point = trackPoint,
                distanceToTrack = point.distanceTo(trackPoint),
            )
        }

        var bestPoint = trackPoints.first()
        var bestDistanceSq = Float.MAX_VALUE
        for (index in 1 until trackPoints.size) {
            val segmentProjection = projectOntoSegment(
                point = point,
                start = trackPoints[index - 1],
                end = trackPoints[index],
            )
            val distanceSq = (point - segmentProjection).len2()
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq
                bestPoint = segmentProjection
            }
        }
        return TrackMapLivePositionProjection(
            point = bestPoint,
            distanceToTrack = sqrt(bestDistanceSq),
        )
    }

    private fun projectOntoSegment(point: Vec2, start: Vec2, end: Vec2): Vec2 {
        val segment = end - start
        val lengthSq = segment.len2()
        if (lengthSq <= 0.0001f) return start
        val t = ((point - start).dot(segment) / lengthSq).coerceIn(0f, 1f)
        return start + segment * t
    }

    private fun Vec3.toTrackMapVec2(): Vec2? = toVec2XZIfValid(maxAbsCoordinate = MAX_VALID_COORDINATE)

    private data class WheelPositions(
        val fl: Vec2?,
        val fr: Vec2?,
        val rl: Vec2?,
        val rr: Vec2?,
    ) {

        val frontAxleCenter: Vec2?
            get() = axleCenter(fl, fr)
        val rearAxleCenter: Vec2?
            get() = axleCenter(rl, rr)
        val carCenter: Vec2?
            get() = when {
                frontAxleCenter != null && rearAxleCenter != null -> frontAxleCenter!!.midpoint(rearAxleCenter!!)
                frontAxleCenter != null -> frontAxleCenter
                rearAxleCenter != null -> rearAxleCenter
                else -> null
            }

        fun positionFor(referencePoint: ReferencePoint): Vec2? = when (referencePoint) {
            ReferencePoint.FRONT_AXLE -> frontAxleCenter
            ReferencePoint.REAR_AXLE -> rearAxleCenter
            ReferencePoint.CAR_CENTER -> carCenter
        }

        fun hasAnyContactPoint(): Boolean = listOf(fl, fr, rl, rr).any { it != null }

        private fun axleCenter(left: Vec2?, right: Vec2?): Vec2? = when {
            left != null && right != null -> left.midpoint(right)
            left != null -> left
            right != null -> right
            else -> null
        }
    }

    private companion object {

        const val MAX_VALID_COORDINATE = 1_000_000f
        const val TRACK_GEOMETRY_MATCH_DISTANCE_METERS = 45f
        const val MAX_ALLOWED_JUMP_METERS = 140f
        const val TRACK_REANCHOR_DISTANCE_METERS = 45f
    }
}
