package com.project.analyzer.inputs.settings.repository

import com.project.analyzer.hud.api.HudScope
import com.project.analyzer.inputs.settings.InputHudSettings
import com.project.analyzer.inputs.settings.createInputHudSettingsDataStore
import com.project.analyzer.utils.AppDirectories
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow

@Inject
@SingleIn(HudScope::class)
internal class InputsHudSettingsRepositoryImpl(appDirectories: AppDirectories) : InputHudSettingsRepository {

    private val store by lazy { createInputHudSettingsDataStore(directory = appDirectories.preferencesDir) }

    override val data: Flow<InputHudSettings>
        get() = store.data

    override suspend fun update(inputHudSettings: InputHudSettings) {
        store.updateData {
            inputHudSettings
        }
    }
}
