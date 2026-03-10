package com.analyzer.trackmap.builder.recording.save

import com.analyzer.trackmap.builder.recording.calibration.TrackMapCalibrationRequest
import com.analyzer.trackmap.builder.recording.calibration.buildTrackCalibration
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderRuntime
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderState
import com.analyzer.trackmap.builder.recording.runtime.resolveTrackIdForSave
import com.analyzer.trackmap.builder.recording.width.TrackMapWidthProfiler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class TrackMapSavePreparer(
    private val runtime: TrackMapRecorderRuntime,
    private val runtimeState: MutableStateFlow<TrackMapRecorderState>,
    private val widthProfiler: TrackMapWidthProfiler,
    private val minPointsToSave: Int,
) {

    fun prepareSaveRequest(): TrackMapSaveRequest? {
        val snapshot = runtimeState.value
        if (snapshot.isSaving) return null

        val trackId = resolveTrackIdForSave(snapshot)
        if (trackId.isBlank()) {
            runtimeState.update { it.copy(message = "Track id is missing") }
            return null
        }

        val points = resolvePointsForSave()
        if (points.size < minPointsToSave) {
            runtimeState.update { it.copy(message = "Not enough points to save") }
            return null
        }

        val widths = widthProfiler.resolvePointWidths(
            runtime = runtime,
            points = points,
            fallbackHalfWidthMeters = snapshot.fallbackHalfWidthMeters,
        )

        runtimeState.update { it.copy(isSaving = true, message = "Saving track map...") }

        return TrackMapSaveRequest(
            payload = TrackMapSavePayload(
                gameId = snapshot.gameId.ifBlank { "unknown" },
                trackId = trackId,
                trackName = snapshot.trackName.ifBlank { trackId },
                layoutId = snapshot.layoutId?.trim().orEmpty(),
                referencePoint = snapshot.referencePoint,
                points = points,
                leftWidthsMeters = widths.leftWidthsMeters,
                rightWidthsMeters = widths.rightWidthsMeters,
                pitPoints = runtime.pitPoints.toList(),
                bounds = runtime.bounds,
                pitEntryPoint = runtime.pitEntryPoint,
                pitExitPoint = runtime.pitExitPoint,
            ),
            calibration = buildTrackCalibration(
                TrackMapCalibrationRequest(
                    snapshot = snapshot,
                    trackId = trackId,
                    sectorCount = runtime.sectorCount,
                    sectorMarkers = runtime.sectorMarkers(),
                    points = points,
                    leftWidthsMeters = widths.leftWidthsMeters,
                    rightWidthsMeters = widths.rightWidthsMeters,
                ),
            ),
        )
    }

    private fun resolvePointsForSave() = when {
        runtime.centerlinePoints.isNotEmpty() -> runtime.centerlinePoints.toList()
        runtime.mapPoints.isNotEmpty() -> runtime.mapPoints.toList()
        else -> runtime.lapPoints.toList()
    }
}
