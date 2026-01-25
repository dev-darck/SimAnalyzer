package com.project.analyzer.inputs.presentation

import com.project.analyzer.inputs.presentation.model.InputsSeries
import com.project.analyzer.inputs.settings.InputHudSettings

internal data class InputsHudUiState(
    val isShow: Boolean = false,
    val isSessionActive: Boolean = false,

    val title: String = "Inputs",

    val throttle: Float = 0f,
    val brake: Float = 0f,
    val clutch: Float = 0f,
    val steerNorm: Float = 0f,

    val settings: InputHudSettings = InputHudSettings(),

    val series: InputsSeries = InputsSeries(capacity = 120), // ~2s for 60Hz
    val renderTick: Long = 0L,
)
