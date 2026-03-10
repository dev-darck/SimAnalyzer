package com.project.analyzer.calibration.presentation.verify

import androidx.lifecycle.viewModelScope
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.calibration.domain.model.CalibrationSample
import com.project.analyzer.calibration.domain.usecase.CaptureGateOnStandstillUseCase
import com.project.analyzer.calibration.domain.usecase.GateCaptureException
import com.project.analyzer.calibration.domain.usecase.LoadTrackCalibrationUseCase
import com.project.analyzer.calibration.domain.usecase.SaveTrackCalibrationUseCase
import com.project.analyzer.calibration.domain.usecase.flipDirection
import com.project.analyzer.calibration.presentation.formatDebugString
import com.project.analyzer.calibration.presentation.toDebugSnapshot
import com.project.analyzer.calibration.presentation.verify.state.CalibrationVerifyState
import com.project.analyzer.calibration.presentation.verify.state.EditingGate
import com.project.analyzer.leak.api.LeakAwareViewModel
import com.project.analyzer.math.Pose2D
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationCarPose
import com.project.analyzer.telemetry.ac.api.debug.AcCalibrationDebugLapAnalyzer
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

@Inject
internal class CalibrationVerifyViewModel(
    private val loadUseCase: LoadTrackCalibrationUseCase,
    private val saveUseCase: SaveTrackCalibrationUseCase,
    private val captureGate: CaptureGateOnStandstillUseCase,
    private val sampleProvider: TelemetrySampleProvider,
    private val lapAnalyzer: AcCalibrationDebugLapAnalyzer,
    statePublisherFactory: UiStatePublisherFactory,
) : LeakAwareViewModel() {

    private val _state = MutableStateFlow(CalibrationVerifyState())
    val state: StateFlow<CalibrationVerifyState> = _state.asStateFlow()

    private val statePublisher = statePublisherFactory.create(
        state = _state,
        lapAnalyzer = lapAnalyzer,
    )

    private var loadJob: Job? = null
    private var sampleJob: Job? = null
    private var calibration: TrackCalibration? = null

    private var lastPosForVelocity: Vec2? = null
    private var lastTsForVelocityNs: Long = 0L

    private var prevPoseForUiCrossing: AcCalibrationCarPose? = null

    init {
        viewModelScope.launch {
            sampleProvider.sample.collect { sample ->
                _state.update { it.copy(debugTelemetry = buildDebugString(sample)) }
            }
        }
    }

    fun start(trackId: String) {
        resetVerificationRuntime()

        loadJob = viewModelScope.launch {
            _state.update { it.copy(trackId = trackId, message = "Loading $trackId…") }

            val loadedCalibration = loadUseCase.load(trackId)
            if (!isActive || _state.value.trackId != trackId) return@launch
            if (loadedCalibration == null) {
                showMissingCalibration(trackId)
                return@launch
            }

            activateCalibration(trackId, loadedCalibration)
        }
    }

    private fun resetVerificationRuntime() {
        loadJob?.cancel()
        sampleJob?.cancel()
        lapAnalyzer.resetSession()
        statePublisher.clearGateCrossings()
        prevPoseForUiCrossing = null
        lastPosForVelocity = null
        lastTsForVelocityNs = 0L
        calibration = null
    }

    private fun showMissingCalibration(trackId: String) {
        _state.update {
            it.copy(
                calibration = null,
                isRunning = false,
                message = "No calibration found for $trackId",
            )
        }
    }

    private fun activateCalibration(trackId: String, loadedCalibration: TrackCalibration) {
        calibration = loadedCalibration
        sampleProvider.setReferencePoint(loadedCalibration.referencePoint)
        lapAnalyzer.loadCalibration(trackId, loadedCalibration)

        _state.update {
            it.copy(
                trackId = trackId,
                calibration = loadedCalibration,
                isRunning = true,
                message = "Loaded. Drive and cross SF/sectors to verify.",
            )
        }

        sampleJob = viewModelScope.launch {
            sampleProvider.sample.collectLatest(::processVerificationSample)
        }
    }

    private fun processVerificationSample(sample: CalibrationSample) {
        val pose = sample.pose ?: return
        val activeCalibration = calibration ?: return

        val nowNs = sample.timestampNs.takeIf { it > 0L } ?: System.nanoTime()
        val nowMs = System.currentTimeMillis()
        val carPose = buildCarPose(sample = sample, pose = pose)

        lapAnalyzer.processPose(nowNs, carPose, activeCalibration)
        markUiCrossingIfNeeded(nowNs, nowMs, carPose, activeCalibration)
        statePublisher.publish(
            nowNs = nowNs,
            carPose = carPose,
            calibration = activeCalibration,
            speedKmh = sample.speedKmh,
        )
    }

    private fun buildCarPose(sample: CalibrationSample, pose: Pose2D): AcCalibrationCarPose {
        val headingDirection = pose.forward.normalized()
        val velocityDirection = computeVelocityDir(sample, pose.pos) ?: headingDirection
        return AcCalibrationCarPose(
            position = pose.pos,
            velocityDir = velocityDirection,
            headingDir = headingDirection,
            speedKmh = sample.speedKmh,
            isMovingForward = velocityDirection.dot(headingDirection) >= 0f,
        )
    }

    private fun markUiCrossingIfNeeded(
        nowNs: Long,
        nowMs: Long,
        carPose: AcCalibrationCarPose,
        calibration: TrackCalibration,
    ) {
        val previousPose = prevPoseForUiCrossing ?: run {
            prevPoseForUiCrossing = carPose
            return
        }
        val snapshot = lapAnalyzer.getSnapshot(nowNs)
        statePublisher.markCrossingsUsingDetector(
            previousPose,
            carPose,
            calibration,
            UiStatePublisher.CrossingContext(
                currentSectorIndex = snapshot.currentSectorIndex,
                isLapRunning = snapshot.isLapRunning,
                nowMs = nowMs,
            ),
        )
        prevPoseForUiCrossing = carPose
    }

    fun stop() {
        loadJob?.cancel()
        sampleJob?.cancel()
        loadJob = null
        sampleJob = null
        prevPoseForUiCrossing = null
        _state.update { it.copy(isRunning = false, message = "Stopped") }
    }

    fun resetSession() {
        lapAnalyzer.resetSession()
        statePublisher.clearGateCrossings()
        prevPoseForUiCrossing = null
        lastPosForVelocity = null
        lastTsForVelocityNs = 0L

        _state.update {
            it.copy(
                lapRunning = false,
                lapIndex = 0,
                currentLapMs = 0L,
                currentSectorIndex = 1,
                currentSectorMs = 0L,
                lastLapMs = null,
                bestLapMs = null,
                lastS1Ms = null,
                lastS2Ms = null,
                lastS3Ms = null,
                bestS1Ms = null,
                bestS2Ms = null,
                bestS3Ms = null,
                lastEvent = "Reset session",
                message = "Session reset",
            )
        }
    }

    fun startEditingGate(gate: EditingGate) {
        _state.update { it.copy(editingGate = gate) }
    }

    fun cancelEditing() {
        _state.update { it.copy(editingGate = null, isCapturing = false) }
    }

    fun onRadiusChanged(radius: Float) {
        _state.update { it.copy(halfWidthMeters = radius) }
    }

    fun flipGateDirection(gate: EditingGate) {
        val currentCalibration = calibration ?: return
        viewModelScope.launch {
            val updatedCalibration = currentCalibration.flipGateDirection(gate) ?: return@launch
            saveAndReloadCalibration(updatedCalibration)

            _state.update {
                it.copy(
                    calibration = updatedCalibration,
                    message = "✓ ${gate.name} direction flipped and saved",
                )
            }
        }
    }

    fun captureCurrentGate() {
        val editingGate = _state.value.editingGate ?: return
        val currentCalibration = calibration ?: return

        viewModelScope.launch {
            _state.update { it.copy(isCapturing = true, message = "⏳ Stop on the line and wait (~2 sec)...") }

            try {
                val result = captureGate.captureWithDetails(halfWidthMeters = state.value.halfWidthMeters)
                val updatedCalibration = currentCalibration.replaceGate(
                    gate = editingGate,
                    replacement = result.gate,
                )
                saveAndReloadCalibration(updatedCalibration)

                val posInfo = "pos=(%.2f, %.2f)".format(result.capturedPosition.x, result.capturedPosition.y)

                _state.update {
                    it.copy(
                        calibration = updatedCalibration,
                        editingGate = null,
                        isCapturing = false,
                        message = "✓ ${editingGate.name} updated: $posInfo",
                    )
                }
            } catch (e: GateCaptureException) {
                _state.update { it.copy(isCapturing = false, message = "❌ ${e.message}") }
            } catch (e: Exception) {
                _state.update { it.copy(isCapturing = false, message = "❌ Error: ${e.message}") }
            }
        }
    }

    private suspend fun saveAndReloadCalibration(updatedCalibration: TrackCalibration) {
        saveUseCase.save(updatedCalibration)
        calibration = updatedCalibration
        lapAnalyzer.loadCalibration(updatedCalibration.trackId, updatedCalibration)
    }

    private fun computeVelocityDir(sample: CalibrationSample, posePos: Vec2): Vec2? {
        val ts = sample.timestampNs.takeIf { it > 0L } ?: return null
        val prevPos = lastPosForVelocity
        val prevTs = lastTsForVelocityNs

        lastPosForVelocity = posePos
        lastTsForVelocityNs = ts

        if (prevPos == null || prevTs == 0L) return null

        val dt = (ts - prevTs).toFloat() / 1_000_000_000f
        if (dt <= 0f) return null

        val d = posePos - prevPos
        val dist = d.len()
        if (dist < 0.01f) return null

        return d.normalized()
    }

    private fun buildDebugString(sample: CalibrationSample): String {
        val snapshot = sample.toDebugSnapshot() ?: return "Waiting for telemetry..."
        return snapshot.formatDebugString(
            speedKmh = sample.speedKmh,
            directionLabel = "DIR (movement)",
            direction = computeVelocityDir(sample, snapshot.pose.pos) ?: snapshot.pose.forward,
        )
    }

    private fun TrackCalibration.flipGateDirection(gate: EditingGate): TrackCalibration? {
        val currentGate = gateToUpdate(gate) ?: return null
        return replaceGate(gate = gate, replacement = currentGate.flipDirection())
    }

    private fun TrackCalibration.replaceGate(gate: EditingGate, replacement: Gate): TrackCalibration = when (gate) {
        EditingGate.START_FINISH -> copy(
            startFinish = replacement,
            sectors = sectors.map { sector ->
                when (sector.index) {
                    1 -> sector.copy(start = replacement)
                    3 -> sector.copy(finish = replacement)
                    else -> sector
                }
            },
        )

        EditingGate.SECTOR_1_FINISH -> copy(
            sectors = sectors.map { sector ->
                when (sector.index) {
                    1 -> sector.copy(finish = replacement)
                    2 -> sector.copy(start = replacement)
                    else -> sector
                }
            },
        )

        EditingGate.SECTOR_2_FINISH -> copy(
            sectors = sectors.map { sector ->
                when (sector.index) {
                    2 -> sector.copy(finish = replacement)
                    3 -> sector.copy(start = replacement)
                    else -> sector
                }
            },
        )
    }

    private fun TrackCalibration.gateToUpdate(gate: EditingGate): Gate? = when (gate) {
        EditingGate.START_FINISH -> startFinish
        EditingGate.SECTOR_1_FINISH -> sectors.find { it.index == 1 }?.finish
        EditingGate.SECTOR_2_FINISH -> sectors.find { it.index == 2 }?.finish
    }
}
