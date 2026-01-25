package com.project.analyzer.inputs.presentation

import com.project.analyzer.inputs.settings.InputHudSettings

internal sealed interface InputsIntent {
    data object Start : InputsIntent
    data class UpdateSettings(val inputHudSettings: InputHudSettings) : InputsIntent
}
