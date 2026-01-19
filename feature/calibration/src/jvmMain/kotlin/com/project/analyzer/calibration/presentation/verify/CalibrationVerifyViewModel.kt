package com.project.analyzer.calibration.presentation.verify

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.ac.telemetry.impl.fallback.analyzer.FallbackLapAnalyzer
import com.project.analyzer.ac.telemetry.impl.fallback.detector.GateCrossingDetector
import com.project.analyzer.ac.telemetry.impl.fallback.pose.model.CarPose
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.data.AcTelemetrySampleProvider
import com.project.analyzer.calibration.data.model.CalibrationSample
import com.project.analyzer.calibration.di.OverlayDebugBus
import com.project.analyzer.calibration.domain.usecase.CaptureGateOnStandstillUseCase
import com.project.analyzer.calibration.domain.usecase.GateCaptureException
import com.project.analyzer.calibration.domain.usecase.LoadTrackCalibrationUseCase
import com.project.analyzer.calibration.domain.usecase.SaveTrackCalibrationUseCase
import com.project.analyzer.calibration.domain.usecase.flipDirection
import com.project.analyzer.calibration.presentation.components.fmt
import com.project.analyzer.calibration.presentation.verify.state.CalibrationVerifyState
import com.project.analyzer.calibration.presentation.verify.state.EditingGate
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.atan2

@Inject
@ViewModelKey(CalibrationVerifyViewModel::class)
@ContributesIntoMap(ScreenScope::class)
class CalibrationVerifyViewModel(
    private val loadUseCase: LoadTrackCalibrationUseCase,
    private val saveUseCase: SaveTrackCalibrationUseCase,
    private val captureGate: CaptureGateOnStandstillUseCase,
    private val sampleProvider: AcTelemetrySampleProvider,
    private val lapAnalyzer: FallbackLapAnalyzer,
    gateCrossingDetector: GateCrossingDetector,
    overlayDebugBus: OverlayDebugBus
) : ViewModel() {

    private val _state = MutableStateFlow(CalibrationVerifyState())
    val state: StateFlow<CalibrationVerifyState> = _state.asStateFlow()

    private val statePublisher = UiStatePublisher(
        state = _state,
        overlayDebugBus = overlayDebugBus,
        lapAnalyzer = lapAnalyzer,
        gateDetector = gateCrossingDetector
    )

    private var job: Job? = null
    private var calibration: TrackCalibration? = null

    private var lastPosForVelocity: Vec2? = null
    private var lastTsForVelocityNs: Long = 0L

    private var prevPoseForUiCrossing: CarPose? = null

    init {
        viewModelScope.launch {
            sampleProvider.sample.collect { sample ->
                _state.update { it.copy(debugTelemetry = buildDebugString(sample)) }
            }
        }
    }

    fun start(trackId: String) {
        job?.cancel()
        lapAnalyzer.resetSession()
        statePublisher.clearGateCrossings()
        prevPoseForUiCrossing = null
        lastPosForVelocity = null
        lastTsForVelocityNs = 0L

        viewModelScope.launch {
            _state.update { it.copy(trackId = trackId, message = "Loading $trackId…") }

            val cal = loadUseCase.load(trackId)
            if (cal == null) {
                _state.update {
                    it.copy(
                        calibration = null,
                        isRunning = false,
                        message = "No calibration file for $trackId"
                    )
                }
                return@launch
            }

            calibration = cal
            sampleProvider.setReferencePoint(cal.referencePoint)
            lapAnalyzer.loadCalibration(trackId, cal)

            _state.update {
                it.copy(
                    trackId = trackId,
                    calibration = cal,
                    isRunning = true,
                    message = "Loaded. Drive and cross SF/sectors to verify."
                )
            }

            job = viewModelScope.launch {
                sampleProvider.sample.collectLatest { sample ->
                    val pose = sample.pose ?: return@collectLatest
                    val calNow = calibration ?: return@collectLatest

                    val nowNs = sample.timestampNs.takeIf { it > 0L } ?: System.nanoTime()
                    val nowMs = System.currentTimeMillis()

                    val headingDir = pose.forward.normalized()

                    val velocityDir = computeVelocityDir(sample, pose.pos)
                        ?: headingDir

                    val isMovingForward = velocityDir.dot(headingDir) >= 0f

                    val carPose = CarPose(
                        position = pose.pos,
                        velocityDir = velocityDir,
                        headingDir = headingDir,
                        speedKmh = sample.speedKmh,
                        isMovingForward = isMovingForward
                    )

                    lapAnalyzer.processPose(nowNs, carPose, calNow)

                    val prev = prevPoseForUiCrossing
                    if (prev != null) {
                        val snapshot = lapAnalyzer.getSnapshot(nowNs)
                        statePublisher.markCrossingsUsingDetector(
                            prev,
                            carPose,
                            calNow,
                            snapshot.currentSectorIndex,
                            snapshot.isLapRunning,
                            nowMs
                        )
                    }
                    prevPoseForUiCrossing = carPose

                    statePublisher.publish(
                        nowNs = nowNs,
                        carPose = carPose,
                        calibration = calNow,
                        speedKmh = sample.speedKmh
                    )
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
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
                message = "Session reset"
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
        val cal = calibration ?: return
        viewModelScope.launch {
            val updatedCalibration = when (gate) {
                EditingGate.START_FINISH -> {
                    val flipped = cal.startFinish.flipDirection()
                    cal.copy(
                        startFinish = flipped,
                        sectors = cal.sectors.map { sector ->
                            when (sector.index) {
                                1 -> sector.copy(start = flipped)
                                3 -> sector.copy(finish = flipped)
                                else -> sector
                            }
                        }
                    )
                }

                EditingGate.SECTOR_1_FINISH -> {
                    val original = cal.sectors.find { it.index == 1 }?.finish ?: return@launch
                    val flipped = original.flipDirection()
                    cal.copy(
                        sectors = cal.sectors.map { sector ->
                            when (sector.index) {
                                1 -> sector.copy(finish = flipped)
                                2 -> sector.copy(start = flipped)
                                else -> sector
                            }
                        }
                    )
                }

                EditingGate.SECTOR_2_FINISH -> {
                    val original = cal.sectors.find { it.index == 2 }?.finish ?: return@launch
                    val flipped = original.flipDirection()
                    cal.copy(
                        sectors = cal.sectors.map { sector ->
                            when (sector.index) {
                                2 -> sector.copy(finish = flipped)
                                3 -> sector.copy(start = flipped)
                                else -> sector
                            }
                        }
                    )
                }
            }

            saveUseCase.save(updatedCalibration)
            calibration = updatedCalibration
            lapAnalyzer.loadCalibration(updatedCalibration.trackId, updatedCalibration)

            _state.update {
                it.copy(
                    calibration = updatedCalibration,
                    message = "✓ ${gate.name} direction flipped and saved"
                )
            }
        }
    }

    fun captureCurrentGate() {
        val editingGate = _state.value.editingGate ?: return
        val cal = calibration ?: return

        viewModelScope.launch {
            _state.update { it.copy(isCapturing = true, message = "⏳ Stop on the line and wait (~2 sec)...") }

            try {
                val result = captureGate.captureWithDetails(halfWidthMeters = state.value.halfWidthMeters)
                val newGate = result.gate

                val updatedCalibration = when (editingGate) {
                    EditingGate.START_FINISH -> cal.copy(
                        startFinish = newGate,
                        sectors = cal.sectors.map { sector ->
                            when (sector.index) {
                                1 -> sector.copy(start = newGate)
                                3 -> sector.copy(finish = newGate)
                                else -> sector
                            }
                        }
                    )

                    EditingGate.SECTOR_1_FINISH -> cal.copy(
                        sectors = cal.sectors.map { sector ->
                            when (sector.index) {
                                1 -> sector.copy(finish = newGate)
                                2 -> sector.copy(start = newGate)
                                else -> sector
                            }
                        }
                    )

                    EditingGate.SECTOR_2_FINISH -> cal.copy(
                        sectors = cal.sectors.map { sector ->
                            when (sector.index) {
                                2 -> sector.copy(finish = newGate)
                                3 -> sector.copy(start = newGate)
                                else -> sector
                            }
                        }
                    )
                }

                saveUseCase.save(updatedCalibration)

                calibration = updatedCalibration
                lapAnalyzer.loadCalibration(updatedCalibration.trackId, updatedCalibration)

                val posInfo = "pos=(%.2f, %.2f)".format(result.capturedPosition.x, result.capturedPosition.y)

                _state.update {
                    it.copy(
                        calibration = updatedCalibration,
                        editingGate = null,
                        isCapturing = false,
                        message = "✓ ${editingGate.name} updated: $posInfo"
                    )
                }
            } catch (e: GateCaptureException) {
                _state.update { it.copy(isCapturing = false, message = "❌ ${e.message}") }
            } catch (e: Exception) {
                _state.update { it.copy(isCapturing = false, message = "❌ Error: ${e.message}") }
            }
        }
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
        val p = sample.pose ?: return "Waiting for telemetry..."
        val w = sample.wheels

        val headingFromForward = if (p.forward.len() > 0.01f) {
            Math.toDegrees(atan2(p.forward.x.toDouble(), p.forward.y.toDouble())).toFloat()
        } else {
            0f
        }

        return """
            POS: ${fmt(p.pos)}  |  Speed: ${"%.1f".format(sample.speedKmh)} km/h
            
            DIR (movement): ${fmt(computeVelocityDir(sample, p.pos) ?: p.forward)}  
            Heading(from forward): ${"%.1f".format(headingFromForward)}°
            
            FL: ${fmt(w?.fl)}   FR: ${fmt(w?.fr)}
            RL: ${fmt(w?.rl)}   RR: ${fmt(w?.rr)}
        """.trimIndent()
    }
}
