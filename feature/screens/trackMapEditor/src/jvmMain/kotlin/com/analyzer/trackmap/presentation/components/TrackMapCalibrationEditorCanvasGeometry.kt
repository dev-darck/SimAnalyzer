package com.analyzer.trackmap.presentation.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationCanvasUiState
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationViewport
import com.analyzer.trackmap.presentation.model.TrackMapEditorBoundsUi
import com.analyzer.trackmap.presentation.model.TrackMapEditorGateUi
import com.analyzer.trackmap.presentation.model.TrackMapEditorMarkerUi
import com.analyzer.trackmap.presentation.model.TrackMapEditorPointUi
import com.analyzer.trackmap.presentation.model.TrackMapEditorSectorUi
import com.analyzer.trackmap.presentation.model.toTrackMapEditorPointUi
import com.analyzer.trackmap.presentation.model.toVec2
import com.project.analyzer.math.Vec2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.sqrt

internal enum class TrackMapCalibrationHandleType {
    Center,
    Direction,
    Width,
}

internal data class TrackMapCalibrationActiveDrag(
    val gateId: String,
    val handleType: TrackMapCalibrationHandleType,
    val position: Offset,
    val previewGate: TrackMapEditorGateUi,
    val anchorPointIndex: Int? = null,
)

internal class TrackMapCalibrationPolylineMetrics(uiState: TrackMapCalibrationCanvasUiState) {

    private val points: List<Vec2> = uiState.points.map(TrackMapEditorPointUi::toVec2)
    private val leftWidths: List<Float> = uiState.leftWidthsMeters
    private val rightWidths: List<Float> = uiState.rightWidthsMeters
    private val cumulativeDistances: FloatArray = FloatArray(points.size)

    init {
        var distance = 0f
        for (index in points.indices) {
            if (index > 0) {
                distance += points[index - 1].distanceTo(points[index])
            }
            cumulativeDistances[index] = distance
        }
    }

    fun anchorAt(pointIndex: Int): TrackMapCalibrationTrackAnchor {
        val index = pointIndex.coerceIn(0, points.lastIndex)
        val center = points[index]
        val previous = points[(index - 1).coerceAtLeast(0)]
        val next = points[(index + 1).coerceAtMost(points.lastIndex)]
        val tangent = (next - previous).safeNormalized(Vec2.Right)
        val normal = tangent.perpLeft().safeNormalized(Vec2.Up)
        val halfWidth = max(
            max(leftWidths.getOrElse(index) { 0f }, rightWidths.getOrElse(index) { 0f }),
            4f,
        )
        return TrackMapCalibrationTrackAnchor(
            pointIndex = index,
            center = center,
            forward = tangent,
            normal = normal,
            halfWidthMeters = halfWidth,
            distanceMeters = cumulativeDistances[index],
        )
    }

    fun nearestAnchor(world: Vec2, hintPointIndex: Int? = null): TrackMapCalibrationTrackAnchor {
        val hintedIndex = hintPointIndex?.let { hint ->
            val candidate = nearestAnchorIndexInRange(
                world = world,
                startIndex = (hint - 96).coerceAtLeast(0),
                endIndex = (hint + 96).coerceAtMost(points.lastIndex),
            )
            val candidateDistance = points[candidate].distanceTo(world)
            if (candidateDistance <= 48f) candidate else null
        }
        val bestIndex = hintedIndex ?: nearestAnchorIndexInRange(
            world = world,
            startIndex = 0,
            endIndex = points.lastIndex,
        )
        return anchorAt(bestIndex)
    }

    fun buildMovedGate(
        anchor: TrackMapCalibrationTrackAnchor,
        currentGate: TrackMapEditorGateUi,
    ): TrackMapEditorGateUi {
        val forward = currentGate.forward.toVec2().safeNormalized(anchor.forward)
        val normal = deriveNormal(
            forward = forward,
            preferred = currentGate.normal.toVec2(),
        )
        return currentGate.copy(
            center = anchor.center.toTrackMapEditorPointUi(),
            forward = forward.toTrackMapEditorPointUi(),
            normal = normal.toTrackMapEditorPointUi(),
            halfWidthMeters = currentGate.halfWidthMeters.coerceAtLeast(4f),
        )
    }

    fun directionHandleAnchor(
        currentGate: TrackMapEditorGateUi,
        fallbackAnchor: TrackMapCalibrationTrackAnchor,
    ): TrackMapCalibrationTrackAnchor {
        val forward = currentGate.forward.toVec2().safeNormalized(fallbackAnchor.forward)
        val step = when {
            points.size >= 400 -> 8
            points.size >= 200 -> 6
            else -> 4
        }
        val direction = if (forward.dot(fallbackAnchor.forward) >= 0f) 1 else -1
        return anchorAt(wrapIndex(fallbackAnchor.pointIndex + direction * step))
    }

    fun buildRotatedGate(
        currentGate: TrackMapEditorGateUi,
        targetAnchor: TrackMapCalibrationTrackAnchor,
    ): TrackMapEditorGateUi {
        val center = currentGate.center.toVec2()
        val currentForward = currentGate.forward.toVec2().safeNormalized(targetAnchor.forward)
        val offset = targetAnchor.center - center
        val trackForward = if (offset.len2() <= 0.25f) {
            if (targetAnchor.forward.dot(currentForward) >= 0f) {
                targetAnchor.forward
            } else {
                targetAnchor.forward * -1f
            }
        } else if (offset.dot(targetAnchor.forward) >= 0f) {
            targetAnchor.forward
        } else {
            targetAnchor.forward * -1f
        }
        val forward = trackForward.safeNormalized(currentForward)
        val normal = deriveNormal(
            forward = forward,
            preferred = currentGate.normal.toVec2(),
        )
        return currentGate.copy(
            center = center.toTrackMapEditorPointUi(),
            forward = forward.toTrackMapEditorPointUi(),
            normal = normal.toTrackMapEditorPointUi(),
            halfWidthMeters = currentGate.halfWidthMeters.coerceAtLeast(4f),
        )
    }

    fun buildResizedGate(currentGate: TrackMapEditorGateUi, worldTarget: Vec2): TrackMapEditorGateUi {
        val center = currentGate.center.toVec2()
        val forward = currentGate.forward.toVec2().safeNormalized(Vec2.Right)
        val normal = deriveNormal(
            forward = forward,
            preferred = currentGate.normal.toVec2(),
        )
        val halfWidth = snapHalfWidthMeters(
            kotlin.math.abs((worldTarget - center).dot(normal)),
        ).coerceIn(2f, 80f)
        return currentGate.copy(
            center = center.toTrackMapEditorPointUi(),
            forward = forward.toTrackMapEditorPointUi(),
            normal = normal.toTrackMapEditorPointUi(),
            halfWidthMeters = halfWidth,
        )
    }

    fun pathBetween(startIndex: Int, endIndex: Int): List<Vec2> {
        if (points.isEmpty()) return emptyList()
        if (startIndex == endIndex) return listOf(points[startIndex])
        val result = ArrayList<Vec2>()
        var index = startIndex.coerceIn(0, points.lastIndex)
        val safeEnd = endIndex.coerceIn(0, points.lastIndex)
        result += points[index]
        while (index != safeEnd) {
            index = (index + 1) % points.size
            result += points[index]
            if (result.size > points.size + 1) break
        }
        return result
    }

    fun buildMarkerGeometry(
        marker: TrackMapEditorMarkerUi,
        gate: TrackMapEditorGateUi?,
    ): TrackMapCalibrationMarkerGeometry {
        val anchor = anchorAt(marker.pointIndex)
        return TrackMapCalibrationMarkerGeometry(
            gateId = marker.gateId,
            title = marker.title,
            color = Color(marker.colorHex),
            center = gate?.center?.toVec2() ?: anchor.center,
            forward = gate?.forward?.toVec2()?.safeNormalized(anchor.forward) ?: anchor.forward,
            normal = gate?.normal?.toVec2()?.safeNormalized(anchor.normal) ?: anchor.normal,
            halfWidthMeters = gate?.halfWidthMeters?.coerceAtLeast(4f) ?: anchor.halfWidthMeters,
            anchor = anchor,
        )
    }

    fun buildSectorGeometry(
        startMarker: TrackMapEditorMarkerUi,
        endMarker: TrackMapEditorMarkerUi,
        color: Color,
        name: String,
    ): TrackMapCalibrationSectorGeometry = TrackMapCalibrationSectorGeometry(
        name = name,
        color = color,
        startGateId = startMarker.gateId,
        endGateId = endMarker.gateId,
        startAnchor = anchorAt(startMarker.pointIndex),
        endAnchor = anchorAt(endMarker.pointIndex),
        labelPoint = pathBetween(startMarker.pointIndex, endMarker.pointIndex).let { points ->
            points[points.size / 2]
        },
        pathPoints = pathBetween(startMarker.pointIndex, endMarker.pointIndex),
    )

    private fun wrapIndex(index: Int): Int {
        if (points.isEmpty()) return 0
        val size = points.size
        val wrapped = index % size
        return if (wrapped >= 0) wrapped else wrapped + size
    }

    private fun nearestAnchorIndexInRange(world: Vec2, startIndex: Int, endIndex: Int): Int {
        var bestIndex = startIndex
        var bestDistance = Float.MAX_VALUE
        for (index in startIndex..endIndex) {
            val distance = points[index].distanceTo(world)
            if (distance < bestDistance) {
                bestDistance = distance
                bestIndex = index
            }
        }
        return bestIndex
    }

    private fun deriveNormal(forward: Vec2, preferred: Vec2): Vec2 {
        val left = forward.perpLeft().safeNormalized(Vec2.Up)
        return if (left.dot(preferred) >= 0f) {
            left
        } else {
            forward.perpRight().safeNormalized(Vec2.Up)
        }
    }

    private fun snapHalfWidthMeters(value: Float): Float {
        val snapped = round(value * 2f) / 2f
        return snapped.coerceAtLeast(2f)
    }
}

internal data class TrackMapCalibrationTrackAnchor(
    val pointIndex: Int,
    val center: Vec2,
    val forward: Vec2,
    val normal: Vec2,
    val halfWidthMeters: Float,
    val distanceMeters: Float,
)

internal data class TrackMapCalibrationSectorGeometry(
    val name: String,
    val color: Color,
    val startGateId: String,
    val endGateId: String,
    val startAnchor: TrackMapCalibrationTrackAnchor,
    val endAnchor: TrackMapCalibrationTrackAnchor,
    val labelPoint: Vec2,
    val pathPoints: List<Vec2>,
)

internal data class TrackMapCalibrationMarkerGeometry(
    val gateId: String,
    val title: String,
    val color: Color,
    val center: Vec2,
    val forward: Vec2,
    val normal: Vec2,
    val halfWidthMeters: Float,
    val anchor: TrackMapCalibrationTrackAnchor,
) {

    val directionHandlePoint: Vec2
        get() = center + forward * max(halfWidthMeters * 1.35f, 10f)

    val widthHandlePoint: Vec2
        get() = center + normal * halfWidthMeters
}

internal fun hitTestMarker(
    offset: Offset,
    markers: List<TrackMapEditorMarkerUi>,
    markerScreenCenters: Map<String, Offset>,
): TrackMapEditorMarkerUi? {
    var bestMarker: TrackMapEditorMarkerUi? = null
    var bestDistance = Float.MAX_VALUE
    markers.forEach { marker ->
        val center = markerScreenCenters[marker.gateId] ?: return@forEach
        val markerDistance = distance(offset, center)
        if (markerDistance < bestDistance) {
            bestDistance = markerDistance
            bestMarker = marker
        }
    }
    return bestMarker?.takeIf { bestDistance <= 24f }
}

internal fun hitTestSector(
    offset: Offset,
    sectors: List<TrackMapEditorSectorUi>,
    sectorScreenPointsByEndGateId: Map<String, List<Offset>>,
): TrackMapEditorSectorUi? {
    var best: TrackMapEditorSectorUi? = null
    var bestDistance = Float.MAX_VALUE
    sectors.forEach { sector ->
        val pathPoints = sectorScreenPointsByEndGateId[sector.endGateId] ?: return@forEach
        for (index in 1 until pathPoints.size) {
            val start = pathPoints[index - 1]
            val end = pathPoints[index]
            val candidateDistance = distanceToSegment(offset, start, end)
            if (candidateDistance < bestDistance) {
                bestDistance = candidateDistance
                best = sector
            }
        }
    }
    return best?.takeIf { bestDistance <= 22f }
}

internal fun buildPath(points: List<TrackMapEditorPointUi>, viewport: TrackMapCalibrationViewport): Path {
    val path = Path()
    points.forEachIndexed { index, point ->
        val mapped = viewport.worldToScreen(point.toVec2())
        if (index == 0) {
            path.moveTo(mapped.x, mapped.y)
        } else {
            path.lineTo(mapped.x, mapped.y)
        }
    }
    return path
}

internal fun buildWorldPath(points: List<Vec2>, viewport: TrackMapCalibrationViewport): Path {
    val path = Path()
    points.forEachIndexed { index, point ->
        val mapped = viewport.worldToScreen(point)
        if (index == 0) {
            path.moveTo(mapped.x, mapped.y)
        } else {
            path.lineTo(mapped.x, mapped.y)
        }
    }
    return path
}

internal fun computeBounds(points: List<TrackMapEditorPointUi>): TrackMapEditorBoundsUi? {
    if (points.isEmpty()) return null
    var minX = points.first().x
    var minY = points.first().y
    var maxX = points.first().x
    var maxY = points.first().y
    points.forEach { point ->
        minX = min(minX, point.x)
        minY = min(minY, point.y)
        maxX = max(maxX, point.x)
        maxY = max(maxY, point.y)
    }
    return TrackMapEditorBoundsUi(minX = minX, minY = minY, maxX = maxX, maxY = maxY)
}

internal fun clampToCanvas(point: Offset, widthPx: Float, heightPx: Float): Offset = Offset(
    x = point.x.coerceIn(0f, widthPx),
    y = point.y.coerceIn(0f, heightPx),
)

private fun distanceToSegment(point: Offset, start: Offset, end: Offset): Float {
    val segment = end - start
    val segmentLengthSquared = segment.x * segment.x + segment.y * segment.y
    if (segmentLengthSquared <= 0.0001f) {
        return distance(point, start)
    }
    val t = (((point.x - start.x) * segment.x) + ((point.y - start.y) * segment.y)) / segmentLengthSquared
    val clampedT = t.coerceIn(0f, 1f)
    val projection = Offset(
        x = start.x + segment.x * clampedT,
        y = start.y + segment.y * clampedT,
    )
    return distance(point, projection)
}

private fun distance(a: Offset, b: Offset): Float {
    val dx = a.x - b.x
    val dy = a.y - b.y
    return sqrt(dx * dx + dy * dy)
}
