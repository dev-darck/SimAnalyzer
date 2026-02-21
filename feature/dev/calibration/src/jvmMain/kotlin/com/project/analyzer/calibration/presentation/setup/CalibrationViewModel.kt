package com.project.analyzer.calibration.presentation.setup

import androidx.lifecycle.viewModelScope
import com.project.analyzer.calibration.data.model.CalibrationSample
import com.project.analyzer.calibration.di.OverlayDebugBus
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.calibration.domain.usecase.CaptureGateOnStandstillUseCase
import com.project.analyzer.calibration.domain.usecase.GateCaptureException
import com.project.analyzer.calibration.domain.usecase.SaveTrackCalibrationUseCase
import com.project.analyzer.calibration.domain.usecase.flipDirection
import com.project.analyzer.calibration.presentation.components.fmt
import com.project.analyzer.calibration.presentation.overlay.OverlayPublisher
import com.project.analyzer.calibration.presentation.overlay.state.CapturePoint
import com.project.analyzer.calibration.presentation.setup.state.CalibrationState
import com.project.analyzer.leak.api.LeakAwareViewModel
import com.project.analyzer.telemetry.ac.api.model.calibration.Gate
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.atan2

@Inject
class CalibrationViewModel(
    private val captureGate: CaptureGateOnStandstillUseCase,
    private val saveUseCase: SaveTrackCalibrationUseCase,
    private val sampleProvider: TelemetrySampleProvider,
    overlayDebugBus: OverlayDebugBus,
) : LeakAwareViewModel() {

    private val _state = MutableStateFlow(CalibrationState())
    val state: StateFlow<CalibrationState> = _state

    private val overlayPublisher = OverlayPublisher(overlayDebugBus)
    private var lastCapturePoint: CapturePoint? = null

    init {
        sampleProvider.setReferencePoint(_state.value.referencePoint)
        viewModelScope.launch {
            sampleProvider.sample.collect { sample ->
                val debugText = buildDebugString(sample)
                val pose = sample.pose
                val trackId = sample.trackId
                val trackName = sample.trackName
                val carModel = sample.carModel

                _state.update {
                    val resolvedTrackName = when {
                        it.trackName.isNotBlank() -> it.trackName
                        !trackName.isNullOrBlank() -> trackName
                        else -> it.trackName
                    }
                    val resolvedTrackId = when {
                        it.trackId.isNotBlank() -> it.trackId
                        !trackId.isNullOrBlank() -> trackId
                        !resolvedTrackName.isNullOrBlank() -> slugify(resolvedTrackName)
                        else -> it.trackId
                    }
                    it.copy(
                        debugTelemetry = debugText,
                        currentPosition = pose?.pos,
                        currentForward = pose?.forward,
                        speedKmh = sample.speedKmh,
                        headingDegrees = Math.toDegrees(sample.headingRad.toDouble()).toFloat(),
                        sessionTrackName = trackName,
                        sessionTrackId = trackId,
                        sessionCarModel = carModel,
                        trackName = resolvedTrackName,
                        trackId = resolvedTrackId,
                    )
                }

                val st = _state.value
                val gates = buildMap {
                    st.startFinish?.let { put("SF", it) }
                    st.sectorStartMarks.forEachIndexed { idx, g ->
                        val sectorIdx = idx + 2
                        put("S${sectorIdx}_START", g)
                    }
                }

                overlayPublisher.publish(
                    trackId = st.trackId.takeIf { it.isNotBlank() },
                    speedKmh = sample.speedKmh,
                    carPos = pose?.pos,
                    carDir = pose?.forward,
                    gates = gates,
                    gateInfoOverride = null,
                    timing = null,
                    lastCapturePoint = lastCapturePoint,
                    pendingCapturePosition = if (st.isBusy) pose?.pos else null,
                )
            }
        }
        viewModelScope.launch {
        }
    }

    fun dispatch(intent: CalibrationIntent) {
        when (intent) {
            is CalibrationIntent.TrackNameChanged -> onTrackName(intent.value)
            is CalibrationIntent.ReferencePointChanged -> onReferencePoint(intent.value)
            is CalibrationIntent.TriggerRadiusChanged -> onTriggerRadiusChanged(intent.meters)
            CalibrationIntent.LoadAllCalibrations -> onLoadAllCalibrations()
            CalibrationIntent.CaptureStartFinish -> onCaptureStartFinish()
            is CalibrationIntent.CaptureSectorStart -> onCaptureSectorStart(intent.index)
            CalibrationIntent.AddSector -> onAddSector()
            is CalibrationIntent.RemoveSector -> onRemoveSector(intent.index)
            CalibrationIntent.FlipStartFinishDirection -> onFlipStartFinishDirection()
            is CalibrationIntent.FlipSectorStartDirection -> onFlipSectorStartDirection(intent.index)
            CalibrationIntent.Save -> save()
            CalibrationIntent.Reset -> onReset()
        }
    }

    private fun onTrackName(name: String) {
        val id = slugify(name)
        _state.update { it.copy(trackName = name, trackId = id) }
    }

    private fun onReferencePoint(rp: ReferencePoint) {
        sampleProvider.setReferencePoint(rp)
        _state.update { it.copy(referencePoint = rp) }
    }

    private fun onTriggerRadiusChanged(meters: Float) {
        _state.update { it.copy(halfWidthMeters = meters) }
    }

    private fun onLoadAllCalibrations() {
        viewModelScope.launch {
            val listOfData = saveUseCase.loadAll()
            _state.update { it.copy(listOfData = listOfData) }
        }
    }

    private fun onCaptureStartFinish() {
        capture("SF") { gate ->
            _state.update { it.copy(startFinish = gate, message = "✓ Start/Finish recorded") }
        }
    }

    private fun onCaptureSectorStart(index: Int) {
        capture("S${index}_START") { gate ->
            _state.update { s ->
                if (index < 2 || index > s.sectorCount) return@update s
                val listIndex = index - 2
                val updated = s.sectorStartMarks.toMutableList().apply { this[listIndex] = gate }
                s.copy(sectorStartMarks = updated, message = "✓ Sector S$index START updated")
            }
        }
    }

    private fun onAddSector() {
        capture("S${_state.value.sectorCount + 2}_START") { gate ->
            _state.update { s ->
                val nextIndex = s.sectorCount + 1
                s.copy(
                    sectorStartMarks = s.sectorStartMarks + gate,
                    message = "✓ Sector S$nextIndex START recorded",
                )
            }
        }
    }

    private fun onRemoveSector(index: Int) {
        _state.update { s ->
            if (index < 2 || index > s.sectorCount) return@update s
            val listIndex = index - 2
            val updated = s.sectorStartMarks.toMutableList().apply { removeAt(listIndex) }
            s.copy(sectorStartMarks = updated, message = "✓ Removed sector S$index")
        }
    }

    private fun onFlipStartFinishDirection() {
        _state.update { s ->
            s.startFinish?.flipDirection()?.let {
                s.copy(startFinish = it, message = "✓ Start/Finish direction flipped")
            } ?: s
        }
    }

    private fun onFlipSectorStartDirection(index: Int) {
        _state.update { s ->
            if (index < 2 || index > s.sectorCount) return@update s
            val listIndex = index - 2
            val current = s.sectorStartMarks[listIndex]
            val flipped = current.flipDirection()
            val updated = s.sectorStartMarks.toMutableList().apply { this[listIndex] = flipped }
            s.copy(sectorStartMarks = updated, message = "✓ Sector S$index START direction flipped")
        }
    }

    private fun onReset() {
        _state.value = CalibrationState()
    }

    private fun capture(label: String, onCaptured: (Gate) -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isBusy = true,
                    message = "⏳ Stop on the line and wait (~2 sec)...",
                )
            }
            try {
                val result = captureGate.captureWithDetails(
                    halfWidthMeters = s.halfWidthMeters,
                )

                lastCapturePoint = CapturePoint(
                    position = result.capturedPosition,
                    forward = result.capturedForward,
                    label = label,
                )

                val posInfo = "pos=(%.2f, %.2f)".format(result.capturedPosition.x, result.capturedPosition.y)
                val fwdInfo = "fwd=(%.2f, %.2f)".format(result.capturedForward.x, result.capturedForward.y)
                val qualityInfo = "std=%.3fm, %d samples".format(result.positionStdMeters, result.sampleCount)

                onCaptured(result.gate)

                _state.update {
                    it.copy(
                        message = "✓ Captured: $posInfo | $fwdInfo | $qualityInfo",
                    )
                }
            } catch (e: GateCaptureException) {
                _state.update { it.copy(message = "❌ ${e.message}") }
            } catch (e: Exception) {
                _state.update { it.copy(message = "❌ Error: ${e.message}") }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun save() {
        val s = _state.value
        if (!s.isReadyToSave()) {
            _state.update { it.copy(message = "❌ Not ready: Start/Finish + all sector START/FINISH required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, message = "⏳ Saving calibration…") }
            try {
                val sectors = (1..s.sectorCount).map { i ->
                    SectorCalibration(
                        index = i,
                        start = requireNotNull(s.sectorStart(i)) { "Sector S$i start missing" },
                        finish = requireNotNull(s.sectorFinish(i)) { "Sector S$i finish missing" },
                    )
                }

                val calibration = TrackCalibration(
                    trackId = s.trackId,
                    trackName = s.trackName,
                    createdAtEpochMs = System.currentTimeMillis(),
                    referencePoint = s.referencePoint,
                    startFinish = requireNotNull(s.startFinish),
                    sectors = sectors,
                )

                saveUseCase.save(calibration)

                _state.update { current ->
                    current.copy(
                        startFinish = null,
                        lastSavedTrackId = current.trackId,
                        message = "✓ Saved: ${current.trackId}",
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(message = "❌ Error saving: ${e.message}") }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
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
            
            FORWARD: ${fmt(p.forward)}  |  Heading: ${"%.1f".format(headingFromForward)}°
            
            FL: ${fmt(w?.fl)}   FR: ${fmt(w?.fr)}
            RL: ${fmt(w?.rl)}   RR: ${fmt(w?.rr)}
        """.trimIndent()
    }

    private fun slugify(text: String): String = text
        .lowercase()
        .trim()
        .replace(Regex("""\s+"""), "_")
        .replace(Regex("""[^a-z0-9_]+"""), "_")
        .replace(Regex("""_+"""), "_")
        .trim('_')
}
