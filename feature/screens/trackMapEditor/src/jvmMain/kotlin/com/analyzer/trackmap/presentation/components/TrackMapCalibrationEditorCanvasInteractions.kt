package com.analyzer.trackmap.presentation.components

import androidx.compose.ui.geometry.Offset
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationEditorMode
import com.analyzer.trackmap.presentation.model.TrackMapEditorGateUi
import com.analyzer.trackmap.presentation.model.toGate
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate

internal fun TrackMapCalibrationEditorCanvasScene.handleTap(
    offset: Offset,
    onAddPoint: (Gate) -> Unit,
    onSelectMarker: (String) -> Unit,
) {
    if (isAddPointMode) {
        val anchor = metrics.nearestAnchor(viewport.screenToWorld(offset))
        onAddPoint(
            Gate.create(
                center = anchor.center,
                forward = anchor.forward,
                normal = anchor.normal,
                halfWidthMeters = selectedGate?.halfWidthMeters
                    ?.coerceAtLeast(4f)
                    ?: anchor.halfWidthMeters,
            ),
        )
        return
    }

    hitTestMarker(
        offset = offset,
        markers = markers,
        markerScreenCenters = markerScreenCenters,
    )?.let { marker ->
        onSelectMarker(marker.gateId)
        return
    }

    hitTestSector(
        offset = offset,
        sectors = sectors,
        sectorScreenPointsByEndGateId = sectorScreenPointsByEndGateId,
    )?.let { sector ->
        onSelectMarker(sector.endGateId)
    }
}

internal fun TrackMapCalibrationEditorCanvasScene.selectionUiState(
    activeDrag: TrackMapCalibrationActiveDrag?,
): TrackMapCalibrationSelectionUiState? {
    val geometry = selectedMarkerGeometry ?: return null
    selectedGate ?: return null

    val currentHandleType = editMode.toHandleType()
    val draggedPosition = activeDrag
        ?.takeIf { drag ->
            drag.gateId == geometry.gateId && drag.handleType == currentHandleType
        }
        ?.position
    val handleCenter = when (editMode) {
        TrackMapCalibrationEditorMode.Move -> draggedPosition ?: viewport.worldToScreen(geometry.center)

        TrackMapCalibrationEditorMode.Direction -> {
            val gateForPreview = activeDrag
                ?.takeIf { it.gateId == geometry.gateId }
                ?.previewGate
                ?: selectedGate
            val directionAnchor = metrics.directionHandleAnchor(
                currentGate = gateForPreview,
                fallbackAnchor = geometry.anchor,
            )
            draggedPosition ?: viewport.worldToScreen(directionAnchor.center)
        }

        TrackMapCalibrationEditorMode.Width -> draggedPosition ?: viewport.worldToScreen(geometry.widthHandlePoint)
    }
    val handleColor = when (editMode) {
        TrackMapCalibrationEditorMode.Width -> geometry.color.copy(alpha = 0.88f)
        else -> geometry.color
    }

    return TrackMapCalibrationSelectionUiState(
        label = selectedSector?.name ?: geometry.title,
        badgePoint = selectedSectorBadgePoint ?: Offset(18f, 18f),
        handleCenter = handleCenter,
        handleColor = handleColor,
    )
}

internal fun TrackMapCalibrationEditorCanvasScene.startHandleDrag(
    screenPoint: Offset,
): TrackMapCalibrationActiveDrag? {
    val geometry = selectedMarkerGeometry ?: return null
    val persistedGate = selectedGate ?: return null
    return when (editMode) {
        TrackMapCalibrationEditorMode.Move -> buildMoveDrag(
            geometry = geometry,
            persistedGate = persistedGate,
            screenPoint = screenPoint,
            hintPointIndex = geometry.anchor.pointIndex,
        )

        TrackMapCalibrationEditorMode.Direction -> buildDirectionDrag(
            geometry = geometry,
            persistedGate = persistedGate,
            screenPoint = screenPoint,
            hintPointIndex = geometry.anchor.pointIndex,
        )

        TrackMapCalibrationEditorMode.Width -> buildWidthDrag(
            geometry = geometry,
            persistedGate = persistedGate,
            screenPoint = screenPoint,
        )
    }
}

internal fun TrackMapCalibrationEditorCanvasScene.updateHandleDrag(
    screenPoint: Offset,
    activeDrag: TrackMapCalibrationActiveDrag?,
): TrackMapCalibrationActiveDrag? {
    val geometry = selectedMarkerGeometry ?: return null
    val persistedGate = selectedGate ?: return null
    return when (editMode) {
        TrackMapCalibrationEditorMode.Move -> buildMoveDrag(
            geometry = geometry,
            persistedGate = persistedGate,
            screenPoint = screenPoint,
            hintPointIndex = activeDrag?.anchorPointIndex ?: geometry.anchor.pointIndex,
        )

        TrackMapCalibrationEditorMode.Direction -> buildDirectionDrag(
            geometry = geometry,
            persistedGate = persistedGate,
            screenPoint = screenPoint,
            hintPointIndex = activeDrag?.anchorPointIndex ?: geometry.anchor.pointIndex,
        )

        TrackMapCalibrationEditorMode.Width -> buildWidthDrag(
            geometry = geometry,
            persistedGate = persistedGate,
            screenPoint = screenPoint,
        )
    }
}

internal fun commitHandleDrag(activeDrag: TrackMapCalibrationActiveDrag?, onUpdateGate: (String, Gate) -> Unit) {
    activeDrag?.previewGate?.let { previewGate ->
        onUpdateGate(activeDrag.gateId, previewGate.toGate())
    }
}

private fun TrackMapCalibrationEditorCanvasScene.buildMoveDrag(
    geometry: TrackMapCalibrationMarkerGeometry,
    persistedGate: TrackMapEditorGateUi,
    screenPoint: Offset,
    hintPointIndex: Int,
): TrackMapCalibrationActiveDrag {
    val clamped = clampToCanvas(screenPoint, widthPx, heightPx)
    val anchor = metrics.nearestAnchor(
        world = viewport.screenToWorld(clamped),
        hintPointIndex = hintPointIndex,
    )
    return TrackMapCalibrationActiveDrag(
        gateId = geometry.gateId,
        handleType = TrackMapCalibrationHandleType.Center,
        position = clamped,
        previewGate = metrics.buildMovedGate(
            anchor = anchor,
            currentGate = persistedGate,
        ),
        anchorPointIndex = anchor.pointIndex,
    )
}

private fun TrackMapCalibrationEditorCanvasScene.buildDirectionDrag(
    geometry: TrackMapCalibrationMarkerGeometry,
    persistedGate: TrackMapEditorGateUi,
    screenPoint: Offset,
    hintPointIndex: Int,
): TrackMapCalibrationActiveDrag {
    val anchor = metrics.nearestAnchor(
        world = viewport.screenToWorld(screenPoint),
        hintPointIndex = hintPointIndex,
    )
    return TrackMapCalibrationActiveDrag(
        gateId = geometry.gateId,
        handleType = TrackMapCalibrationHandleType.Direction,
        position = viewport.worldToScreen(anchor.center),
        previewGate = metrics.buildRotatedGate(
            currentGate = persistedGate,
            targetAnchor = anchor,
        ),
        anchorPointIndex = anchor.pointIndex,
    )
}

private fun TrackMapCalibrationEditorCanvasScene.buildWidthDrag(
    geometry: TrackMapCalibrationMarkerGeometry,
    persistedGate: TrackMapEditorGateUi,
    screenPoint: Offset,
): TrackMapCalibrationActiveDrag {
    val clamped = clampToCanvas(screenPoint, widthPx, heightPx)
    return TrackMapCalibrationActiveDrag(
        gateId = geometry.gateId,
        handleType = TrackMapCalibrationHandleType.Width,
        position = clamped,
        previewGate = metrics.buildResizedGate(
            currentGate = persistedGate,
            worldTarget = viewport.screenToWorld(clamped),
        ),
    )
}

private fun TrackMapCalibrationEditorMode.toHandleType(): TrackMapCalibrationHandleType = when (this) {
    TrackMapCalibrationEditorMode.Move -> TrackMapCalibrationHandleType.Center
    TrackMapCalibrationEditorMode.Direction -> TrackMapCalibrationHandleType.Direction
    TrackMapCalibrationEditorMode.Width -> TrackMapCalibrationHandleType.Width
}
