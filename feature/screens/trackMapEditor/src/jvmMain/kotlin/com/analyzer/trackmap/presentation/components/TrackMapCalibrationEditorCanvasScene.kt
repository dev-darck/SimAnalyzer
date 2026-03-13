package com.analyzer.trackmap.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationCanvasUiState
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationEditorMode
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationViewport
import com.analyzer.trackmap.presentation.model.TrackMapEditorGateUi
import com.analyzer.trackmap.presentation.model.TrackMapEditorMarkerUi
import com.analyzer.trackmap.presentation.model.TrackMapEditorSectorUi
import com.analyzer.trackmap.presentation.model.toTrackMapBounds
import com.analyzer.trackmap.presentation.model.toVec2

internal data class TrackMapCalibrationEditorCanvasScene(
    val isAddPointMode: Boolean,
    val editMode: TrackMapCalibrationEditorMode,
    val widthPx: Float,
    val heightPx: Float,
    val markers: List<TrackMapEditorMarkerUi>,
    val sectors: List<TrackMapEditorSectorUi>,
    val viewport: TrackMapCalibrationViewport,
    val metrics: TrackMapCalibrationPolylineMetrics,
    val trackBasePath: Path,
    val markerGeometries: Map<String, TrackMapCalibrationMarkerGeometry>,
    val markerScreenCenters: Map<String, Offset>,
    val sectorPathsByEndGateId: Map<String, Path>,
    val sectorScreenPointsByEndGateId: Map<String, List<Offset>>,
    val selectedMarker: TrackMapEditorMarkerUi?,
    val selectedGate: TrackMapEditorGateUi?,
    val selectedMarkerGeometry: TrackMapCalibrationMarkerGeometry?,
    val selectedSector: TrackMapEditorSectorUi?,
    val selectedSectorBadgePoint: Offset?,
    val livePositionOnScreen: Offset?,
    val previewAnchor: TrackMapCalibrationTrackAnchor?,
)

@Composable
internal fun rememberTrackMapCalibrationEditorCanvasScene(
    uiState: TrackMapCalibrationCanvasUiState,
    widthPx: Float,
    heightPx: Float,
    activeDrag: TrackMapCalibrationActiveDrag?,
): TrackMapCalibrationEditorCanvasScene? {
    val bounds = remember(uiState.bounds, uiState.points) {
        uiState.bounds ?: computeBounds(uiState.points)
    } ?: return null
    val viewport = remember(bounds, widthPx, heightPx) {
        TrackMapCalibrationViewport.fit(
            bounds = bounds.toTrackMapBounds(),
            widthPx = widthPx,
            heightPx = heightPx,
            paddingPx = 28f,
        )
    }
    val metrics = remember(
        uiState.points,
        uiState.leftWidthsMeters,
        uiState.rightWidthsMeters,
    ) {
        TrackMapCalibrationPolylineMetrics(uiState)
    }
    val trackBasePath = remember(uiState.points, viewport) {
        buildPath(uiState.points, viewport)
    }
    val selectedMarker = remember(uiState.markers, uiState.selectedMarkerId) {
        uiState.markers.firstOrNull { it.gateId == uiState.selectedMarkerId } ?: uiState.markers.firstOrNull()
    }
    val gatesById = remember(uiState.gates) {
        uiState.gates.associateBy(TrackMapEditorGateUi::id)
    }
    val selectedGate = remember(selectedMarker, gatesById) {
        selectedMarker?.let { marker -> gatesById[marker.gateId] }
    }
    val displayedSelectedGate = remember(activeDrag, selectedGate, selectedMarker) {
        activeDrag
            ?.takeIf { drag -> drag.gateId == selectedMarker?.gateId }
            ?.previewGate
            ?: selectedGate
    }
    val markerGeometries = remember(uiState.markers, gatesById, metrics) {
        uiState.markers.associateBy(
            keySelector = TrackMapEditorMarkerUi::gateId,
            valueTransform = { marker ->
                metrics.buildMarkerGeometry(
                    marker = marker,
                    gate = gatesById[marker.gateId],
                )
            },
        )
    }
    val selectedMarkerGeometry = remember(selectedMarker, displayedSelectedGate, metrics) {
        selectedMarker?.let { marker ->
            metrics.buildMarkerGeometry(
                marker = marker,
                gate = displayedSelectedGate,
            )
        }
    }
    val markerScreenCenters = remember(markerGeometries, selectedMarkerGeometry, viewport) {
        buildMap {
            markerGeometries.values.forEach { geometry ->
                put(geometry.gateId, viewport.worldToScreen(geometry.center))
            }
            selectedMarkerGeometry?.let { geometry ->
                put(geometry.gateId, viewport.worldToScreen(geometry.center))
            }
        }
    }
    val sectorGeometriesByEndGateId = remember(uiState.sectors, uiState.markers, metrics) {
        buildMap {
            uiState.sectors.forEach { sector ->
                val startMarker = uiState.markers.firstOrNull { it.gateId == sector.startGateId } ?: return@forEach
                val endMarker = uiState.markers.firstOrNull { it.gateId == sector.endGateId } ?: return@forEach
                put(
                    sector.endGateId,
                    metrics.buildSectorGeometry(
                        startMarker = startMarker,
                        endMarker = endMarker,
                        color = Color(sector.colorHex),
                        name = sector.name,
                    ),
                )
            }
        }
    }
    val sectorPathsByEndGateId = remember(sectorGeometriesByEndGateId, viewport) {
        sectorGeometriesByEndGateId.mapValues { (_, geometry) ->
            buildWorldPath(geometry.pathPoints, viewport)
        }
    }
    val sectorScreenPointsByEndGateId = remember(sectorGeometriesByEndGateId, viewport) {
        sectorGeometriesByEndGateId.mapValues { (_, geometry) ->
            geometry.pathPoints.map(viewport::worldToScreen)
        }
    }
    val selectedSector = remember(uiState.sectors, selectedMarker) {
        selectedMarker?.let { marker ->
            uiState.sectors.firstOrNull { it.endGateId == marker.gateId }
        }
    }
    val selectedSectorBadgePoint = remember(selectedSector, sectorGeometriesByEndGateId, viewport) {
        selectedSector
            ?.let { sector -> sectorGeometriesByEndGateId[sector.endGateId] }
            ?.labelPoint
            ?.let(viewport::worldToScreen)
    }
    val livePositionOnScreen = remember(uiState.livePosition, viewport) {
        uiState.livePosition?.toVec2()?.let(viewport::worldToScreen)
    }
    val previewAnchor = remember(activeDrag, metrics) {
        activeDrag?.anchorPointIndex?.let(metrics::anchorAt)
    }

    return TrackMapCalibrationEditorCanvasScene(
        isAddPointMode = uiState.isAddPointMode,
        editMode = uiState.editMode,
        widthPx = widthPx,
        heightPx = heightPx,
        markers = uiState.markers,
        sectors = uiState.sectors,
        viewport = viewport,
        metrics = metrics,
        trackBasePath = trackBasePath,
        markerGeometries = markerGeometries,
        markerScreenCenters = markerScreenCenters,
        sectorPathsByEndGateId = sectorPathsByEndGateId,
        sectorScreenPointsByEndGateId = sectorScreenPointsByEndGateId,
        selectedMarker = selectedMarker,
        selectedGate = selectedGate,
        selectedMarkerGeometry = selectedMarkerGeometry,
        selectedSector = selectedSector,
        selectedSectorBadgePoint = selectedSectorBadgePoint,
        livePositionOnScreen = livePositionOnScreen,
        previewAnchor = previewAnchor,
    )
}
