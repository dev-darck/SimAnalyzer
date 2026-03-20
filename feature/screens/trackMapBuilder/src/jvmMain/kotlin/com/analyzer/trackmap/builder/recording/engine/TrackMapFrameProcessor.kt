package com.analyzer.trackmap.builder.recording.engine

import com.analyzer.trackmap.builder.recording.lap.TrackMapLapTracker
import com.analyzer.trackmap.builder.recording.lap.TrackMapLapTransitionResult
import com.analyzer.trackmap.builder.recording.point.TrackMapPointDecision
import com.analyzer.trackmap.builder.recording.point.TrackMapPointFilter
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderRuntime
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderState
import com.analyzer.trackmap.builder.recording.runtime.applySessionInfo
import com.analyzer.trackmap.builder.recording.runtime.shouldRefreshSessionInfo
import com.analyzer.trackmap.builder.recording.width.TrackMapWidthProfiler
import com.project.analyzer.math.Vec2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class TrackMapFrameProcessor(
    private val runtime: TrackMapRecorderRuntime,
    private val runtimeState: MutableStateFlow<TrackMapRecorderState>,
    private val widthProfiler: TrackMapWidthProfiler,
    private val pointFilter: TrackMapPointFilter,
    private val lapTracker: TrackMapLapTracker,
    private val config: Config,
) {

    fun processFrame(frameContext: TrackMapFrameContext) {
        val snapshot = runtimeState.value
        if (
            shouldRefreshSessionInfo(
                timestampNs = frameContext.timestampNs,
                lastInfoUpdateNs = runtime.lastInfoUpdateNs,
                snapshot = snapshot,
                trackId = frameContext.trackId,
                trackName = frameContext.trackName,
                layoutId = frameContext.layoutId,
                infoUpdateIntervalNs = config.infoUpdateIntervalNs,
            )
        ) {
            runtime.lastInfoUpdateNs = frameContext.timestampNs
            runtimeState.update { current ->
                applySessionInfo(
                    current = current,
                    trackId = frameContext.trackId,
                    trackName = frameContext.trackName,
                    layoutId = frameContext.layoutId,
                    speed = frameContext.safeSpeed,
                    position = frameContext.currentPos,
                )
            }
        }

        runtime.sectorCount = maxOf(runtime.sectorCount, frameContext.sectorCount?.coerceAtLeast(0) ?: 0)
        val inPitLane = resolvePitLaneState(
            currentPos = frameContext.currentPos,
            inPitLaneAuto = frameContext.inPitLaneAuto,
        )

        val lapResult = lapTracker.handleLapTransition(
            lapIndex = frameContext.lapIndex,
            runtime = runtime,
        )
        applyLapResult(lapResult, frameContext.lapIndex)
        updateRuntimeState(lapIndex = frameContext.lapIndex, inPitLane = inPitLane)
        captureSectorIfNeeded(frameContext, inPitLane)

        if (lapResult.forcePublish) {
            publishPoints(frameContext.timestampNs, force = true)
        }

        recordPointIfEligible(
            snapshot = snapshot,
            frameContext = frameContext,
            inPitLane = inPitLane,
        )
    }

    fun publishPoints(timestampNs: Long, force: Boolean) {
        val displayPoints = when {
            runtime.centerlinePoints.isNotEmpty() -> runtime.centerlinePoints
            runtime.mapPoints.isNotEmpty() -> runtime.mapPoints
            else -> runtime.lapPoints
        }
        val distance = if (runtime.centerlinePoints.isNotEmpty() || runtime.mapPoints.isNotEmpty()) {
            runtime.trackDistanceMeters
        } else {
            runtime.lapDistanceMeters
        }
        val widths = widthProfiler.resolvePointWidths(
            runtime = runtime,
            points = displayPoints,
            fallbackHalfWidthMeters = runtimeState.value.fallbackHalfWidthMeters,
        )
        val sectorMarkers = runtime.sectorMarkers()

        val shouldUpdatePoints = force ||
            displayPoints.isEmpty() ||
            displayPoints.size == 1 ||
            (timestampNs - runtime.lastUiUpdateNs) >= config.uiUpdateIntervalNs
        if (shouldUpdatePoints) {
            runtime.lastUiUpdateNs = timestampNs
        }

        runtimeState.update {
            it.copy(
                points = if (shouldUpdatePoints) displayPoints.toList() else it.points,
                leftWidthsMeters = if (shouldUpdatePoints) widths.leftWidthsMeters else it.leftWidthsMeters,
                rightWidthsMeters = if (shouldUpdatePoints) widths.rightWidthsMeters else it.rightWidthsMeters,
                pointCount = displayPoints.size,
                totalDistanceMeters = distance,
                bounds = runtime.bounds,
                pitPoints = if (shouldUpdatePoints) runtime.pitPoints.toList() else it.pitPoints,
                pitPointCount = runtime.pitPoints.size,
                pitEntryPoint = runtime.pitEntryPoint,
                pitExitPoint = runtime.pitExitPoint,
                sectorCount = runtime.sectorCount,
                capturedSectorCount = sectorMarkers.size,
                sectorMarkers = sectorMarkers,
                leftCoverageRatio = runtime.leftCoverageRatio,
                rightCoverageRatio = runtime.rightCoverageRatio,
                averageTrackWidthMeters = widths.averageTrackWidthMeters,
                guidanceText = widthProfiler.buildGuidanceText(
                    TrackMapWidthProfiler.TrackMapGuidanceRequest(
                        runtime = runtime,
                        recording = it.recording,
                        currentPosition = it.currentPosition,
                        points = displayPoints,
                        leftWidthsMeters = widths.leftWidthsMeters,
                        rightWidthsMeters = widths.rightWidthsMeters,
                    ),
                ),
            )
        }
    }

    fun setManualPit(inPitLane: Boolean, message: String) {
        val pos = runtimeState.value.currentPosition
        runtime.manualPitLane = inPitLane
        runtime.sawPitInLap = true
        if (pos != null) {
            if (inPitLane) {
                runtime.pitEntryPoint = pos
            } else {
                runtime.pitExitPoint = pos
            }
            maybeRecordPitPoint(pos)
        }
        updateRuntimeState(lapIndex = runtime.lastLapIndex, inPitLane = inPitLane)
        runtimeState.update {
            it.copy(
                pitPoints = runtime.pitPoints.toList(),
                pitPointCount = runtime.pitPoints.size,
                pitEntryPoint = runtime.pitEntryPoint,
                pitExitPoint = runtime.pitExitPoint,
                message = message,
            )
        }
    }

    private fun maybeRecordPitPoint(point: Vec2) {
        val last = runtime.lastPitAccepted
        if (last == null || last.distanceTo(point) >= config.pitMinSpacingMeters) {
            runtime.recordPitPoint(point)
        }
    }

    private fun captureSingleSectorStart(lapIndex: Int?, point: Vec2) {
        runtime.sectorCount = 1
        runtime.recordSectorSample(
            sectorStartIndex = 1,
            lapIndex = lapIndex,
            point = point,
        )
    }

    private fun resolvePitLaneState(currentPos: Vec2?, inPitLaneAuto: Boolean): Boolean {
        var inPitLane = runtime.manualPitLane ?: inPitLaneAuto
        val manualPitLane = runtime.manualPitLane
        if (manualPitLane != null && manualPitLane == inPitLaneAuto) {
            runtime.manualPitLane = null
            inPitLane = inPitLaneAuto
        }

        if (inPitLane != runtime.previousInPitLane) {
            currentPos?.let { pos ->
                if (inPitLane) {
                    runtime.pitEntryPoint = pos
                } else {
                    runtime.pitExitPoint = pos
                }
                maybeRecordPitPoint(pos)
            }
        }

        runtime.previousInPitLane = inPitLane
        if (inPitLane) {
            runtime.sawPitInLap = true
            currentPos?.let(::maybeRecordPitPoint)
        }
        return inPitLane
    }

    private fun applyLapResult(lapResult: TrackMapLapTransitionResult, lapIndex: Int?) {
        lapResult.message?.let(::updateMessage)
        if (!lapResult.lapAccepted) return

        if (runtime.sectorCount == 1) {
            lapResult.completedLapPoints.firstOrNull()?.let { startFinishPoint ->
                captureSingleSectorStart(lapIndex = lapIndex, point = startFinishPoint)
            }
        }

        widthProfiler.onLapAccepted(
            runtime = runtime,
            completedLapPoints = lapResult.completedLapPoints,
        )?.let(::updateMessage)
    }

    private fun captureSectorIfNeeded(frameContext: TrackMapFrameContext, inPitLane: Boolean) {
        val currentPos = frameContext.currentPos ?: return
        if (inPitLane) return

        captureSectorTransition(
            sectorCount = frameContext.sectorCount,
            currentSectorIndex = frameContext.currentSectorIndex,
            currentPosition = currentPos,
            lapIndex = frameContext.lapIndex,
        )?.let(::updateMessage)
    }

    private fun recordPointIfEligible(
        snapshot: TrackMapRecorderState,
        frameContext: TrackMapFrameContext,
        inPitLane: Boolean,
    ) {
        val tooSlow = frameContext.safeSpeed != null &&
            frameContext.safeSpeed > 0f &&
            frameContext.safeSpeed < snapshot.minSpeedKmh
        if (tooSlow) return

        val currentPos = frameContext.currentPos ?: return
        if (inPitLane) {
            publishPoints(frameContext.timestampNs, force = false)
            return
        }

        when (pointFilter.evaluate(currentPos, snapshot, runtime)) {
            TrackMapPointDecision.ACCEPTED -> publishPoints(frameContext.timestampNs, force = false)

            TrackMapPointDecision.TELEPORT -> {
                restartSegmentAfterTeleport(
                    currentPos = currentPos,
                    timestampNs = frameContext.timestampNs,
                )
            }

            TrackMapPointDecision.REJECTED -> Unit
        }
    }

    private fun restartSegmentAfterTeleport(currentPos: Vec2, timestampNs: Long) {
        // A persistent coordinate jump can wedge the recorder on the old anchor forever.
        // Reset only the in-flight lap segment so capture can continue without restarting the screen.
        runtime.resetLap(clearMap = false)
        pointFilter.restartSegment(point = currentPos, runtime = runtime)
        publishPoints(timestampNs = timestampNs, force = true)
        updateMessage(MESSAGE_SEGMENT_RESTARTED)
    }

    private fun updateRuntimeState(lapIndex: Int?, inPitLane: Boolean) {
        val pitOverrideActive = runtime.manualPitLane != null
        val runtimeStateUnchanged = lapIndex == runtime.lastReportedLapIndex &&
            inPitLane == runtime.lastReportedPitLane &&
            runtime.lapsRecorded == runtime.lastReportedLapsRecorded
        val pitOverrideUnchanged = pitOverrideActive == runtimeState.value.pitOverrideActive
        if (runtimeStateUnchanged && pitOverrideUnchanged) {
            return
        }

        runtime.lastReportedLapIndex = lapIndex
        runtime.lastReportedPitLane = inPitLane
        runtime.lastReportedLapsRecorded = runtime.lapsRecorded

        runtimeState.update { current ->
            current.copy(
                lapIndex = lapIndex,
                lapsRecorded = runtime.lapsRecorded,
                isInPitLane = inPitLane,
                pitOverrideActive = pitOverrideActive,
            )
        }
    }

    private fun captureSectorTransition(
        sectorCount: Int?,
        currentSectorIndex: Int?,
        currentPosition: Vec2,
        lapIndex: Int?,
    ): String? {
        val resolvedSectorCount = sectorCount?.coerceAtLeast(1) ?: return null
        val resolvedCurrentSector = currentSectorIndex?.coerceIn(0, resolvedSectorCount - 1) ?: return null
        val previousSector = runtime.lastSectorIndex
        runtime.lastSectorIndex = resolvedCurrentSector

        if (previousSector == null || previousSector == resolvedCurrentSector) return null
        val expectedSector = (previousSector + 1) % resolvedSectorCount
        if (resolvedCurrentSector != expectedSector) return null

        val sectorStartIndex = resolvedCurrentSector + 1
        val isNewSector = runtime.recordSectorSample(
            sectorStartIndex = sectorStartIndex,
            lapIndex = lapIndex,
            point = currentPosition,
        )
        if (!isNewSector) return null

        return "Sector S$sectorStartIndex captured (${runtime.sectorMarkers().size}/$resolvedSectorCount)"
    }

    private fun updateMessage(message: String) {
        runtimeState.update { current -> current.copy(message = message) }
    }

    internal data class Config(
        val pitMinSpacingMeters: Float,
        val uiUpdateIntervalNs: Long,
        val infoUpdateIntervalNs: Long,
    )

    private companion object {

        const val MESSAGE_SEGMENT_RESTARTED = "Position jump detected, segment restarted"
    }
}
