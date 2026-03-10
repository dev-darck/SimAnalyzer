package com.analyzer.trackmap.data.live

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.math.Vec2
import com.project.analyzer.math.Vec3
import com.project.analyzer.math.toVec2XZIfValid
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import com.project.analyzer.utils.TrackIdentityAliasMatcher
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.sample
import kotlin.math.abs
import kotlin.math.sqrt

@OptIn(FlowPreview::class)
@SingleIn(ScreenScope::class)
@ContributesBinding(ScreenScope::class, binding = binding<TrackMapLivePositionRepository>())
@Inject
class TrackMapLivePositionRepositoryImpl(
    private val telemetry: TelemetryLifecycle,
    @param:Default
    private val defaultDispatcher: CoroutineDispatcher,
) : TrackMapLivePositionRepository {

    private val logger = logger()

    override fun observe(item: TrackMapLibraryItem): Flow<Vec2?> = flow {
        var lastVisiblePosition: Vec2? = null
        var lastMatchedAtNs = 0L
        var lastMismatchAtNs = 0L

        telemetry.frames
            .sample(50L)
            .collect { frame ->
                val nowNs = System.nanoTime()
                val speedKmh = frame.car?.speedKmh ?: 0f
                val worldPosition = frame.car?.worldPosition?.toTrackMapVec2()
                val wheelPositions = extractWheelPositions(frame)
                val wheelReferencePosition = wheelPositions?.positionFor(item.map.referencePoint)
                val rawPosition = selectRawPosition(
                    item = item,
                    lastVisiblePosition = lastVisiblePosition,
                    worldPosition = worldPosition,
                    wheelReferencePosition = wheelReferencePosition,
                )
                val rawProjection = rawPosition?.let { candidate ->
                    projectOntoTrackResult(
                        point = candidate,
                        trackPoints = item.points,
                    )
                }
                val projectedPosition = rawProjection?.point
                val acceptedPosition = projectedPosition?.let { candidate ->
                    acceptProjectedPosition(
                        projectedPosition = candidate,
                        lastVisiblePosition = lastVisiblePosition,
                        speedKmh = speedKmh,
                    )
                }
                val lapProgressPosition = positionFromNormalizedLapPosition(
                    trackPoints = item.points,
                    normalizedLapPosition = frame.session?.track?.normalizedLapPosition,
                )
                val geometryFallbackPosition = rawProjection
                    ?.takeIf { it.distanceToTrack <= TRACK_GEOMETRY_MATCH_DISTANCE_METERS }
                    ?.point

                val nextPosition = when (matchTrack(frame = frame, item = item)) {
                    TrackMapMatchResult.MATCH -> {
                        when {
                            acceptedPosition != null -> {
                                lastVisiblePosition = acceptedPosition
                                lastMatchedAtNs = nowNs
                                lastMismatchAtNs = 0L
                                acceptedPosition
                            }

                            lapProgressPosition != null -> {
                                lastVisiblePosition = lapProgressPosition
                                lastMatchedAtNs = nowNs
                                lastMismatchAtNs = 0L
                                lapProgressPosition
                            }

                            rawPosition != null -> {
                                logger.atDebug(RATE_LIMITED) {
                                    message = buildString {
                                        append("TrackMap live rejected jump ")
                                        append("trackId=").append(frame.session?.track?.trackId.orEmpty())
                                        append(" layoutId=").append(frame.session?.track?.layoutId.orEmpty())
                                        append(" speed=").append(speedKmh)
                                        append(" raw=").append(rawPosition)
                                        append(" projected=").append(projectedPosition)
                                        append(" last=").append(lastVisiblePosition)
                                        append(" world=").append(worldPosition)
                                        append(" wheel=").append(wheelReferencePosition)
                                    }
                                }
                                lastVisiblePosition
                            }

                            else ->
                                lastVisiblePosition
                                    ?.takeIf { nowNs - lastMatchedAtNs <= TRACK_MATCH_GRACE_PERIOD_NS }
                                    .also {
                                        logger.atDebug(RATE_LIMITED) {
                                            message = buildString {
                                                append("TrackMap live position missing on matched frame ")
                                                append("trackId=").append(frame.session?.track?.trackId.orEmpty())
                                                append(" layoutId=").append(frame.session?.track?.layoutId.orEmpty())
                                                append(" speed=").append(speedKmh)
                                                append(" lap=").append(frame.session?.track?.normalizedLapPosition)
                                                append(" world=").append(frame.car?.worldPosition)
                                                append(" wheel=").append(wheelReferencePosition)
                                            }
                                        }
                                    }
                        }
                    }

                    TrackMapMatchResult.AMBIGUOUS -> {
                        when {
                            geometryFallbackPosition != null -> {
                                lastVisiblePosition = geometryFallbackPosition
                                lastMatchedAtNs = nowNs
                                lastMismatchAtNs = 0L
                                geometryFallbackPosition
                            }

                            lapProgressPosition != null -> {
                                lastVisiblePosition = lapProgressPosition
                                lastMatchedAtNs = nowNs
                                lastMismatchAtNs = 0L
                                lapProgressPosition
                            }

                            else -> {
                                logger.atDebug(RATE_LIMITED) {
                                    message = buildString {
                                        append("TrackMap live ambiguous frame ")
                                        append("itemTrack=").append(item.map.trackId)
                                        append(" itemLayout=").append(item.map.layoutId.orEmpty())
                                        append(" frameTrack=").append(frame.session?.track?.trackId.orEmpty())
                                        append(" frameLayout=").append(frame.session?.track?.layoutId.orEmpty())
                                        append(" lap=").append(frame.session?.track?.normalizedLapPosition)
                                        append(" world=").append(frame.car?.worldPosition)
                                    }
                                }
                                lastVisiblePosition
                            }
                        }
                    }

                    TrackMapMatchResult.MISMATCH -> {
                        if (geometryFallbackPosition != null) {
                            lastVisiblePosition = geometryFallbackPosition
                            lastMatchedAtNs = nowNs
                            lastMismatchAtNs = 0L
                            geometryFallbackPosition
                        } else {
                            val keepVisible = lastVisiblePosition != null &&
                                (lastMismatchAtNs == 0L || nowNs - lastMismatchAtNs <= TRACK_MISMATCH_GRACE_PERIOD_NS)
                            lastMismatchAtNs = nowNs
                            if (keepVisible) {
                                lastVisiblePosition
                            } else {
                                lastVisiblePosition = null
                                lastMatchedAtNs = 0L
                                null
                            }
                        }
                    }
                }
                emit(nextPosition)
            }
    }
        .distinctUntilChanged(::samePosition)
        .flowOn(defaultDispatcher)
        .onStart { emit(null) }
}

private enum class TrackMapMatchResult {
    MATCH,
    AMBIGUOUS,
    MISMATCH,
}

private fun matchTrack(frame: TelemetryFrame, item: TrackMapLibraryItem): TrackMapMatchResult {
    val trackInfo = frame.session?.track ?: return TrackMapMatchResult.AMBIGUOUS
    return if (
        TrackIdentityAliasMatcher.areEquivalent(
            trackId = item.map.trackId,
            layoutId = item.map.layoutId,
            otherTrackId = trackInfo.trackId,
            otherLayoutId = trackInfo.layoutId,
        )
    ) {
        TrackMapMatchResult.MATCH
    } else {
        logger.atDebug(RATE_LIMITED) {
            message = buildString {
                append("TrackMap live mismatch ")
                append("itemTrack=").append(item.map.trackId)
                append(" itemLayout=").append(item.map.layoutId.orEmpty())
                append(" frameTrack=").append(trackInfo.trackId.orEmpty())
                append(" frameLayout=").append(trackInfo.layoutId.orEmpty())
            }
        }
        TrackMapMatchResult.MISMATCH
    }
}

private fun extractPosition(frame: TelemetryFrame, referencePoint: ReferencePoint): Vec2? {
    frame.car?.worldPosition?.toTrackMapVec2()?.let { return it }
    val wheelPositions = extractWheelPositions(frame)
    if (wheelPositions != null) {
        when (referencePoint) {
            ReferencePoint.FRONT_AXLE -> wheelPositions.frontAxleCenter?.let { return it }
            ReferencePoint.REAR_AXLE -> wheelPositions.rearAxleCenter?.let { return it }
            ReferencePoint.CAR_CENTER -> wheelPositions.carCenter?.let { return it }
        }
    }
    return null
}

private data class WheelPositions(val fl: Vec2?, val fr: Vec2?, val rl: Vec2?, val rr: Vec2?) {

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
}

private data class ProjectionResult(val point: Vec2, val distanceToTrack: Float)

private fun extractWheelPositions(frame: TelemetryFrame): WheelPositions? {
    val wheels = frame.wheels ?: return null
    val wheelPositions = WheelPositions(
        fl = wheels.fl?.contactPoint?.toTrackMapVec2(),
        fr = wheels.fr?.contactPoint?.toTrackMapVec2(),
        rl = wheels.rl?.contactPoint?.toTrackMapVec2(),
        rr = wheels.rr?.contactPoint?.toTrackMapVec2(),
    )
    return if (
        wheelPositions.fl == null &&
        wheelPositions.fr == null &&
        wheelPositions.rl == null &&
        wheelPositions.rr == null
    ) {
        null
    } else {
        wheelPositions
    }
}

private fun axleCenter(left: Vec2?, right: Vec2?): Vec2? = when {
    left != null && right != null -> left.midpoint(right)
    left != null -> left
    right != null -> right
    else -> null
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

private fun acceptProjectedPosition(projectedPosition: Vec2, lastVisiblePosition: Vec2?, speedKmh: Float): Vec2? {
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

private fun projectOntoTrackResult(point: Vec2, trackPoints: List<Vec2>): ProjectionResult {
    if (trackPoints.isEmpty()) {
        return ProjectionResult(
            point = point,
            distanceToTrack = 0f,
        )
    }
    if (trackPoints.size == 1) {
        return ProjectionResult(
            point = trackPoints.first(),
            distanceToTrack = point.distanceTo(trackPoints.first()),
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
    return ProjectionResult(
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

private fun samePosition(old: Vec2?, new: Vec2?): Boolean {
    if (old == null || new == null) return old == new
    return abs(old.x - new.x) < POSITION_EPSILON && abs(old.y - new.y) < POSITION_EPSILON
}

private const val MAX_VALID_COORDINATE = 1_000_000f
private const val POSITION_EPSILON = 0.25f
private const val TRACK_MATCH_GRACE_PERIOD_NS = 3_000_000_000L
private const val TRACK_MISMATCH_GRACE_PERIOD_NS = 5_000_000_000L
private const val TRACK_GEOMETRY_MATCH_DISTANCE_METERS = 45f
private const val MAX_ALLOWED_JUMP_METERS = 140f
