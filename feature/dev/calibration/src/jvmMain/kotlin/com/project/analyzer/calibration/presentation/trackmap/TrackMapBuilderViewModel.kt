package com.project.analyzer.calibration.presentation.trackmap

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.trackmap.TrackMapRecorder
import com.project.analyzer.calibration.trackmap.TrackMapRecorderState
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Inject
@ViewModelKey(TrackMapBuilderViewModel::class)
@ContributesIntoMap(ScreenScope::class)
class TrackMapBuilderViewModel(private val recorder: TrackMapRecorder) : ViewModel() {

    val state: StateFlow<TrackMapRecorderState> = recorder.state

    fun start() {
        recorder.start()
    }

    fun stop() {
        recorder.stop()
    }

    fun reset() {
        recorder.reset()
    }

    fun setReferencePoint(point: ReferencePoint) {
        recorder.setReferencePoint(point)
    }

    fun setFallbackHalfWidthMeters(value: Float) {
        recorder.setFallbackHalfWidthMeters(value)
    }

    fun markPitEntry() {
        recorder.markPitEntry()
    }

    fun markPitExit() {
        recorder.markPitExit()
    }

    fun save() {
        viewModelScope.launch {
            recorder.save()
        }
    }
}
