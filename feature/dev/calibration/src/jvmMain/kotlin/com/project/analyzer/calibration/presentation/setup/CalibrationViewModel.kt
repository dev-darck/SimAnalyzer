package com.project.analyzer.calibration.presentation.setup

import androidx.lifecycle.viewModelScope
import com.project.analyzer.calibration.di.OverlayDebugBus
import com.project.analyzer.calibration.domain.TelemetrySampleProvider
import com.project.analyzer.calibration.domain.model.CalibrationSample
import com.project.analyzer.calibration.domain.usecase.CaptureGateOnStandstillUseCase
import com.project.analyzer.calibration.domain.usecase.GateCaptureException
import com.project.analyzer.calibration.domain.usecase.SaveTrackCalibrationUseCase
import com.project.analyzer.calibration.domain.usecase.TrackCalibrationDraft
import com.project.analyzer.calibration.domain.usecase.flipDirection
import com.project.analyzer.calibration.presentation.model.CalibrationGateUi
import com.project.analyzer.calibration.presentation.model.toDomain
import com.project.analyzer.calibration.presentation.model.toUi
import com.project.analyzer.calibration.presentation.formatDebugString
import com.project.analyzer.calibration.presentation.model.CalibrationReferencePointUi
import com.project.analyzer.calibration.presentation.overlay.OverlayPublisher
import com.project.analyzer.calibration.presentation.overlay.state.CapturePoint
import com.project.analyzer.calibration.presentation.setup.state.CalibrationState
import com.project.analyzer.calibration.presentation.setup.state.isReadyToSave
import com.project.analyzer.calibration.presentation.toDebugSnapshot
import com.project.analyzer.leak.api.LeakAwareViewModel
import com.project.analyzer.utils.logger.logger
import com.project.analyzer.utils.toSlugId
import dev.zacsweers.metro.Inject
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
internal class CalibrationViewModel(
    private val captureGate: CaptureGateOnStandstillUseCase,
    private val saveUseCase: SaveTrackCalibrationUseCase,
    private val sampleProvider: TelemetrySampleProvider,
    overlayDebugBus: OverlayDebugBus,
) : LeakAwareViewModel() {

    private val logger = logger()

    private val _state = MutableStateFlow(CalibrationState())
    val state: StateFlow<CalibrationState> = _state

    private val overlayPublisher = OverlayPublisher(overlayDebugBus)
    private var lastCapturePoint: CapturePoint? = null

    init {
        sampleProvider.setReferencePoint(_state.value.referencePoint.toDomain())
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
                        resolvedTrackName.isNotBlank() -> slugify(resolvedTrackName)
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

    private fun onReferencePoint(rp: CalibrationReferencePointUi) {
        sampleProvider.setReferencePoint(rp.toDomain())
        _state.update { it.copy(referencePoint = rp) }
    }

    private fun onTriggerRadiusChanged(meters: Float) {
        _state.update { it.copy(halfWidthMeters = meters) }
    }

    private fun onLoadAllCalibrations() {
        viewModelScope.launch {
            val listOfData = saveUseCase.loadAll()
            _state.update { it.copy(listOfData = listOfData.toPersistentList()) }
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
                s.copy(sectorStartMarks = updated.toPersistentList(), message = "✓ Sector S$index START updated")
            }
        }
    }

    private fun onAddSector() {
        capture("S${_state.value.sectorCount + 2}_START") { gate ->
            _state.update { s ->
                val nextIndex = s.sectorCount + 1
                s.copy(
                    sectorStartMarks = (s.sectorStartMarks + gate).toPersistentList(),
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
            s.copy(sectorStartMarks = updated.toPersistentList(), message = "✓ Removed sector S$index")
        }
    }

    private fun onFlipStartFinishDirection() {
        _state.update { s ->
            s.startFinish?.toDomain()?.flipDirection()?.toUi()?.let {
                s.copy(startFinish = it, message = "✓ Start/Finish direction flipped")
            } ?: s
        }
    }

    private fun onFlipSectorStartDirection(index: Int) {
        _state.update { s ->
            if (index < 2 || index > s.sectorCount) return@update s
            val listIndex = index - 2
            val current = s.sectorStartMarks[listIndex]
            val flipped = current.toDomain().flipDirection().toUi()
            val updated = s.sectorStartMarks.toMutableList().apply { this[listIndex] = flipped }
            s.copy(sectorStartMarks = updated.toPersistentList(), message = "✓ Sector S$index START direction flipped")
        }
    }

    private fun onReset() {
        val resetState = CalibrationState()
        sampleProvider.setReferencePoint(resetState.referencePoint.toDomain())
        _state.value = resetState
    }

    private fun capture(
        label: String,
        onCaptured: (CalibrationGateUi) -> Unit,
    ) {
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

                onCaptured(result.gate.toUi())

                _state.update {
                    it.copy(
                        message = "✓ Captured: $posInfo | $fwdInfo | $qualityInfo",
                    )
                }
            } catch (e: GateCaptureException) {
                _state.update { it.copy(message = "❌ ${e.message}") }
            } catch (e: Exception) {
                logger.error(e) { "Gate capture failed" }
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
                saveUseCase.save(
                    TrackCalibrationDraft(
                        trackId = s.trackId,
                        trackName = s.trackName,
                        referencePoint = s.referencePoint.toDomain(),
                        startFinish = requireNotNull(s.startFinish).toDomain(),
                        sectorStartMarks = s.sectorStartMarks.map { it.toDomain() },
                    ),
                )

                _state.update { current ->
                    current.copy(
                        startFinish = null,
                        lastSavedTrackId = current.trackId,
                        message = "✓ Saved: ${current.trackId}",
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to save calibration" }
                _state.update { it.copy(message = "❌ Error saving: ${e.message}") }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun buildDebugString(sample: CalibrationSample): String {
        val snapshot = sample.toDebugSnapshot() ?: return "Waiting for telemetry..."
        return snapshot.formatDebugString(
            speedKmh = sample.speedKmh,
            directionLabel = "FORWARD",
            direction = snapshot.pose.forward,
        )
    }

    private fun slugify(text: String): String = text.toSlugId()
}
