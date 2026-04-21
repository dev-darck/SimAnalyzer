package com.analyzer.trackmap.builder.recording

import com.analyzer.trackmap.builder.recording.engine.TrackMapFrameProcessor
import com.analyzer.trackmap.builder.recording.engine.toTrackMapFrameContext
import com.analyzer.trackmap.builder.recording.lap.TrackMapLapTracker
import com.analyzer.trackmap.builder.recording.lap.TrackMapMerger
import com.analyzer.trackmap.builder.recording.point.TrackMapPointFilter
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderRuntime
import com.analyzer.trackmap.builder.recording.runtime.TrackMapRecorderState
import com.analyzer.trackmap.builder.recording.runtime.toTrackMapBuilderState
import com.analyzer.trackmap.builder.recording.save.TrackMapSavePreparer
import com.analyzer.trackmap.builder.recording.save.TrackMapSaveRequest
import com.analyzer.trackmap.builder.recording.save.toTrackMap
import com.analyzer.trackmap.builder.recording.width.TrackMapWidthProfiler
import com.analyzer.trackmap.data.library.TrackMapStatsCalculator
import com.analyzer.trackmap.domain.TrackMapCaptureController
import com.analyzer.trackmap.domain.model.TrackMapBuilderState
import com.project.analyzer.api.di.Default
import com.project.analyzer.api.di.IO
import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.telemetry.ac.api.calibration.ReferencePointPoseExtractor
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import com.project.analyzer.telemetry.api.contract.TelemetryFrameSource
import com.project.analyzer.telemetry.api.contract.TelemetryGameSettings
import com.project.analyzer.telemetry.api.model.TelemetryFrame
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TrackMapCaptureController>())
@Inject
class TrackMapRecorder(
    telemetryFrames: TelemetryFrameSource,
    private val repository: TrackMapRepository,
    private val calibrationRepository: TrackCalibrationRepository,
    private val gameSettings: TelemetryGameSettings,
    @param:Default
    private val defaultDispatcher: CoroutineDispatcher,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TrackMapCaptureController {

    private val scope = CoroutineScope(SupervisorJob() + defaultDispatcher)
    private val mutex = Mutex()

    private val stats = TrackMapStatsCalculator()
    private val runtime = TrackMapRecorderRuntime()
    private val widthProfiler = TrackMapWidthProfiler(
        stats = stats,
        minPointsToSave = MIN_POINTS_TO_SAVE,
    )
    private val merger = TrackMapMerger(
        insertDistanceMeters = MERGE_INSERT_DISTANCE_METERS,
        minInsertSpacingMeters = MERGE_MIN_INSERT_SPACING_METERS,
        searchWindow = MERGE_SEARCH_WINDOW,
    )
    private val pointFilter = TrackMapPointFilter(
        stats = stats,
        teleportDistanceMeters = TELEPORT_DISTANCE_METERS,
        refinementSpacingMultiplier = REFINEMENT_SPACING_MULTIPLIER,
        minRefinedSpacingMeters = MIN_REFINED_SPACING_METERS,
    )
    private val lapTracker = TrackMapLapTracker(
        merger = merger,
        stats = stats,
        minPointsToSave = MIN_POINTS_TO_SAVE,
    )
    private val runtimeState = MutableStateFlow(TrackMapRecorderState())
    private val frameProcessor = TrackMapFrameProcessor(
        runtime = runtime,
        runtimeState = runtimeState,
        widthProfiler = widthProfiler,
        pointFilter = pointFilter,
        lapTracker = lapTracker,
        config = TrackMapFrameProcessor.Config(
            pitMinSpacingMeters = PIT_MIN_SPACING_METERS,
            uiUpdateIntervalNs = UI_UPDATE_INTERVAL_NS,
            infoUpdateIntervalNs = INFO_UPDATE_INTERVAL_NS,
        ),
    )
    private val savePreparer = TrackMapSavePreparer(
        runtime = runtime,
        runtimeState = runtimeState,
        widthProfiler = widthProfiler,
        minPointsToSave = MIN_POINTS_TO_SAVE,
    )

    private var poseExtractor = ReferencePointPoseExtractor(ReferencePoint.FRONT_AXLE)

    override val state: StateFlow<TrackMapBuilderState> = runtimeState
        .map(TrackMapRecorderState::toTrackMapBuilderState)
        .stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = TrackMapBuilderState(),
        )

    init {
        scope.launch {
            telemetryFrames.frames
                .conflate()
                .collect { frame ->
                    try {
                        processFrame(frame)
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (error: Throwable) {
                        runtimeState.update { current ->
                            current.copy(
                                message = "Track map frame skipped: ${error.message ?: "unknown"}",
                            )
                        }
                    }
                }
        }
    }

    override suspend fun start() = withRecorderLock {
        resetLocked(clearMessage = false)
        val selection = runCatching { gameSettings.currentSelection() }.getOrNull()
        val (gameId, gameLabel) = when (selection) {
            is GameSelection.Manual -> selection.game.id to selection.game.displayName
            else -> "" to ""
        }
        runtimeState.update {
            it.copy(
                recording = true,
                gameId = gameId,
                gameLabel = gameLabel,
                message = "Recording started",
            )
        }
    }

    override suspend fun stop() = withRecorderLock {
        runtimeState.update { current -> current.copy(recording = false, message = "Recording stopped") }
    }

    override suspend fun reset() = withRecorderLock {
        resetLocked(clearMessage = true)
    }

    override suspend fun setReferencePoint(point: ReferencePoint) = withRecorderLock {
        poseExtractor = ReferencePointPoseExtractor(point)
        if (runtime.mapPoints.isNotEmpty() || runtime.lapPoints.isNotEmpty()) {
            resetLocked(clearMessage = false)
        }
        runtimeState.update { current ->
            current.copy(
                referencePoint = point,
                message = "Reference point set to ${point.name.lowercase()}",
            )
        }
    }

    override suspend fun setFallbackHalfWidthMeters(value: Float) = withRecorderLock {
        val clamped = value.coerceIn(
            TrackMapWidthProfiler.MIN_SIDE_WIDTH_METERS,
            TrackMapWidthProfiler.MAX_SIDE_WIDTH_METERS,
        )
        runtimeState.update { current ->
            current.copy(
                fallbackHalfWidthMeters = clamped,
                message = "Fallback half-width set to ${"%.1f".format(clamped)}m",
            )
        }
        frameProcessor.publishPoints(timestampNs = System.nanoTime(), force = true)
    }

    override suspend fun markPitEntry() {
        setManualPit(true, "Pit entry marked")
    }

    override suspend fun markPitExit() {
        setManualPit(false, "Pit exit marked")
    }

    override suspend fun save() {
        val saveRequest = withRecorderLock { savePreparer.prepareSaveRequest() } ?: return
        val trackMap = withContext(defaultDispatcher) {
            saveRequest.payload.toTrackMap(stats)
        }

        try {
            val calibrationError = withContext(ioDispatcher) {
                repository.save(trackMap)
                runCatching {
                    if (saveRequest.calibration != null) {
                        calibrationRepository.save(saveRequest.calibration)
                    }
                }.exceptionOrNull()
            }
            withContext(defaultDispatcher) {
                applySaveSuccess(saveRequest, calibrationError)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            withContext(defaultDispatcher) {
                runtimeState.update {
                    it.copy(
                        isSaving = false,
                        message = "Save failed: ${error.message}",
                    )
                }
            }
        }
    }

    private suspend fun processFrame(frame: TelemetryFrame) {
        if (!runtimeState.value.recording) return
        val frameContext = frame.toTrackMapFrameContext(poseExtractor) ?: return

        mutex.withLock {
            if (!runtimeState.value.recording) return
            frameProcessor.processFrame(frameContext)
        }
    }

    private fun applySaveSuccess(saveRequest: TrackMapSaveRequest, calibrationError: Throwable?) {
        runtimeState.update {
            it.copy(
                isSaving = false,
                lastSavedTrackId = saveRequest.payload.trackId,
                lastSavedAtEpochMs = System.currentTimeMillis(),
                message = when {
                    calibrationError != null -> {
                        "Saved track map, sector save failed: ${calibrationError.message}"
                    }

                    saveRequest.calibration != null -> {
                        "Saved track map + sectors: ${saveRequest.payload.trackId}"
                    }

                    else -> "Saved track map: ${saveRequest.payload.trackId}"
                },
            )
        }
    }

    private fun resetLocked(clearMessage: Boolean) {
        runtime.resetAll()
        poseExtractor = ReferencePointPoseExtractor(runtimeState.value.referencePoint)

        runtimeState.update {
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
                sectorCount = 0,
                capturedSectorCount = 0,
                sectorMarkers = emptyList(),
                leftCoverageRatio = 0f,
                rightCoverageRatio = 0f,
                guidanceText = null,
                averageTrackWidthMeters = TrackMapRecorderState.DEFAULT_AVERAGE_TRACK_WIDTH_METERS,
                message = if (clearMessage) null else it.message,
            )
        }
    }

    private suspend fun setManualPit(inPitLane: Boolean, message: String) {
        withRecorderLock {
            frameProcessor.setManualPit(inPitLane = inPitLane, message = message)
        }
    }

    private suspend fun <T> withRecorderLock(block: suspend () -> T): T = withContext(defaultDispatcher) {
        mutex.withLock {
            block()
        }
    }

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
