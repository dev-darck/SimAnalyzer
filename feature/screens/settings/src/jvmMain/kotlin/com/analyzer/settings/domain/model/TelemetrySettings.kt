package com.analyzer.settings.domain.model

import com.project.analyzer.game.api.GameSelection
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionDefaults

data class TelemetrySettings(
    val samplingRateHz: Int = DEFAULT_SAMPLING_RATE_HZ,
    val storageLocation: String = "",
    val recordingEnabled: Boolean = DEFAULT_RECORDING_ENABLED,
    val maxRecordedLaps: Int = DEFAULT_MAX_RECORDED_LAPS,
    val gameSelection: GameSelection = DEFAULT_GAME_SELECTION,
) {

    internal companion object {

        const val MIN_SAMPLING_RATE_HZ = TelemetryAcquisitionDefaults.MIN_SAMPLING_RATE_HZ
        const val MAX_SAMPLING_RATE_HZ = TelemetryAcquisitionDefaults.MAX_SAMPLING_RATE_HZ
        const val DEFAULT_SAMPLING_RATE_HZ = TelemetryAcquisitionDefaults.DEFAULT_SAMPLING_RATE_HZ

        const val DEFAULT_RECORDING_ENABLED = TelemetryAcquisitionDefaults.DEFAULT_RECORDING_ENABLED
        const val MIN_MAX_RECORDED_LAPS = TelemetryAcquisitionDefaults.MIN_MAX_RECORDED_LAPS
        const val MAX_MAX_RECORDED_LAPS = TelemetryAcquisitionDefaults.MAX_MAX_RECORDED_LAPS
        const val DEFAULT_MAX_RECORDED_LAPS = TelemetryAcquisitionDefaults.DEFAULT_MAX_RECORDED_LAPS

        val DEFAULT_GAME_SELECTION: GameSelection = GameSelection.Auto
    }
}
