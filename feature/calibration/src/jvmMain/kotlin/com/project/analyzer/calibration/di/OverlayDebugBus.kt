package com.project.analyzer.calibration.di

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.calibration.presentation.overlay.state.OverlayDebugState
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

@SingleIn(ScreenScope::class)
class OverlayDebugBus @Inject constructor() {
    private val _state = MutableStateFlow(OverlayDebugState())
    val state: StateFlow<OverlayDebugState> = _state
    fun update(reducer: (OverlayDebugState) -> OverlayDebugState) = _state.update(reducer)
}
