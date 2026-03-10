package com.analyzer.trackmap.domain.usecase.editor

import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorGate
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSnapshot
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import dev.zacsweers.metro.Inject

@Inject
class TrackMapCalibrationEditorReducer(private val snapshotFactory: TrackMapCalibrationEditorSnapshotFactory) {

    fun selectMarker(snapshot: TrackMapCalibrationEditorSnapshot, gateId: String): TrackMapCalibrationEditorSnapshot {
        val selectedMarkerId = snapshot.gates.firstOrNull { it.id == gateId }?.id ?: snapshot.selectedMarkerId
        return snapshot.copy(selectedMarkerId = selectedMarkerId)
    }

    fun updateGate(
        item: TrackMapLibraryItem,
        snapshot: TrackMapCalibrationEditorSnapshot,
        gateId: String,
        gate: Gate,
    ): TrackMapCalibrationEditorSnapshot {
        val updatedGates = snapshot.gates.map { current ->
            if (current.id == gateId) current.copy(gate = gate) else current
        }
        return snapshotFactory.rebuild(
            item = item,
            referencePoint = snapshot.referencePoint,
            source = snapshot.source,
            gates = updatedGates,
            selectedMarkerId = gateId,
        )
    }

    fun addGateAfterSelected(
        item: TrackMapLibraryItem,
        snapshot: TrackMapCalibrationEditorSnapshot,
        gate: Gate,
    ): TrackMapCalibrationEditorSnapshot {
        if (snapshot.gates.isEmpty()) {
            return snapshotFactory.rebuild(
                item = item,
                referencePoint = snapshot.referencePoint,
                source = snapshot.source,
                gates = listOf(
                    TrackMapCalibrationEditorGate(
                        id = "sf",
                        order = 0,
                        shortLabel = "SF",
                        title = "Start / Finish",
                        gate = gate,
                    ),
                ),
                selectedMarkerOrder = 0,
            )
        }

        val selectedIndex = snapshot.gates.indexOfFirst { it.id == snapshot.selectedMarkerId }
            .takeIf { it >= 0 }
            ?: 0
        val insertIndex = (selectedIndex + 1).coerceAtMost(snapshot.gates.size)
        val candidate = TrackMapCalibrationEditorGate(
            id = "pending_$insertIndex",
            order = insertIndex,
            shortLabel = "NEW",
            title = "New Point",
            gate = gate,
        )
        val updated = snapshot.gates.toMutableList().apply {
            add(insertIndex, candidate)
        }
        return snapshotFactory.rebuild(
            item = item,
            referencePoint = snapshot.referencePoint,
            source = snapshot.source,
            gates = updated,
            selectedMarkerOrder = insertIndex,
        )
    }

    fun deleteGate(
        item: TrackMapLibraryItem,
        snapshot: TrackMapCalibrationEditorSnapshot,
        gateId: String,
    ): TrackMapCalibrationEditorSnapshot {
        if (gateId == "sf") return snapshot
        val remaining = snapshot.gates.filterNot { it.id == gateId }
        if (remaining.size == snapshot.gates.size || remaining.isEmpty()) return snapshot
        val selectedIndex = remaining.indexOfFirst { it.id == snapshot.selectedMarkerId }
            .takeIf { it >= 0 }
            ?: remaining.lastIndex
        return snapshotFactory.rebuild(
            item = item,
            referencePoint = snapshot.referencePoint,
            source = snapshot.source,
            gates = remaining,
            selectedMarkerOrder = selectedIndex,
        )
    }
}
