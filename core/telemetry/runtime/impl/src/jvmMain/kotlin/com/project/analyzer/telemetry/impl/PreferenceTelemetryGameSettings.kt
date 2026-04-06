package com.project.analyzer.telemetry.impl

import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.UserPref
import com.project.analyzer.telemetry.api.contract.TelemetryGameDefaults
import com.project.analyzer.telemetry.api.contract.TelemetryGameSettings
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TelemetryGameSettings>())
class PreferenceTelemetryGameSettings(
    @param:UserPref
    private val preferences: Preference,
) : TelemetryGameSettings {

    override fun observeSelection(): Flow<GameSelection> = preferences.observe(
        TelemetryGameDefaults.KEY_GAME_SELECTION,
        TelemetryGameDefaults.DEFAULT_GAME_SELECTION,
    )
        .map { GameSelection.fromPreference(it) }
        .distinctUntilChanged()

    override suspend fun currentSelection(): GameSelection {
        val raw = preferences.get(
            TelemetryGameDefaults.KEY_GAME_SELECTION,
            TelemetryGameDefaults.DEFAULT_GAME_SELECTION,
        )
        return GameSelection.fromPreference(raw)
    }
}
