package com.project.analyzer.inputs.domain.usecase

import com.project.analyzer.inputs.domain.model.InputsResult
import com.project.analyzer.inputs.settings.InputHudSettings
import kotlinx.coroutines.flow.Flow

interface InputsUseCase {

    val settings: Flow<InputHudSettings>
    val results: Flow<InputsResult>

    suspend fun updateSettings(inputHudSettings: InputHudSettings)
}
