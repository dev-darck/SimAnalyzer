package com.analyzer.trackmap.presentation.model

import androidx.compose.runtime.Immutable
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorGate
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorMarker
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSector
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.presentation.format.formatMeters
import com.analyzer.trackmap.presentation.state.TrackMapCalibrationEditorState
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

@Immutable
internal data class TrackMapMarkerRowUi(
    val gateId: String,
    val title: String,
    val startLabel: String,
    val endLabel: String,
    val colorHex: Long,
)

@Immutable
internal data class TrackMapEditorPointUi(val x: Float, val y: Float)

@Immutable
internal data class TrackMapEditorBoundsUi(
    val minX: Float,
    val minY: Float,
    val maxX: Float,
    val maxY: Float,
)

@Immutable
internal data class TrackMapEditorGateUi(
    val id: String,
    val title: String,
    val center: TrackMapEditorPointUi,
    val forward: TrackMapEditorPointUi,
    val normal: TrackMapEditorPointUi,
    val halfWidthMeters: Float,
)

@Immutable
internal data class TrackMapEditorMarkerUi(
    val gateId: String,
    val title: String,
    val colorHex: Long,
    val meters: Float,
    val pointIndex: Int,
)

@Immutable
internal data class TrackMapEditorSectorUi(
    val name: String,
    val colorHex: Long,
    val startGateId: String,
    val endGateId: String,
    val startPointIndex: Int,
    val endPointIndex: Int,
)

@Immutable
internal data class TrackMapCalibrationCanvasUiState(
    val points: ImmutableList<TrackMapEditorPointUi> = persistentListOf(),
    val leftWidthsMeters: ImmutableList<Float> = persistentListOf(),
    val rightWidthsMeters: ImmutableList<Float> = persistentListOf(),
    val bounds: TrackMapEditorBoundsUi? = null,
    val markers: ImmutableList<TrackMapEditorMarkerUi> = persistentListOf(),
    val sectors: ImmutableList<TrackMapEditorSectorUi> = persistentListOf(),
    val gates: ImmutableList<TrackMapEditorGateUi> = persistentListOf(),
    val selectedMarkerId: String? = null,
    val livePosition: TrackMapEditorPointUi? = null,
    val editMode: TrackMapCalibrationEditorMode = TrackMapCalibrationEditorMode.Move,
    val isAddPointMode: Boolean = false,
)

@Immutable
internal data class TrackMapCalibrationWorkspaceUiState(
    val title: String,
    val subtitle: String,
    val sourceLabel: String,
    val markerCount: Int,
    val canReset: Boolean,
    val addPointHint: String? = null,
    val editMode: TrackMapCalibrationEditorMode = TrackMapCalibrationEditorMode.Move,
    val canvas: TrackMapCalibrationCanvasUiState = TrackMapCalibrationCanvasUiState(),
)

@Immutable
internal data class TrackMapCalibrationMarkerPanelUiState(
    val markerRows: ImmutableList<TrackMapMarkerRowUi> = persistentListOf(),
    val selectedMarkerId: String? = null,
    val isAddPointMode: Boolean = false,
)

@Immutable
internal data class TrackMapCalibrationInspectorUiState(
    val selectedRow: TrackMapMarkerRowUi? = null,
    val addPointHint: String? = null,
    val message: String? = null,
    val saveLabel: String = "Save",
    val canSave: Boolean = false,
    val deleteGateId: String? = null,
)

@Immutable
internal data class TrackMapCalibrationSidebarUiState(
    val markerPanel: TrackMapCalibrationMarkerPanelUiState = TrackMapCalibrationMarkerPanelUiState(),
    val inspector: TrackMapCalibrationInspectorUiState = TrackMapCalibrationInspectorUiState(),
)

internal fun TrackMapCalibrationEditorState.toWorkspaceUiState(
    item: TrackMapLibraryItem,
    editMode: TrackMapCalibrationEditorMode,
    isAddPointMode: Boolean,
): TrackMapCalibrationWorkspaceUiState = TrackMapCalibrationWorkspaceUiState(
    title = item.map.trackName,
    subtitle = item.toWorkspaceSubtitle(),
    sourceLabel = source?.name ?: "NEW",
    markerCount = markers.size,
    canReset = canReset,
    addPointHint = if (isAddPointMode) {
        if (markers.isEmpty()) {
            "Click on the track to place Start / Finish"
        } else {
            "Click on the track to insert a new marker"
        }
    } else {
        null
    },
    editMode = editMode,
    canvas = toCanvasUiState(
        item = item,
        editMode = editMode,
        isAddPointMode = isAddPointMode,
    ),
)

internal fun TrackMapCalibrationEditorState.toSidebarUiState(isAddPointMode: Boolean): TrackMapCalibrationSidebarUiState {
    val markerRows = buildTrackMapMarkerRows(markers)
    val selectedRow = markerRows.firstOrNull { it.gateId == selectedMarkerId } ?: markerRows.firstOrNull()
    val selectedMarker = markers.firstOrNull { it.gateId == selectedRow?.gateId }

    return TrackMapCalibrationSidebarUiState(
        markerPanel = TrackMapCalibrationMarkerPanelUiState(
            markerRows = markerRows,
            selectedMarkerId = selectedMarkerId,
            isAddPointMode = isAddPointMode,
        ),
        inspector = TrackMapCalibrationInspectorUiState(
            selectedRow = selectedRow,
            addPointHint = if (isAddPointMode && selectedRow != null) {
                if (selectedMarker?.gateId == "sf" && markers.size == 1) {
                    "Click on the track to insert the first sector boundary after Start / Finish."
                } else {
                    "Click on the track to insert a new marker after ${selectedRow.title}."
                }
            } else {
                null
            },
            message = message,
            saveLabel = if (isSaving) "Saving..." else "Save",
            canSave = canSave,
            deleteGateId = selectedMarker?.gateId?.takeUnless { it == "sf" },
        ),
    )
}

internal fun TrackMapCalibrationEditorState.toCanvasUiState(
    item: TrackMapLibraryItem,
    editMode: TrackMapCalibrationEditorMode,
    isAddPointMode: Boolean,
): TrackMapCalibrationCanvasUiState = TrackMapCalibrationCanvasUiState(
    points = item.points.map(Vec2::toTrackMapEditorPointUi).toImmutableList(),
    leftWidthsMeters = item.leftWidthsMeters.toImmutableList(),
    rightWidthsMeters = item.rightWidthsMeters.toImmutableList(),
    bounds = item.bounds?.toTrackMapEditorBoundsUi(),
    markers = markers.map(TrackMapCalibrationEditorMarker::toTrackMapEditorMarkerUi).toImmutableList(),
    sectors = sectors.map(TrackMapCalibrationEditorSector::toTrackMapEditorSectorUi).toImmutableList(),
    gates = gates.map(TrackMapCalibrationEditorGate::toTrackMapEditorGateUi).toImmutableList(),
    selectedMarkerId = selectedMarkerId,
    livePosition = livePosition?.toTrackMapEditorPointUi(),
    editMode = editMode,
    isAddPointMode = isAddPointMode,
)

internal fun buildTrackMapMarkerRows(markers: List<TrackMapCalibrationEditorMarker>): ImmutableList<TrackMapMarkerRowUi> =
    markers.mapIndexed { index, marker ->
        val previousMarker = if (markers.isEmpty()) {
            null
        } else {
            markers.getOrNull(if (index == 0) markers.lastIndex else index - 1)
        }
        TrackMapMarkerRowUi(
            gateId = marker.gateId,
            title = marker.title,
            startLabel = formatMeters(previousMarker?.meters ?: marker.meters),
            endLabel = formatMeters(marker.meters),
            colorHex = marker.colorHex,
        )
    }.toImmutableList()

internal fun TrackMapMarkerRowUi.color(): androidx.compose.ui.graphics.Color =
    androidx.compose.ui.graphics.Color(colorHex)

internal fun TrackMapEditorPointUi.toVec2(): Vec2 = Vec2(x = x, y = y)

internal fun Vec2.toTrackMapEditorPointUi(): TrackMapEditorPointUi = TrackMapEditorPointUi(x = x, y = y)

internal fun TrackMapBounds.toTrackMapEditorBoundsUi(): TrackMapEditorBoundsUi = TrackMapEditorBoundsUi(
    minX = minX,
    minY = minY,
    maxX = maxX,
    maxY = maxY,
)

internal fun TrackMapEditorBoundsUi.toTrackMapBounds(): TrackMapBounds = TrackMapBounds(
    minX = minX,
    minY = minY,
    maxX = maxX,
    maxY = maxY,
)

internal fun Gate.toTrackMapEditorGateUi(id: String, title: String): TrackMapEditorGateUi = TrackMapEditorGateUi(
    id = id,
    title = title,
    center = centerV2().toTrackMapEditorPointUi(),
    forward = forwardV2().toTrackMapEditorPointUi(),
    normal = normalV2().toTrackMapEditorPointUi(),
    halfWidthMeters = halfWidthMeters,
)

internal fun TrackMapCalibrationEditorGate.toTrackMapEditorGateUi(): TrackMapEditorGateUi =
    gate.toTrackMapEditorGateUi(id = id, title = title)

internal fun TrackMapEditorGateUi.toGate(): Gate = Gate.create(
    center = center.toVec2(),
    forward = forward.toVec2(),
    normal = normal.toVec2(),
    halfWidthMeters = halfWidthMeters,
)

private fun TrackMapCalibrationEditorMarker.toTrackMapEditorMarkerUi(): TrackMapEditorMarkerUi = TrackMapEditorMarkerUi(
    gateId = gateId,
    title = title,
    colorHex = colorHex,
    meters = meters,
    pointIndex = pointIndex,
)

private fun TrackMapCalibrationEditorSector.toTrackMapEditorSectorUi(): TrackMapEditorSectorUi = TrackMapEditorSectorUi(
    name = name,
    colorHex = colorHex,
    startGateId = startGateId,
    endGateId = endGateId,
    startPointIndex = startPointIndex,
    endPointIndex = endPointIndex,
)

private fun TrackMapLibraryItem.toWorkspaceSubtitle(): String {
    val layout = map.layoutId?.takeIf(String::isNotBlank)
    return buildList {
        add(map.gameId.ifBlank { "unknown" })
        add(map.trackId)
        layout?.let(::add)
    }.joinToString(" / ")
}
