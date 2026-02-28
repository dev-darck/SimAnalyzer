package com.project.analyzer.calibration.presentation.trackmap

import androidx.lifecycle.viewModelScope
import com.project.analyzer.calibration.trackmap.TrackMapRecorder
import com.project.analyzer.calibration.trackmap.TrackMapRecorderState
import com.project.analyzer.leak.api.LeakAwareViewModel
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Inject
internal class TrackMapBuilderViewModel(private val recorder: TrackMapRecorder) : LeakAwareViewModel() {

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
