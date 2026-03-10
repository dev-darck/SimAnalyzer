package com.analyzer.trackmap.presentation.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorGate
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorMarker
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSector
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationEditorMode
import com.analyzer.trackmap.presentation.model.TrackMapCalibrationViewport
import com.analyzer.trackmap.presentation.state.TrackMapCalibrationEditorState
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import com.project.analyzer.theme.SimAnalyzerTheme
import kotlin.math.max
import kotlin.math.min
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sqrt

private enum class TrackMapCalibrationHandleType {
    Center,
    Direction,
    Width,
}

@Composable
internal fun TrackMapCalibrationEditorCanvas(
    item: TrackMapLibraryItem,
    state: TrackMapCalibrationEditorState,
    markers: List<TrackMapCalibrationEditorMarker>,
    sectors: List<TrackMapCalibrationEditorSector>,
    editMode: TrackMapCalibrationEditorMode,
    isAddPointMode: Boolean,
    modifier: Modifier = Modifier,
    onAddPoint: (Gate) -> Unit,
    onSelectMarker: (String) -> Unit,
    onUpdateGate: (String, Gate) -> Unit,
) {
    val material = SimAnalyzerTheme.material
    val chrome = SimAnalyzerTheme.chrome
    val extended = SimAnalyzerTheme.extended
    val surfaceColor = material.background
    val borderColor = chrome.borderSubtle
    val baseTrackColor = material.primary.copy(alpha = 0.18f)
    val baseTrackStrokeColor = material.primary.copy(alpha = 0.34f)
    val dashedCenterLineColor = material.primary.copy(alpha = 0.9f)
    val labelBackground = material.surface
    val labelTextColor = material.onSurface
    val emptyTextColor = material.onSurfaceVariant
    val handleCenterColor = material.background

    BoxWithConstraints(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.large)
            .background(surfaceColor)
            .border(1.dp, borderColor, SimAnalyzerTheme.shapes.large)
            .padding(18.dp),
    ) {
        if (item.points.size < 2) {
            Text(
                text = "Track map has no points",
                modifier = Modifier.align(Alignment.Center),
                color = emptyTextColor,
                style = SimAnalyzerTheme.typography.bodySmall,
            )
            return@BoxWithConstraints
        }

        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val bounds = remember(item.bounds, item.points) {
            item.bounds ?: computeBounds(item.points)
        } ?: return@BoxWithConstraints
        val viewport = remember(bounds, widthPx, heightPx) {
            TrackMapCalibrationViewport.fit(
                bounds = bounds,
                widthPx = widthPx,
                heightPx = heightPx,
                paddingPx = 28f,
            )
        }
        val metrics = remember(
            item.points,
            item.leftWidthsMeters,
            item.rightWidthsMeters,
        ) {
            TrackMapCalibrationPolylineMetrics(item)
        }
        val trackBasePath = remember(item.points, viewport) {
            buildPath(item.points, viewport)
        }
        val selectedMarker = remember(markers, state.selectedMarkerId) {
            markers.firstOrNull { it.gateId == state.selectedMarkerId } ?: markers.firstOrNull()
        }
        val gatesById = remember(state.gates) {
            state.gates.associateBy(TrackMapCalibrationEditorGate::id)
        }
        val selectedGate = remember(selectedMarker, gatesById) {
            selectedMarker?.let { marker -> gatesById[marker.gateId]?.gate }
        }
        val markerGeometries = remember(markers, gatesById, metrics) {
            markers.associateBy(
                keySelector = TrackMapCalibrationEditorMarker::gateId,
                valueTransform = { marker ->
                    metrics.buildMarkerGeometry(
                        marker = marker,
                        gate = gatesById[marker.gateId]?.gate,
                    )
                },
            )
        }
        var activeDrag by remember {
            mutableStateOf<TrackMapCalibrationActiveDrag?>(null)
        }
        val displayedSelectedGate = remember(activeDrag, selectedGate, selectedMarker) {
            activeDrag
                ?.takeIf { drag -> drag.gateId == selectedMarker?.gateId }
                ?.previewGate
                ?: selectedGate
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
        val sectorGeometriesByEndGateId = remember(sectors, markers, metrics) {
            buildMap {
                sectors.forEach { sector ->
                    val startMarker = markers.firstOrNull { it.gateId == sector.startGateId } ?: return@forEach
                    val endMarker = markers.firstOrNull { it.gateId == sector.endGateId } ?: return@forEach
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
                buildPath(geometry.pathPoints, viewport)
            }
        }
        val sectorScreenPointsByEndGateId = remember(sectorGeometriesByEndGateId, viewport) {
            sectorGeometriesByEndGateId.mapValues { (_, geometry) ->
                geometry.pathPoints.map(viewport::worldToScreen)
            }
        }
        val selectedSector = remember(sectors, selectedMarker) {
            selectedMarker?.let { marker ->
                sectors.firstOrNull { it.endGateId == marker.gateId }
            }
        }
        val selectedSectorScreenPoints = remember(selectedSector, sectorScreenPointsByEndGateId) {
            selectedSector?.let { sector -> sectorScreenPointsByEndGateId[sector.endGateId] }
        }
        val selectedSectorBadgePoint = remember(selectedSectorScreenPoints) {
            selectedSectorScreenPoints
                ?.takeIf(List<Offset>::isNotEmpty)
                ?.let { points -> points[points.size / 2] }
        }
        val livePositionOnScreen = remember(state.livePosition, viewport) {
            state.livePosition?.let(viewport::worldToScreen)
        }
        val previewAnchor = remember(activeDrag) {
            activeDrag?.anchorPointIndex?.let(metrics::anchorAt)
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(
                    markers,
                    selectedMarker?.gateId,
                    sectorScreenPointsByEndGateId,
                    markerScreenCenters,
                    isAddPointMode,
                ) {
                    detectTapGestures { offset ->
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
                            return@detectTapGestures
                        }
                        hitTestMarker(
                            offset = offset,
                            markers = markers,
                            markerScreenCenters = markerScreenCenters,
                        )?.let { marker ->
                            onSelectMarker(marker.gateId)
                            return@detectTapGestures
                        }
                        hitTestSector(
                            offset = offset,
                            sectors = sectors,
                            sectorScreenPointsByEndGateId = sectorScreenPointsByEndGateId,
                        )?.let { sector ->
                            onSelectMarker(sector.endGateId)
                        }
                    }
                },
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawTrackBase(
                    path = trackBasePath,
                    glowColor = baseTrackColor,
                    strokeColor = baseTrackStrokeColor,
                    dashColor = dashedCenterLineColor,
                )
                selectedSector?.let { sector ->
                    val path = sectorPathsByEndGateId[sector.endGateId] ?: return@let
                    drawSelectedSector(
                        path = path,
                        color = extended.purple,
                        alpha = 0.22f,
                    )
                }
                markerGeometries.values.forEach { geometry ->
                    val effectiveGeometry = selectedMarkerGeometry
                        ?.takeIf { it.gateId == geometry.gateId }
                        ?: geometry
                    if (effectiveGeometry.gateId == selectedMarker?.gateId) {
                        drawGateGuide(
                            geometry = effectiveGeometry,
                            viewport = viewport,
                            selected = true,
                        )
                    }
                    drawMarkerPoint(
                        center = viewport.worldToScreen(effectiveGeometry.center),
                        color = effectiveGeometry.color,
                        selected = effectiveGeometry.gateId == selectedMarker?.gateId ||
                            effectiveGeometry.gateId == selectedSector?.startGateId,
                        surfaceColor = surfaceColor,
                    )
                }
                previewAnchor?.let { anchor ->
                    drawPreviewAnchor(
                        center = viewport.worldToScreen(anchor.center),
                        color = selectedMarker?.let { Color(it.colorHex) } ?: dashedCenterLineColor,
                    )
                }
                livePositionOnScreen?.let { position ->
                    drawLiveCarMarker(
                        center = position,
                        haloColor = material.primary.copy(alpha = 0.16f),
                        markerColor = material.primary,
                    )
                }
            }

            selectedMarker?.let { marker ->
                val geometry = selectedMarkerGeometry ?: markerGeometries[marker.gateId] ?: return@let
                val persistedGate = state.gates.firstOrNull { it.id == geometry.gateId }?.gate ?: return@let
                val gateForPreview = activeDrag?.previewGate ?: persistedGate
                val directionAnchor = metrics.directionHandleAnchor(
                    currentGate = gateForPreview,
                    fallbackAnchor = geometry.anchor,
                )
                val centerHandle = activeDrag
                    ?.takeIf {
                        it.gateId == geometry.gateId &&
                            it.handleType == TrackMapCalibrationHandleType.Center
                    }
                    ?.position
                    ?: viewport.worldToScreen(geometry.center)
                val directionHandle = activeDrag
                    ?.takeIf {
                        it.gateId == geometry.gateId &&
                            it.handleType == TrackMapCalibrationHandleType.Direction
                    }
                    ?.position
                    ?: viewport.worldToScreen(directionAnchor.center)
                val widthHandle = activeDrag
                    ?.takeIf {
                        it.gateId == geometry.gateId &&
                            it.handleType == TrackMapCalibrationHandleType.Width
                    }
                    ?.position
                    ?: viewport.worldToScreen(geometry.widthHandlePoint)
                TrackMapSelectedMarkerBadge(
                    text = selectedSector?.name ?: geometry.title,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            val badgePoint = selectedSectorBadgePoint ?: Offset(18f, 18f)
                            IntOffset(
                                x = (badgePoint.x - 42f).roundToInt(),
                                y = (badgePoint.y + 18f).roundToInt(),
                            )
                        },
                    backgroundColor = labelBackground,
                    textColor = labelTextColor,
                    borderColor = material.primary.copy(alpha = 0.75f),
                )
                when (editMode) {
                    TrackMapCalibrationEditorMode.Move -> {
                        TrackMapEditorHandle(
                            center = centerHandle,
                            color = geometry.color,
                            innerColor = handleCenterColor,
                            handleSize = 30.dp,
                            onDragStart = { screenPoint ->
                                val clamped = clampToCanvas(screenPoint, widthPx, heightPx)
                                val anchor = metrics.nearestAnchor(
                                    world = viewport.screenToWorld(clamped),
                                    hintPointIndex = geometry.anchor.pointIndex,
                                )
                                activeDrag = TrackMapCalibrationActiveDrag(
                                    gateId = geometry.gateId,
                                    handleType = TrackMapCalibrationHandleType.Center,
                                    position = clamped,
                                    previewGate = metrics.buildMovedGate(
                                        anchor = anchor,
                                        currentGate = persistedGate,
                                    ),
                                    anchorPointIndex = anchor.pointIndex,
                                )
                            },
                            onDrag = { screenPoint ->
                                val clamped = clampToCanvas(screenPoint, widthPx, heightPx)
                                val anchor = metrics.nearestAnchor(
                                    world = viewport.screenToWorld(clamped),
                                    hintPointIndex = activeDrag?.anchorPointIndex ?: geometry.anchor.pointIndex,
                                )
                                activeDrag = TrackMapCalibrationActiveDrag(
                                    gateId = geometry.gateId,
                                    handleType = TrackMapCalibrationHandleType.Center,
                                    position = clamped,
                                    previewGate = metrics.buildMovedGate(
                                        anchor = anchor,
                                        currentGate = persistedGate,
                                    ),
                                    anchorPointIndex = anchor.pointIndex,
                                )
                            },
                            onDragEnd = {
                                activeDrag?.previewGate?.let { previewGate ->
                                    onUpdateGate(geometry.gateId, previewGate)
                                }
                                activeDrag = null
                            },
                        )
                    }

                    TrackMapCalibrationEditorMode.Direction -> {
                        TrackMapEditorHandle(
                            center = directionHandle,
                            color = geometry.color,
                            innerColor = material.background,
                            handleSize = 30.dp,
                            onDragStart = { screenPoint ->
                                val anchor = metrics.nearestAnchor(
                                    world = viewport.screenToWorld(screenPoint),
                                    hintPointIndex = geometry.anchor.pointIndex,
                                )
                                activeDrag = TrackMapCalibrationActiveDrag(
                                    gateId = geometry.gateId,
                                    handleType = TrackMapCalibrationHandleType.Direction,
                                    position = viewport.worldToScreen(anchor.center),
                                    previewGate = metrics.buildRotatedGate(
                                        currentGate = persistedGate,
                                        targetAnchor = anchor,
                                    ),
                                    anchorPointIndex = anchor.pointIndex,
                                )
                            },
                            onDrag = { screenPoint ->
                                val anchor = metrics.nearestAnchor(
                                    world = viewport.screenToWorld(screenPoint),
                                    hintPointIndex = activeDrag?.anchorPointIndex ?: geometry.anchor.pointIndex,
                                )
                                val dragPosition = viewport.worldToScreen(anchor.center)
                                activeDrag = TrackMapCalibrationActiveDrag(
                                    gateId = geometry.gateId,
                                    handleType = TrackMapCalibrationHandleType.Direction,
                                    position = dragPosition,
                                    previewGate = metrics.buildRotatedGate(
                                        currentGate = persistedGate,
                                        targetAnchor = anchor,
                                    ),
                                    anchorPointIndex = anchor.pointIndex,
                                )
                            },
                            onDragEnd = {
                                activeDrag?.previewGate?.let { previewGate ->
                                    onUpdateGate(geometry.gateId, previewGate)
                                }
                                activeDrag = null
                            },
                        )
                    }

                    TrackMapCalibrationEditorMode.Width -> {
                        TrackMapEditorHandle(
                            center = widthHandle,
                            color = geometry.color.copy(alpha = 0.88f),
                            innerColor = handleCenterColor,
                            handleSize = 30.dp,
                            onDragStart = { screenPoint ->
                                val clamped = clampToCanvas(screenPoint, widthPx, heightPx)
                                activeDrag = TrackMapCalibrationActiveDrag(
                                    gateId = geometry.gateId,
                                    handleType = TrackMapCalibrationHandleType.Width,
                                    position = clamped,
                                    previewGate = metrics.buildResizedGate(
                                        currentGate = persistedGate,
                                        worldTarget = viewport.screenToWorld(clamped),
                                    ),
                                )
                            },
                            onDrag = { screenPoint ->
                                val dragPosition = clampToCanvas(screenPoint, widthPx, heightPx)
                                activeDrag = TrackMapCalibrationActiveDrag(
                                    gateId = geometry.gateId,
                                    handleType = TrackMapCalibrationHandleType.Width,
                                    position = dragPosition,
                                    previewGate = metrics.buildResizedGate(
                                        currentGate = persistedGate,
                                        worldTarget = viewport.screenToWorld(dragPosition),
                                    ),
                                )
                            },
                            onDragEnd = {
                                activeDrag?.previewGate?.let { previewGate ->
                                    onUpdateGate(geometry.gateId, previewGate)
                                }
                                activeDrag = null
                            },
                        )
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawLiveCarMarker(center: Offset, haloColor: Color, markerColor: Color) {
    drawCircle(
        color = haloColor,
        radius = 16f,
        center = center,
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = 6f,
        center = center,
    )
    drawCircle(
        color = markerColor,
        radius = 3.5f,
        center = center,
    )
}

@Composable
private fun TrackMapSelectedMarkerBadge(
    text: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color,
    textColor: Color,
    borderColor: Color,
) {
    Box(
        modifier = modifier
            .clip(SimAnalyzerTheme.shapes.medium)
            .background(backgroundColor)
            .border(1.dp, borderColor, SimAnalyzerTheme.shapes.medium)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = SimAnalyzerTheme.typography.bodySmall,
            color = textColor,
        )
    }
}

@Composable
private fun TrackMapEditorHandle(
    center: Offset,
    color: Color,
    innerColor: Color,
    handleSize: androidx.compose.ui.unit.Dp = 32.dp,
    onDragStart: (Offset) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
) {
    val halfSizePx = with(LocalDensity.current) { (handleSize / 2).toPx() }
    val currentCenter = rememberUpdatedState(center)
    val currentOnDragStart = rememberUpdatedState(onDragStart)
    val currentOnDrag = rememberUpdatedState(onDrag)
    val currentOnDragEnd = rememberUpdatedState(onDragEnd)
    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (center.x - halfSizePx).roundToInt(),
                    y = (center.y - halfSizePx).roundToInt(),
                )
            }
            .size(handleSize)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .border(2.dp, color, CircleShape)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { start ->
                        val absolute = Offset(
                            x = currentCenter.value.x - halfSizePx + start.x,
                            y = currentCenter.value.y - halfSizePx + start.y,
                        )
                        currentOnDragStart.value(absolute)
                    },
                    onDragCancel = {
                        currentOnDragEnd.value()
                    },
                    onDragEnd = {
                        currentOnDragEnd.value()
                    },
                ) { change, _ ->
                    change.consume()
                    val absolute = Offset(
                        x = currentCenter.value.x - halfSizePx + change.position.x,
                        y = currentCenter.value.y - halfSizePx + change.position.y,
                    )
                    currentOnDrag.value(absolute)
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(10.dp)
                .clip(CircleShape)
                .background(innerColor)
                .border(2.dp, color, CircleShape),
        )
    }
}

private data class TrackMapCalibrationActiveDrag(
    val gateId: String,
    val handleType: TrackMapCalibrationHandleType,
    val position: Offset,
    val previewGate: Gate,
    val anchorPointIndex: Int? = null,
)

private class TrackMapCalibrationPolylineMetrics(item: TrackMapLibraryItem) {

    private val points: List<Vec2> = item.points
    private val leftWidths: List<Float> = item.leftWidthsMeters
    private val rightWidths: List<Float> = item.rightWidthsMeters
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

    fun buildMovedGate(anchor: TrackMapCalibrationTrackAnchor, currentGate: Gate): Gate {
        val forward = currentGate.forwardV2().safeNormalized(anchor.forward)
        val normal = deriveNormal(
            forward = forward,
            preferred = currentGate.normalV2(),
        )
        return Gate.create(
            center = anchor.center,
            forward = forward,
            normal = normal,
            halfWidthMeters = currentGate.halfWidthMeters.coerceAtLeast(4f),
        )
    }

    fun directionHandleAnchor(
        currentGate: Gate,
        fallbackAnchor: TrackMapCalibrationTrackAnchor,
    ): TrackMapCalibrationTrackAnchor {
        val forward = currentGate.forwardV2().safeNormalized(fallbackAnchor.forward)
        val step = when {
            points.size >= 400 -> 8
            points.size >= 200 -> 6
            else -> 4
        }
        val direction = if (forward.dot(fallbackAnchor.forward) >= 0f) 1 else -1
        return anchorAt(wrapIndex(fallbackAnchor.pointIndex + direction * step))
    }

    fun buildRotatedGate(currentGate: Gate, targetAnchor: TrackMapCalibrationTrackAnchor): Gate {
        val center = currentGate.centerV2()
        val currentForward = currentGate.forwardV2().safeNormalized(targetAnchor.forward)
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
            preferred = currentGate.normalV2(),
        )
        return Gate.create(
            center = center,
            forward = forward,
            normal = normal,
            halfWidthMeters = currentGate.halfWidthMeters.coerceAtLeast(4f),
        )
    }

    fun buildResizedGate(currentGate: Gate, worldTarget: Vec2): Gate {
        val center = currentGate.centerV2()
        val forward = currentGate.forwardV2().safeNormalized(Vec2.Right)
        val normal = deriveNormal(
            forward = forward,
            preferred = currentGate.normalV2(),
        )
        val halfWidth = snapHalfWidthMeters(
            kotlin.math.abs((worldTarget - center).dot(normal)),
        ).coerceIn(2f, 80f)
        return Gate.create(
            center = center,
            forward = forward,
            normal = normal,
            halfWidthMeters = halfWidth,
        )
    }

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

    fun buildMarkerGeometry(marker: TrackMapCalibrationEditorMarker, gate: Gate?): TrackMapCalibrationMarkerGeometry {
        val anchor = anchorAt(marker.pointIndex)
        return TrackMapCalibrationMarkerGeometry(
            gateId = marker.gateId,
            title = marker.title,
            color = Color(marker.colorHex),
            center = gate?.centerV2() ?: anchor.center,
            forward = gate?.forwardV2()?.safeNormalized(anchor.forward) ?: anchor.forward,
            normal = gate?.normalV2()?.safeNormalized(anchor.normal) ?: anchor.normal,
            halfWidthMeters = gate?.halfWidthMeters?.coerceAtLeast(4f) ?: anchor.halfWidthMeters,
            anchor = anchor,
        )
    }

    fun buildSectorGeometry(
        startMarker: TrackMapCalibrationEditorMarker,
        endMarker: TrackMapCalibrationEditorMarker,
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

private data class TrackMapCalibrationTrackAnchor(
    val pointIndex: Int,
    val center: Vec2,
    val forward: Vec2,
    val normal: Vec2,
    val halfWidthMeters: Float,
    val distanceMeters: Float,
)

private data class TrackMapCalibrationSectorGeometry(
    val name: String,
    val color: Color,
    val startGateId: String,
    val endGateId: String,
    val startAnchor: TrackMapCalibrationTrackAnchor,
    val endAnchor: TrackMapCalibrationTrackAnchor,
    val labelPoint: Vec2,
    val pathPoints: List<Vec2>,
)

private data class TrackMapCalibrationMarkerGeometry(
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

private fun DrawScope.drawPreviewAnchor(center: Offset, color: Color) {
    drawCircle(
        color = color.copy(alpha = 0.18f),
        radius = 16f,
        center = center,
    )
    drawCircle(
        color = color,
        radius = 7f,
        center = center,
    )
}

private fun DrawScope.drawGateGuide(
    geometry: TrackMapCalibrationMarkerGeometry,
    viewport: TrackMapCalibrationViewport,
    selected: Boolean,
) {
    val lineStart = viewport.worldToScreen(
        geometry.center - geometry.normal * geometry.halfWidthMeters,
    )
    val lineEnd = viewport.worldToScreen(
        geometry.center + geometry.normal * geometry.halfWidthMeters,
    )
    val center = viewport.worldToScreen(geometry.center)
    val arrowTip = viewport.worldToScreen(geometry.directionHandlePoint)
    val strokeWidth = if (selected) 4.5f else 2.5f
    val alpha = if (selected) 0.92f else 0.34f
    drawLine(
        color = geometry.color.copy(alpha = alpha),
        start = lineStart,
        end = lineEnd,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = geometry.color.copy(alpha = alpha),
        start = center,
        end = arrowTip,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    val arrowDirection = (arrowTip - center).normalizedSafe()
    val leftWing = rotateOffset(arrowDirection, 2.55f) * 10f
    val rightWing = rotateOffset(arrowDirection, -2.55f) * 10f
    drawLine(
        color = geometry.color.copy(alpha = alpha),
        start = arrowTip,
        end = arrowTip + leftWing,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = geometry.color.copy(alpha = alpha),
        start = arrowTip,
        end = arrowTip + rightWing,
        strokeWidth = strokeWidth,
        cap = StrokeCap.Round,
    )
}

private fun DrawScope.drawTrackBase(path: Path, glowColor: Color, strokeColor: Color, dashColor: Color) {
    drawPath(
        path = path,
        color = glowColor,
        style = Stroke(width = 14f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = path,
        color = strokeColor,
        style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = path,
        color = dashColor,
        style = Stroke(
            width = 1.8f,
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f, 11f)),
        ),
    )
}

private fun DrawScope.drawSelectedSector(path: Path, color: Color, alpha: Float) {
    drawPath(
        path = path,
        color = color.copy(alpha = alpha),
        style = Stroke(width = 16f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    drawPath(
        path = path,
        color = color,
        style = Stroke(width = 9f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawMarkerPoint(center: Offset, color: Color, selected: Boolean, surfaceColor: Color) {
    val radius = if (selected) 7f else 4.5f
    drawCircle(
        color = color.copy(alpha = if (selected) 0.24f else 0.18f),
        radius = radius + if (selected) 6f else 4f,
        center = center,
    )
    drawCircle(
        color = surfaceColor,
        radius = radius + 1.5f,
        center = center,
    )
    drawCircle(
        color = color,
        radius = radius,
        center = center,
    )
}

private fun hitTestMarker(
    offset: Offset,
    markers: List<TrackMapCalibrationEditorMarker>,
    markerScreenCenters: Map<String, Offset>,
): TrackMapCalibrationEditorMarker? {
    var bestMarker: TrackMapCalibrationEditorMarker? = null
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

private fun hitTestSector(
    offset: Offset,
    sectors: List<TrackMapCalibrationEditorSector>,
    sectorScreenPointsByEndGateId: Map<String, List<Offset>>,
): TrackMapCalibrationEditorSector? {
    var best: TrackMapCalibrationEditorSector? = null
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

private fun buildPath(points: List<Vec2>, viewport: TrackMapCalibrationViewport): Path {
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

private fun computeBounds(points: List<Vec2>): TrackMapBounds? {
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
    return TrackMapBounds(minX = minX, minY = minY, maxX = maxX, maxY = maxY)
}

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

private fun Offset.normalizedSafe(): Offset {
    val length = sqrt(x * x + y * y)
    if (length <= 0.0001f) return Offset(1f, 0f)
    return Offset(x / length, y / length)
}

private fun rotateOffset(offset: Offset, radians: Float): Offset {
    val sin = kotlin.math.sin(radians)
    val cos = kotlin.math.cos(radians)
    return Offset(
        x = offset.x * cos - offset.y * sin,
        y = offset.x * sin + offset.y * cos,
    )
}

private fun clampToCanvas(point: Offset, widthPx: Float, heightPx: Float): Offset = Offset(
    x = point.x.coerceIn(0f, widthPx),
    y = point.y.coerceIn(0f, heightPx),
)
