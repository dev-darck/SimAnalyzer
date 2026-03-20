package com.analyzer.trackmap.presentation

import androidx.lifecycle.viewModelScope
import com.analyzer.trackmap.domain.TrackMapCaptureController
import com.analyzer.trackmap.presentation.model.TrackMapBuilderIntent
import com.analyzer.trackmap.presentation.model.TrackMapBuilderUiState
import com.project.analyzer.leak.api.LeakAwareMviViewModel
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

@Inject
internal class TrackMapBuilderViewModel(
    private val controller: TrackMapCaptureController,
) : LeakAwareMviViewModel<TrackMapBuilderIntent, TrackMapBuilderUiState>(
    controller.state.value.toTrackMapBuilderUiState(),
) {

    init {
        controller.state
            .map { currentState -> currentState.toTrackMapBuilderUiState() }
            .onEach(::setState)
            .launchIn(viewModelScope)
    }

    override suspend fun handleIntent(intent: TrackMapBuilderIntent) {
        when (intent) {
            TrackMapBuilderIntent.Start -> controller.start()
            TrackMapBuilderIntent.Stop -> controller.stop()
            TrackMapBuilderIntent.Reset -> controller.reset()
            TrackMapBuilderIntent.Save -> controller.save()
            TrackMapBuilderIntent.MarkPitEntry -> controller.markPitEntry()
            TrackMapBuilderIntent.MarkPitExit -> controller.markPitExit()
            is TrackMapBuilderIntent.SetReferencePoint -> controller.setReferencePoint(intent.point)
            is TrackMapBuilderIntent.SetFallbackHalfWidthMeters -> controller.setFallbackHalfWidthMeters(intent.value)
        }
    }
}
