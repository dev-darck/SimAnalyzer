package com.project.analyzer.telemetry.api.contract

import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.preference.api.StringPrefKey
import com.project.analyzer.preference.api.str
import kotlinx.coroutines.flow.Flow

public object TelemetryGameDefaults {

    public val KEY_GAME_SELECTION: StringPrefKey = "telemetry_settings_game".str
    public val KEY_GAME_VARIANT: StringPrefKey = "telemetry_settings_game_variant".str
    public const val DEFAULT_GAME_SELECTION: String = GameSelection.AUTO_ID
}

public interface TelemetryGameSettings {

    public fun observeSelection(): Flow<GameSelection>
    public suspend fun currentSelection(): GameSelection
}
