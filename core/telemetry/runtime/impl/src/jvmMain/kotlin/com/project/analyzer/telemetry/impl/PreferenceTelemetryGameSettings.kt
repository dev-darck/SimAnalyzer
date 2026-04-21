package com.project.analyzer.telemetry.impl

import com.project.analyzer.game.api.GameId
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TelemetryGameSettings>())
class PreferenceTelemetryGameSettings(
    @param:UserPref
    private val preferences: Preference,
) : TelemetryGameSettings {

    override fun observeSelection(): Flow<GameSelection> = combine(
        preferences.observe(
            TelemetryGameDefaults.KEY_GAME_SELECTION,
            TelemetryGameDefaults.DEFAULT_GAME_SELECTION,
        ),
        preferences.observe(TelemetryGameDefaults.KEY_GAME_VARIANT, ""),
    ) { selectionRaw, variantRaw ->
        GameSelection.fromPreference(selectionRaw, GameId.fromName(variantRaw))
    }
        .distinctUntilChanged()

    override suspend fun currentSelection(): GameSelection {
        val raw = preferences.get(
            TelemetryGameDefaults.KEY_GAME_SELECTION,
            TelemetryGameDefaults.DEFAULT_GAME_SELECTION,
        )
        val variantRaw = preferences.get(TelemetryGameDefaults.KEY_GAME_VARIANT, "")
        return GameSelection.fromPreference(raw, GameId.fromName(variantRaw))
    }
}
