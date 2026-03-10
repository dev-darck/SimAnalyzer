package com.analyzer.trackmap.domain.usecase.editor

import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorGate
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorMarker
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSector
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSnapshot
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import dev.zacsweers.metro.Inject

@Inject
class TrackMapCalibrationEditorSnapshotFactory {

    fun createEmpty(item: TrackMapLibraryItem): TrackMapCalibrationEditorSnapshot = TrackMapCalibrationEditorSnapshot(
        referencePoint = item.map.referencePoint,
        source = null,
        gates = emptyList(),
        markers = emptyList(),
        sectors = emptyList(),
        selectedMarkerId = null,
    )

    fun createLoaded(item: TrackMapLibraryItem, calibration: TrackCalibration): TrackMapCalibrationEditorSnapshot =
        rebuild(
            item = item,
            referencePoint = calibration.referencePoint,
            source = calibration.source,
            gates = calibration.toEditorGates(),
            selectedMarkerId = calibration.toEditorGates().firstOrNull()?.id,
        )

    fun rebuild(
        item: TrackMapLibraryItem,
        referencePoint: ReferencePoint,
        source: TrackCalibrationSource?,
        gates: List<TrackMapCalibrationEditorGate>,
        selectedMarkerId: String? = null,
        selectedMarkerOrder: Int? = null,
    ): TrackMapCalibrationEditorSnapshot {
        val normalizedGates = normalizeEditorGates(gates)
        val markers = buildEditorMarkers(item = item, gates = normalizedGates)
        val sectors = buildEditorSectors(markers = markers)
        val resolvedSelection = when {
            selectedMarkerOrder != null -> normalizedGates.getOrNull(selectedMarkerOrder)?.id
            selectedMarkerId != null && normalizedGates.any { it.id == selectedMarkerId } -> selectedMarkerId
            else -> normalizedGates.firstOrNull()?.id
        }
        return TrackMapCalibrationEditorSnapshot(
            referencePoint = referencePoint,
            source = source,
            gates = normalizedGates,
            markers = markers,
            sectors = sectors,
            selectedMarkerId = resolvedSelection,
        )
    }

    fun toTrackCalibration(item: TrackMapLibraryItem, snapshot: TrackMapCalibrationEditorSnapshot): TrackCalibration {
        val orderedGates = snapshot.gates.sortedBy(TrackMapCalibrationEditorGate::order)
        val startFinish = orderedGates.first().gate
        val sectorCount = orderedGates.size
        val sectors = (1..sectorCount).map { sectorIndex ->
            val startGate = if (sectorIndex == 1) {
                startFinish
            } else {
                orderedGates[sectorIndex - 1].gate
            }
            val finishGate = if (sectorIndex == sectorCount) {
                startFinish
            } else {
                orderedGates[sectorIndex].gate
            }
            SectorCalibration(
                index = sectorIndex,
                start = startGate,
                finish = finishGate,
            )
        }
        return TrackCalibration(
            trackId = item.map.trackId,
            trackName = item.map.trackName,
            layoutId = item.map.layoutId,
            createdAtEpochMs = System.currentTimeMillis(),
            source = TrackCalibrationSource.USER,
            referencePoint = snapshot.referencePoint,
            startFinish = startFinish,
            sectors = sectors,
        )
    }
}

private fun TrackCalibration.toEditorGates(): List<TrackMapCalibrationEditorGate> {
    val result = ArrayList<TrackMapCalibrationEditorGate>(sectors.size.coerceAtLeast(1))
    result += TrackMapCalibrationEditorGate(
        id = "sf",
        order = 0,
        shortLabel = "SF",
        title = "Start / Finish",
        gate = startFinish,
    )
    for (sectorIndex in 1 until sectors.size) {
        result += TrackMapCalibrationEditorGate(
            id = "s$sectorIndex",
            order = sectorIndex,
            shortLabel = "S$sectorIndex",
            title = "Finish Sector $sectorIndex",
            gate = sectors[sectorIndex - 1].finish,
        )
    }
    return result
}

private fun normalizeEditorGates(gates: List<TrackMapCalibrationEditorGate>): List<TrackMapCalibrationEditorGate> {
    val startFinish = gates.firstOrNull() ?: return emptyList()
    val normalized = ArrayList<TrackMapCalibrationEditorGate>(gates.size)
    normalized += startFinish.copy(
        id = "sf",
        order = 0,
        shortLabel = "SF",
        title = "Start / Finish",
    )
    gates.drop(1).forEachIndexed { index, gate ->
        val sectorIndex = index + 1
        normalized += gate.copy(
            id = "s$sectorIndex",
            order = sectorIndex,
            shortLabel = "S$sectorIndex",
            title = "Finish Sector $sectorIndex",
        )
    }
    return normalized
}

private fun buildEditorMarkers(
    item: TrackMapLibraryItem,
    gates: List<TrackMapCalibrationEditorGate>,
): List<TrackMapCalibrationEditorMarker> {
    if (item.points.isEmpty() || gates.isEmpty()) return emptyList()
    val colors = listOf(
        0xFF4EA1FF,
        0xFFF6C544,
        0xFFB06CFF,
        0xFF25D2A4,
        0xFFFF5277,
        0xFF38D0FF,
    )
    val anchors = gates.associate { gate ->
        gate.id to nearestPointIndex(item.points, gate.gate.centerV2())
    }
    return gates.mapIndexed { index, gate ->
        val pointIndex = anchors[gate.id] ?: 0
        TrackMapCalibrationEditorMarker(
            gateId = gate.id,
            order = gate.order,
            shortLabel = gate.shortLabel,
            title = buildMarkerName(gate = gate, index = index),
            colorHex = colors[index % colors.size],
            meters = cumulativeDistance(item.points, pointIndex),
            pointIndex = pointIndex,
        )
    }
}

private fun buildEditorSectors(markers: List<TrackMapCalibrationEditorMarker>): List<TrackMapCalibrationEditorSector> {
    if (markers.isEmpty()) return emptyList()
    return markers.indices.map { index ->
        val startMarker = markers[index]
        val endMarker = markers[(index + 1) % markers.size]
        TrackMapCalibrationEditorSector(
            index = index + 1,
            name = buildSectorName(endMarker = endMarker, sectorIndex = index + 1),
            colorHex = endMarker.colorHex,
            startGateId = startMarker.gateId,
            endGateId = endMarker.gateId,
            startMeters = startMarker.meters,
            endMeters = endMarker.meters,
            startPointIndex = startMarker.pointIndex,
            endPointIndex = endMarker.pointIndex,
        )
    }
}

private fun buildMarkerName(gate: TrackMapCalibrationEditorGate, index: Int): String {
    if (gate.id == "sf") return "Start / Finish"
    return gate.title
        .removePrefix("Finish ")
        .takeIf(String::isNotBlank)
        ?: "Marker ${index + 1}"
}

private fun buildSectorName(endMarker: TrackMapCalibrationEditorMarker, sectorIndex: Int): String {
    if (endMarker.gateId == "sf") return "Final Sector"
    return endMarker.title
        .takeIf(String::isNotBlank)
        ?: "Sector $sectorIndex"
}

private fun nearestPointIndex(points: List<Vec2>, target: Vec2): Int {
    var bestIndex = 0
    var bestDistance = Float.MAX_VALUE
    points.forEachIndexed { index, point ->
        val distance = point.distanceTo(target)
        if (distance < bestDistance) {
            bestDistance = distance
            bestIndex = index
        }
    }
    return bestIndex
}

private fun cumulativeDistance(points: List<Vec2>, endIndexInclusive: Int): Float {
    if (points.size < 2 || endIndexInclusive <= 0) return 0f
    val clampedEnd = endIndexInclusive.coerceAtMost(points.lastIndex)
    var distance = 0f
    for (index in 1..clampedEnd) {
        distance += points[index - 1].distanceTo(points[index])
    }
    return distance
}
