package com.project.analyzer.inputs.data.repository

import com.project.analyzer.inputs.settings.InputHudSettings
import kotlinx.coroutines.flow.Flow

interface InputHudSettingsRepository {

    val data: Flow<InputHudSettings>
    suspend fun update(inputHudSettings: InputHudSettings)
}
