package com.project.analyzer.calibration.trackmap

import com.project.analyzer.calibration.data.PoseExtractor
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import com.project.analyzer.telemetry.api.contract.SimStatus
import com.project.analyzer.telemetry.api.contract.TelemetryGameSettings
import com.project.analyzer.telemetry.api.contract.TelemetryLifecycle
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration.Companion.milliseconds

@Inject
@SingleIn(AppScope::class)
class TrackMapRecorder(
    telemetry: TelemetryLifecycle,
    private val repository: TrackMapRepository,
    private val gameSettings: TelemetryGameSettings,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    private val stats = TrackMapStatsCalculator()
    private val runtime = TrackMapRecorderRuntime()
    private val widthProfiler = TrackMapWidthProfiler(
        stats = stats,
        minPointsToSave = MIN_POINTS_TO_SAVE
    )
    private val merger = TrackMapMerger(
        insertDistanceMeters = MERGE_INSERT_DISTANCE_METERS,
        minInsertSpacingMeters = MERGE_MIN_INSERT_SPACING_METERS,
        searchWindow = MERGE_SEARCH_WINDOW
    )
    private val pointFilter = TrackMapPointFilter(
        stats = stats,
        teleportDistanceMeters = TELEPORT_DISTANCE_METERS,
        refinementSpacingMultiplier = REFINEMENT_SPACING_MULTIPLIER,
        minRefinedSpacingMeters = MIN_REFINED_SPACING_METERS
    )
    private val lapTracker = TrackMapLapTracker(
        merger = merger,
        stats = stats,
        minPointsToSave = MIN_POINTS_TO_SAVE
    )

    private var poseExtractor = PoseExtractor(ReferencePoint.FRONT_AXLE)

    private val _state = MutableStateFlow(TrackMapRecorderState())
    val state: StateFlow<TrackMapRecorderState> = _state

    init {
        scope.launch {
            telemetry.frames
                .conflate()
                .collect { frame ->
                    runCatching { processFrame(frame) }
                        .onFailure { error ->
                            _state.update {
                                it.copy(
                                    recording = false,
                                    message = "Track map error: ${error.message ?: "unknown"}"
                                )
                            }
                        }
                }
        }
    }

    fun start() {
        scope.launch {
            mutex.withLock {
                resetLocked(clearMessage = false)
                val selection = runCatching { gameSettings.currentSelection() }.getOrNull()
                val (gameId, gameLabel) = when (selection) {
                    is GameSelection.Manual -> selection.game.id to selection.game.displayName
                    else -> "" to ""
                }

                _state.update {
                    it.copy(
                        recording = true,
                        gameId = gameId,
                        gameLabel = gameLabel,
                        message = "Recording started"
                    )
                }
            }
        }
    }

    fun stop() {
        scope.launch {
            mutex.withLock {
                _state.update { it.copy(recording = false, message = "Recording stopped") }
            }
        }
    }

    fun reset() {
        scope.launch {
            mutex.withLock {
                resetLocked(clearMessage = true)
            }
        }
    }

    fun setReferencePoint(point: ReferencePoint) {
        scope.launch {
            mutex.withLock {
                poseExtractor = PoseExtractor(point)
                if (runtime.mapPoints.isNotEmpty() || runtime.lapPoints.isNotEmpty()) {
                    resetLocked(clearMessage = false)
                }
                _state.update {
                    it.copy(
                        referencePoint = point,
                        message = "Reference point set to ${point.name.lowercase()}"
                    )
                }
            }
        }
    }

    fun setFallbackHalfWidthMeters(value: Float) {
        scope.launch {
            mutex.withLock {
                val clamped = value.coerceIn(
                    TrackMapWidthProfiler.MIN_SIDE_WIDTH_METERS,
                    TrackMapWidthProfiler.MAX_SIDE_WIDTH_METERS
                )
                _state.update {
                    it.copy(
                        fallbackHalfWidthMeters = clamped,
                        message = "Fallback half-width set to ${"%.1f".format(clamped)}m"
                    )
                }
                publishPoints(timestampNs = System.nanoTime(), force = true)
            }
        }
    }

    fun markPitEntry() {
        setManualPit(true, "Pit entry marked")
    }

    fun markPitExit() {
        setManualPit(false, "Pit exit marked")
    }

    suspend fun save() {
        val payload = mutex.withLock {
            val snapshot = _state.value
            if (snapshot.isSaving) return@withLock null

            val trackId = resolveTrackIdForSave(snapshot)
            if (trackId.isBlank()) {
                _state.update { it.copy(message = "Track id is missing") }
                return@withLock null
            }

            val points = when {
                runtime.centerlinePoints.isNotEmpty() -> runtime.centerlinePoints.toList()
                runtime.mapPoints.isNotEmpty() -> runtime.mapPoints.toList()
                else -> runtime.lapPoints.toList()
            }
            if (points.size < MIN_POINTS_TO_SAVE) {
                _state.update { it.copy(message = "Not enough points to save") }
                return@withLock null
            }
            val widths = widthProfiler.resolvePointWidths(
                runtime = runtime,
                points = points,
                fallbackHalfWidthMeters = snapshot.fallbackHalfWidthMeters
            )

            _state.update { it.copy(isSaving = true, message = "Saving track map...") }

            TrackMapSavePayload(
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
                pitExitPoint = runtime.pitExitPoint
            )
        } ?: return

        try {
            repository.save(payload.toTrackMap(stats))

            _state.update {
                it.copy(
                    isSaving = false,
                    lastSavedTrackId = payload.trackId,
                    lastSavedAtEpochMs = System.currentTimeMillis(),
                    message = "Saved track map: ${payload.trackId}"
                )
            }
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    isSaving = false,
                    message = "Save failed: ${e.message}"
                )
            }
        }
    }

    private suspend fun processFrame(frame: TelemetryFrame) {
        if (!_state.value.recording) return
        val session = frame.session
        val status = session?.status
        if (status == SimStatus.REPLAY) return

        val track = session?.track
        val speed = frame.car?.speedKmh
        val safeSpeed = speed?.takeIf { it.isFinite() }
        val pit = session?.pit
        val inPitLaneAuto = pit?.isInPitLane == true || pit?.isInPit == true
        val lapIndex = frame.lap?.currentLapIndex
            ?: frame.lap?.completedLaps?.let { it + 1 }
            ?: session?.completedLaps?.let { it + 1 }
        val timestampNs = frame.timestampNs.takeIf { it > 0L } ?: System.nanoTime()

        val pose = poseExtractor.extract(frame, timestampNs)
        val currentPos = pose?.pos?.takeIf { it.isFinite() }
        val trackId = track?.trackId
        val trackName = track?.trackName
        val layoutId = track?.layoutId

        mutex.withLock {
            val snapshot = _state.value
            if (!snapshot.recording) return

            if (shouldRefreshSessionInfo(
                    timestampNs = timestampNs,
                    lastInfoUpdateNs = runtime.lastInfoUpdateNs,
                    snapshot = snapshot,
                    trackId = trackId,
                    trackName = trackName,
                    layoutId = layoutId,
                    infoUpdateIntervalNs = INFO_UPDATE_INTERVAL_NS
                )
            ) {
                runtime.lastInfoUpdateNs = timestampNs
                _state.update { current ->
                    applySessionInfo(
                        current = current,
                        trackId = trackId,
                        trackName = trackName,
                        layoutId = layoutId,
                        speed = safeSpeed,
                        position = currentPos
                    )
                }
            }

            val tooSlow = safeSpeed != null && safeSpeed > 0f && safeSpeed < snapshot.minSpeedKmh

            var inPitLane = runtime.manualPitLane ?: inPitLaneAuto
            val manualPitLane = runtime.manualPitLane
            if (manualPitLane != null && manualPitLane == inPitLaneAuto) {
                runtime.manualPitLane = null
                inPitLane = inPitLaneAuto
            }
            val pitLaneChanged = inPitLane != runtime.previousInPitLane
            if (pitLaneChanged) {
                if (inPitLane) {
                    currentPos?.let { pos ->
                        runtime.pitEntryPoint = pos
                        maybeRecordPitPoint(pos)
                    }
                } else {
                    currentPos?.let { pos ->
                        runtime.pitExitPoint = pos
                        maybeRecordPitPoint(pos)
                    }
                }
            }
            runtime.previousInPitLane = inPitLane
            if (inPitLane) {
                runtime.sawPitInLap = true
                currentPos?.let(::maybeRecordPitPoint)
            }

            val lapResult = lapTracker.handleLapTransition(
                lapIndex = lapIndex,
                runtime = runtime
            )

            if (lapResult.message != null) {
                _state.update { it.copy(message = lapResult.message) }
            }
            if (lapResult.lapAccepted) {
                val guidanceMessage = widthProfiler.onLapAccepted(
                    runtime = runtime,
                    completedLapPoints = lapResult.completedLapPoints
                )
                if (guidanceMessage != null) {
                    _state.update { it.copy(message = guidanceMessage) }
                }
            }

            updateRuntimeState(lapIndex = lapIndex, inPitLane = inPitLane)

            if (lapResult.forcePublish) {
                publishPoints(timestampNs, force = true)
            }

            if (tooSlow) return
            if (currentPos == null) return
            if (inPitLane) {
                publishPoints(timestampNs, force = false)
                return
            }

            when (pointFilter.evaluate(currentPos, snapshot, runtime)) {
                TrackMapPointDecision.ACCEPTED -> publishPoints(timestampNs, force = false)

                TrackMapPointDecision.TELEPORT -> {
                    _state.update {
                        it.copy(recording = false, message = "Teleport detected, recording stopped")
                    }
                }

                TrackMapPointDecision.REJECTED -> Unit
            }
        }
    }

    private fun publishPoints(timestampNs: Long, force: Boolean) {
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
            fallbackHalfWidthMeters = _state.value.fallbackHalfWidthMeters
        )
        val averageTrackWidth = widths.averageTrackWidthMeters

        val shouldUpdatePoints = force ||
            displayPoints.isEmpty() ||
            displayPoints.size == 1 ||
            (timestampNs - runtime.lastUiUpdateNs) >= UI_UPDATE_INTERVAL_NS
        if (shouldUpdatePoints) {
            runtime.lastUiUpdateNs = timestampNs
        }

        _state.update {
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
                leftCoverageRatio = runtime.leftCoverageRatio,
                rightCoverageRatio = runtime.rightCoverageRatio,
                averageTrackWidthMeters = averageTrackWidth,
                guidanceText = widthProfiler.buildGuidanceText(
                    runtime = runtime,
                    recording = it.recording,
                    currentPosition = it.currentPosition,
                    points = displayPoints,
                    leftWidthsMeters = widths.leftWidthsMeters,
                    rightWidthsMeters = widths.rightWidthsMeters
                )
            )
        }
    }

    private fun updateRuntimeState(lapIndex: Int?, inPitLane: Boolean) {
        val pitOverrideActive = runtime.manualPitLane != null
        if (lapIndex == runtime.lastReportedLapIndex &&
            inPitLane == runtime.lastReportedPitLane &&
            runtime.lapsRecorded == runtime.lastReportedLapsRecorded &&
            pitOverrideActive == state.value.pitOverrideActive
        ) {
            return
        }

        runtime.lastReportedLapIndex = lapIndex
        runtime.lastReportedPitLane = inPitLane
        runtime.lastReportedLapsRecorded = runtime.lapsRecorded

        _state.update { current ->
            current.copy(
                lapIndex = lapIndex,
                lapsRecorded = runtime.lapsRecorded,
                isInPitLane = inPitLane,
                pitOverrideActive = pitOverrideActive
            )
        }
    }

    private fun resetLocked(clearMessage: Boolean) {
        runtime.resetAll()
        poseExtractor = PoseExtractor(_state.value.referencePoint)

        _state.update {
            it.copy(
                recording = false,
                points = emptyList(),
                leftWidthsMeters = emptyList(),
                rightWidthsMeters = emptyList(),
                pointCount = 0,
                totalDistanceMeters = 0f,
                bounds = null,
                lapIndex = null,
                lapsRecorded = 0,
                isInPitLane = false,
                pitOverrideActive = false,
                pitPoints = emptyList(),
                pitPointCount = 0,
                pitEntryPoint = null,
                pitExitPoint = null,
                leftCoverageRatio = 0f,
                rightCoverageRatio = 0f,
                guidanceText = null,
                averageTrackWidthMeters = TrackMapRecorderState.DEFAULT_AVERAGE_TRACK_WIDTH_METERS,
                message = if (clearMessage) null else it.message
            )
        }
    }

    private fun setManualPit(inPitLane: Boolean, message: String) {
        scope.launch {
            mutex.withLock {
                val pos = state.value.currentPosition
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
                _state.update {
                    it.copy(
                        pitPoints = runtime.pitPoints.toList(),
                        pitPointCount = runtime.pitPoints.size,
                        pitEntryPoint = runtime.pitEntryPoint,
                        pitExitPoint = runtime.pitExitPoint,
                        message = message
                    )
                }
            }
        }
    }

    private fun maybeRecordPitPoint(point: Vec2) {
        val last = runtime.lastPitAccepted
        if (last == null || last.distanceTo(point) >= PIT_MIN_SPACING_METERS) {
            runtime.recordPitPoint(point)
        }
    }

    private fun Vec2.isFinite(): Boolean = x.isFinite() && y.isFinite()

    private companion object {

        const val REFINEMENT_SPACING_MULTIPLIER = 0.5f
        const val MIN_REFINED_SPACING_METERS = 0.2f
        const val MERGE_INSERT_DISTANCE_METERS = 0.8f
        const val MERGE_MIN_INSERT_SPACING_METERS = 0.3f
        const val MERGE_SEARCH_WINDOW = 120
        const val TELEPORT_DISTANCE_METERS = 80f
        const val PIT_MIN_SPACING_METERS = 0.75f
        const val MIN_POINTS_TO_SAVE = 50
        val UI_UPDATE_INTERVAL_NS = 200.milliseconds.inWholeNanoseconds
        val INFO_UPDATE_INTERVAL_NS = 100.milliseconds.inWholeNanoseconds
    }
}
