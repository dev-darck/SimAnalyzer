package com.project.analyzer.calibration.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.calibration.data.AcTelemetrySampleProvider
import com.project.analyzer.calibration.data.model.Gate
import com.project.analyzer.calibration.data.model.ReferencePoint
import com.project.analyzer.calibration.data.model.SectorCalibration
import com.project.analyzer.calibration.data.model.TrackCalibration
import com.project.analyzer.calibration.domain.usecase.CaptureGateOnStandstillUseCase
import com.project.analyzer.calibration.domain.usecase.GateCaptureException
import com.project.analyzer.calibration.domain.usecase.SaveTrackCalibrationUseCase
import com.project.analyzer.calibration.presentation.state.CalibrationState
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Inject
@ViewModelKey(CalibrationViewModel::class)
@ContributesIntoMap(AppScope::class)
class CalibrationViewModel(
    private val captureGate: CaptureGateOnStandstillUseCase,
    private val saveUseCase: SaveTrackCalibrationUseCase,
    private val sampleProvider: AcTelemetrySampleProvider,
) : ViewModel() {

    private val _state = MutableStateFlow(CalibrationState())
    val state: StateFlow<CalibrationState> = _state

    private val sectorCount = 3

    fun dispatch(intent: CalibrationIntent) {
        when (intent) {
            is CalibrationIntent.TrackNameChanged -> onTrackName(intent.value)
            is CalibrationIntent.ReferencePointChanged -> onReferencePoint(intent.value)
            is CalibrationIntent.TriggerRadiusChanged ->
                _state.update { it.copy(triggerRadiusMeters = intent.meters) }
            is CalibrationIntent.DebugWidthChanged ->
                _state.update { it.copy(debugHalfWidthMeters = intent.halfWidthMeters) }

            CalibrationIntent.CaptureStartFinish -> capture { gate ->
                applyStartFinish(gate)
                _state.update { it.copy(message = "Start/Finish recorded") }
            }

            CalibrationIntent.CaptureSplit1 -> capture { gate ->
                applySplit1(gate)
                _state.update { it.copy(message = "Split1 recorded (S1 end / S2 start)") }
            }

            CalibrationIntent.CaptureSplit2 -> capture { gate ->
                applySplit2(gate)
                _state.update { it.copy(message = "Split2 recorded (S2 end / S3 start)") }
            }

            is CalibrationIntent.CaptureSectorStart -> capture { gate ->
                val idx = intent.index
                _state.update {
                    it.copy(
                        sectorStarts = it.sectorStarts + (idx to gate),
                        message = "Sector $idx START recorded"
                    )
                }
            }

            is CalibrationIntent.CaptureSectorFinish -> capture { gate ->
                val idx = intent.index
                _state.update {
                    it.copy(
                        sectorFinishes = it.sectorFinishes + (idx to gate),
                        message = "Sector $idx FINISH recorded"
                    )
                }
            }

            CalibrationIntent.Save -> save()
            CalibrationIntent.Reset -> _state.value = CalibrationState()
            is CalibrationIntent.SetFileToSave -> {
                saveUseCase.setPath(intent.path)
                _state.update { it.copy(savedPathHint = intent.path) }
            }
        }
    }

    private fun applyStartFinish(gate: Gate) {
        _state.update { s ->
            s.copy(
                startFinish = gate,
                sectorStarts = s.sectorStarts + (1 to gate),
                sectorFinishes = s.sectorFinishes + (3 to gate),
            )
        }
    }

    private fun applySplit1(gate: Gate) {
        _state.update { s ->
            s.copy(
                sectorFinishes = s.sectorFinishes + (1 to gate),
                sectorStarts = s.sectorStarts + (2 to gate),
            )
        }
    }

    private fun applySplit2(gate: Gate) {
        _state.update { s ->
            s.copy(
                sectorFinishes = s.sectorFinishes + (2 to gate),
                sectorStarts = s.sectorStarts + (3 to gate),
            )
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

    private fun capture(onCaptured: (Gate) -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            _state.update {
                it.copy(
                    isBusy = true,
                    message = "Stop on the line, then press the button"
                )
            }
            try {
                val gate = captureGate.capture(
                    triggerRadiusMeters = s.triggerRadiusMeters,
                    debugHalfWidthMeters = s.debugHalfWidthMeters
                )
                onCaptured(gate)
            } catch (e: GateCaptureException) {
                _state.update { it.copy(message = e.message) }
            } catch (e: Exception) {
                _state.update { it.copy(message = "Ошибка: ${e.message}") }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun save() {
        val s = _state.value
        if (!s.isReadyToSave(sectorCount)) {
            _state.update { it.copy(message = "Not ready: SF + Split1 + Split2 required") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isBusy = true, message = "Save to ${it.savedPathHint}…") }
            try {
                val sectors = (1..sectorCount).map { i ->
                    SectorCalibration(
                        index = i,
                        start = s.sectorStarts.getValue(i),
                        finish = s.sectorFinishes.getValue(i),
                    )
                }

                val calibration = TrackCalibration(
                    trackId = s.trackId,
                    trackName = s.trackName,
                    createdAtEpochMs = System.currentTimeMillis(),
                    referencePoint = s.referencePoint,
                    startFinish = requireNotNull(s.startFinish),
                    sectors = sectors
                )

                saveUseCase.save(calibration)

                _state.update { current ->
                    current.copy(
                        startFinish = null,
                        sectorStarts = emptyMap(),
                        sectorFinishes = emptyMap(),
                        lastSavedTrackId = current.trackId,
                        message = "Saved: ${current.trackId}.json"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(message = "Error during saving: ${e.message}") }
            } finally {
                _state.update { it.copy(isBusy = false) }
            }
        }
    }

    private fun slugify(text: String): String {
        val trimmed = text.trim().lowercase()
        val sb = StringBuilder(trimmed.length)
        for (c in trimmed) {
            when {
                c.isLetterOrDigit() -> sb.append(c)
                c == ' ' || c == '-' || c == '_' -> sb.append('_')
            }
        }
        return sb.toString().replace(Regex("_+"), "_").trim('_')
    }
}
