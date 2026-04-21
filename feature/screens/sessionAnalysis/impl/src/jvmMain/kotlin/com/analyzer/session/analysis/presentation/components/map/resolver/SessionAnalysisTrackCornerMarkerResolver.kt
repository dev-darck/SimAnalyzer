package com.analyzer.session.analysis.presentation.components.map.resolver

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.IntSize
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarker
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarkerPlacement
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarkerResolverInput
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerMarkerSide
import com.analyzer.session.analysis.presentation.components.map.model.TrackCornerTurnDirection
import com.analyzer.session.analysis.presentation.components.map.model.TrackDiagnosticPalette
import com.analyzer.session.analysis.presentation.components.map.support.isClosedTelemetryLoop
import com.analyzer.session.analysis.presentation.components.map.support.lerp
import com.analyzer.session.analysis.presentation.components.map.support.normalizedOrNull
import com.analyzer.session.analysis.presentation.components.map.support.projectPointOnSegment
import com.analyzer.session.analysis.presentation.components.map.support.sampleDirectionAtFraction
import com.analyzer.session.analysis.presentation.components.map.support.samplePointAtFraction
import com.analyzer.session.analysis.presentation.components.map.support.toOffset
import com.analyzer.session.analysis.presentation.model.CornerScoreUi
import com.analyzer.session.analysis.presentation.model.studio.SessionAnalysisFractionPointUi
import kotlin.math.abs
import kotlin.math.sqrt

private const val trackCornerMarkerTurnThreshold: Float = 0.012f
private const val trackCornerMarkerCandidateAttempts: Int = 6
private const val trackCornerMarkerOuterSideBiasPx: Float = 6f
private const val trackCornerMarkerGeometryWindowFraction: Float = 0.018f
private const val trackCornerMarkerSkippedSegmentsRadius: Int = 1
private const val trackCornerMarkerClearanceBufferPx: Float = 2f
private const val trackCornerMarkerTangentScorePenaltyFactor: Float = 0.12f

/**
 * Places numbered corner markers around the rendered track while balancing clearance, collision, and turn direction.
 */
internal fun resolveCornerMarkers(
    corners: List<CornerScoreUi>,
    anchorLine: List<SessionAnalysisFractionPointUi>,
    input: TrackCornerMarkerResolverInput,
    occupiedPositions: List<Offset> = emptyList(),
): List<TrackCornerMarker> {
    if (corners.isEmpty()) return emptyList()
    val placedMarkers = mutableListOf<TrackCornerMarker>()
    corners.forEach { corner ->
        val marker = anchorLine.toCornerMarker(
            corner = corner,
            occupiedPositions = occupiedPositions + placedMarkers.map(TrackCornerMarker::position),
            input = input,
        ) ?: return@forEach
        placedMarkers += marker
    }
    return placedMarkers
}

internal fun resolveCornerMarkerPlacement(
    surfaceStrokePx: Float,
    markerRadiusPx: Float = 13f,
    edgeGapPx: Float = 16f,
    spacingPx: Float = 32f,
    tangentRetryPx: Float = 18f,
    outwardRetryPx: Float = 10f,
    viewportMarginPx: Float = 24f,
): TrackCornerMarkerPlacement {
    val edgeOffset = markerRadiusPx + edgeGapPx
    val baseDistance = surfaceStrokePx * 0.5f + edgeOffset
    return TrackCornerMarkerPlacement(
        edgeOffsetPx = edgeOffset,
        baseDistancePx = baseDistance,
        spacingPx = spacingPx,
        tangentRetryPx = tangentRetryPx,
        outwardRetryPx = outwardRetryPx,
        viewportMarginPx = viewportMarginPx,
    )
}

private fun List<SessionAnalysisFractionPointUi>.toCornerMarker(
    corner: CornerScoreUi,
    occupiedPositions: List<Offset>,
    input: TrackCornerMarkerResolverInput,
): TrackCornerMarker? {
    val geometry = resolveCornerMarkerGeometry(
        fraction = corner.markerTrackPosition,
        trackCenter = input.trackCenter,
    ) ?: return null
    val context = CornerMarkerPlacementContext(
        geometry = geometry,
        corner = corner,
        occupiedPositions = occupiedPositions,
        input = input,
    )
    val candidates = listOf(
        resolveCornerMarkerCandidateForSide(
            context = context,
            side = TrackCornerMarkerSide.Outer,
        ),
        resolveCornerMarkerCandidateForSide(
            context = context,
            side = TrackCornerMarkerSide.Inner,
        ),
    )
    val bestCandidate = candidates
        .filter(CornerMarkerCandidate::valid)
        .maxByOrNull(CornerMarkerCandidate::score)
        ?: candidates.maxByOrNull(CornerMarkerCandidate::score)
        ?: return null
    return TrackCornerMarker(
        corner = corner,
        position = bestCandidate.position,
        accent = input.palette.cornerAccent(geometry.turnDirection),
        turnDirection = geometry.turnDirection,
        placementSide = bestCandidate.side,
    )
}

private fun List<SessionAnalysisFractionPointUi>.resolveCornerMarkerCandidateForSide(
    context: CornerMarkerPlacementContext,
    side: TrackCornerMarkerSide,
): CornerMarkerCandidate {
    var bestCandidate: CornerMarkerCandidate? = null
    repeat(trackCornerMarkerCandidateAttempts) { attempt ->
        val candidate = buildCornerMarkerCandidate(
            context = context,
            side = side,
            attempt = attempt,
        )
        if (candidate.valid) return candidate
        if (bestCandidate == null || candidate.score > bestCandidate.score) {
            bestCandidate = candidate
        }
    }
    return bestCandidate
        ?: CornerMarkerCandidate(
            position = context.geometry.anchorOffset,
            side = side,
            score = Float.NEGATIVE_INFINITY,
            valid = false,
        )
}

private fun List<SessionAnalysisFractionPointUi>.buildCornerMarkerCandidate(
    context: CornerMarkerPlacementContext,
    side: TrackCornerMarkerSide,
    attempt: Int,
): CornerMarkerCandidate {
    val placement = context.input.placement
    val sideNormal = context.geometry.normalFor(side)
    val surfaceAnchor = resolveTrackSurfaceAnchor(
        fraction = context.corner.markerTrackPosition,
        anchorOffset = context.geometry.anchorOffset,
        sideNormal = sideNormal,
        input = context.input,
    )
    val outwardRetryLevel = attempt / 3
    val baseAnchor = surfaceAnchor ?: context.geometry.anchorOffset
    val outwardDistance = (
        if (surfaceAnchor != null) placement.edgeOffsetPx else placement.baseDistancePx
        ) + outwardRetryLevel * placement.outwardRetryPx
    val tangentDistance = when (attempt % 3) {
        0 -> 0f
        1 -> -placement.tangentRetryPx * (1f + outwardRetryLevel * 0.2f)
        else -> placement.tangentRetryPx * (1f + outwardRetryLevel * 0.2f)
    }
    val position = (
        baseAnchor +
            sideNormal * outwardDistance +
            context.geometry.tangentDirection * tangentDistance
        ).clampToViewport(
        canvasSize = context.input.canvasSize,
        margin = placement.viewportMarginPx,
    )
    val markerCollision = context.occupiedPositions.any { occupied ->
        occupied.distanceTo(position) < placement.spacingPx
    }
    val trackClearance = resolveTrackMarkerClearance(
        markerPosition = position,
        anchorFraction = context.corner.markerTrackPosition,
        input = context.input,
    )
    val requiredTrackClearance = placement.baseDistancePx +
        outwardRetryLevel * placement.outwardRetryPx +
        trackCornerMarkerClearanceBufferPx
    val valid = !markerCollision && trackClearance >= requiredTrackClearance
    val sideBias = if (side == TrackCornerMarkerSide.Outer) trackCornerMarkerOuterSideBiasPx else 0f
    val score = trackClearance -
        if (markerCollision) {
            placement.spacingPx
        } else {
            0f -
                outwardRetryLevel * placement.outwardRetryPx * 0.08f -
                abs(tangentDistance) * trackCornerMarkerTangentScorePenaltyFactor +
                sideBias
        }
    return CornerMarkerCandidate(
        position = position,
        side = side,
        score = score,
        valid = valid,
    )
}

private fun List<SessionAnalysisFractionPointUi>.resolveCornerMarkerGeometry(
    fraction: Float,
    trackCenter: Offset,
): CornerMarkerGeometry? {
    val anchorPoint = samplePointAtFraction(fraction) ?: return null
    val tangentDirection = sampleDirectionAtFraction(fraction)?.normalizedOrNull() ?: return null
    val leftNormal = tangentDirection.toLeftNormal().normalizedOrNull() ?: return null
    val rightNormal = leftNormal * -1f
    val turnDirection = resolveTurnDirection(fraction = fraction)
    val outwardNormal = when (turnDirection) {
        TrackCornerTurnDirection.Left -> rightNormal

        TrackCornerTurnDirection.Right -> leftNormal

        TrackCornerTurnDirection.Straight -> resolveOutwardNormal(
            anchorOffset = anchorPoint.toOffset(),
            trackCenter = trackCenter,
            leftNormal = leftNormal,
            rightNormal = rightNormal,
        )
    }
    return CornerMarkerGeometry(
        anchorOffset = anchorPoint.toOffset(),
        tangentDirection = tangentDirection,
        outerNormal = outwardNormal,
        innerNormal = outwardNormal * -1f,
        turnDirection = turnDirection,
    )
}

private fun List<SessionAnalysisFractionPointUi>.resolveTurnDirection(fraction: Float): TrackCornerTurnDirection {
    val previousDirection = sampleDirectionAtFraction(
        ((fraction - trackCornerMarkerGeometryWindowFraction) + 1f) % 1f,
    ) ?: return TrackCornerTurnDirection.Straight
    val nextDirection = sampleDirectionAtFraction(
        (fraction + trackCornerMarkerGeometryWindowFraction) % 1f,
    ) ?: return TrackCornerTurnDirection.Straight
    val turnSign = previousDirection.x * nextDirection.y - previousDirection.y * nextDirection.x
    return when {
        turnSign < -trackCornerMarkerTurnThreshold -> TrackCornerTurnDirection.Left
        turnSign > trackCornerMarkerTurnThreshold -> TrackCornerTurnDirection.Right
        else -> TrackCornerTurnDirection.Straight
    }
}

private fun resolveOutwardNormal(
    anchorOffset: Offset,
    trackCenter: Offset,
    leftNormal: Offset,
    rightNormal: Offset,
): Offset {
    val outward = (anchorOffset - trackCenter).normalizedOrNull() ?: return rightNormal
    return if (
        leftNormal.x * outward.x + leftNormal.y * outward.y >=
        rightNormal.x * outward.x + rightNormal.y * outward.y
    ) {
        leftNormal
    } else {
        rightNormal
    }
}

private fun TrackDiagnosticPalette.cornerAccent(
    turnDirection: TrackCornerTurnDirection,
): androidx.compose.ui.graphics.Color = when (turnDirection) {
    TrackCornerTurnDirection.Left -> leftTurn
    TrackCornerTurnDirection.Right -> rightTurn
    TrackCornerTurnDirection.Straight -> straightTurn
}

private fun Offset.toLeftNormal(): Offset = Offset(x = y, y = -x)

private fun List<SessionAnalysisFractionPointUi>.resolveTrackMarkerClearance(
    markerPosition: Offset,
    anchorFraction: Float,
    input: TrackCornerMarkerResolverInput,
): Float = listOf(this, input.trackLeftEdge, input.trackRightEdge)
    .filter { path -> path.size >= 2 }
    .minOfOrNull { path ->
        path.pathClearance(markerPosition = markerPosition, anchorFraction = anchorFraction)
    } ?: Float.POSITIVE_INFINITY

private fun List<SessionAnalysisFractionPointUi>.pathClearance(markerPosition: Offset, anchorFraction: Float): Float {
    if (size < 2) return Float.POSITIVE_INFINITY
    val anchorSegmentIndex = resolveAnchorSegmentIndex(anchorFraction)
    var minimumDistance = Float.POSITIVE_INFINITY
    for (index in 1 until size) {
        if (abs(index - anchorSegmentIndex) <= trackCornerMarkerSkippedSegmentsRadius) {
            continue
        }
        val start = this[index - 1]
        val end = this[index]
        minimumDistance = minOf(
            minimumDistance,
            markerPosition.distanceToSegment(start = start.toOffset(), end = end.toOffset()),
        )
    }
    if (
        isClosedTelemetryLoop() &&
        !isClosureSegmentNearAnchor(anchorSegmentIndex)
    ) {
        minimumDistance = minOf(
            minimumDistance,
            markerPosition.distanceToSegment(start = last().toOffset(), end = first().toOffset()),
        )
    }
    return minimumDistance
}

private fun resolveTrackSurfaceAnchor(
    fraction: Float,
    anchorOffset: Offset,
    sideNormal: Offset,
    input: TrackCornerMarkerResolverInput,
): Offset? {
    val candidateEdges = listOf(input.trackLeftEdge, input.trackRightEdge)
        .mapNotNull { edge -> edge.samplePointAtFraction(fraction)?.toOffset() }
    if (candidateEdges.isEmpty()) return null
    val bestEdge = candidateEdges.maxByOrNull { edgePoint ->
        val edgeVector = edgePoint - anchorOffset
        edgeVector.x * sideNormal.x + edgeVector.y * sideNormal.y
    } ?: return null
    val edgeVector = bestEdge - anchorOffset
    val alignment = edgeVector.x * sideNormal.x + edgeVector.y * sideNormal.y
    return bestEdge.takeIf { alignment > 0.5f }
}

private fun List<SessionAnalysisFractionPointUi>.resolveAnchorSegmentIndex(anchorFraction: Float): Int {
    if (size < 2) return 1
    val nextIndex = indexOfFirst { point -> point.fraction >= anchorFraction }
        .let { index -> if (index == -1) lastIndex else index.coerceAtLeast(1) }
    return nextIndex.coerceIn(1, lastIndex)
}

private fun List<SessionAnalysisFractionPointUi>.isClosureSegmentNearAnchor(anchorSegmentIndex: Int): Boolean {
    if (size < 3) return true
    return anchorSegmentIndex <= trackCornerMarkerSkippedSegmentsRadius ||
        anchorSegmentIndex >= lastIndex - trackCornerMarkerSkippedSegmentsRadius
}

private fun Offset.distanceTo(other: Offset): Float {
    val dx = x - other.x
    val dy = y - other.y
    return sqrt((dx * dx) + (dy * dy))
}

private fun Offset.distanceToSegment(start: Offset, end: Offset): Float {
    val projection = projectPointOnSegment(pointer = this, start = start, end = end)
    val nearest = Offset(
        x = lerp(start.x, end.x, projection),
        y = lerp(start.y, end.y, projection),
    )
    return distanceTo(nearest)
}

private fun Offset.clampToViewport(canvasSize: IntSize, margin: Float): Offset {
    if (canvasSize.width <= 0 || canvasSize.height <= 0) return this
    return Offset(
        x = x.coerceIn(margin, canvasSize.width.toFloat() - margin),
        y = y.coerceIn(margin, canvasSize.height.toFloat() - margin),
    )
}
