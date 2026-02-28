package com.project.analyzer.telemetry.recording.api.acquisition

import com.project.analyzer.preference.api.BooleanPrefKey
import com.project.analyzer.preference.api.IntPrefKey
import com.project.analyzer.preference.api.StringPrefKey
import com.project.analyzer.preference.api.bool
import com.project.analyzer.preference.api.int
import com.project.analyzer.preference.api.str

public object TelemetryAcquisitionDefaults {

    public const val MIN_SAMPLING_RATE_HZ: Int = 10
    public const val MAX_SAMPLING_RATE_HZ: Int = 100
    public const val DEFAULT_SAMPLING_RATE_HZ: Int = 70

    public val KEY_SAMPLING_RATE: IntPrefKey = "telemetry_settings_rate".int
    public val KEY_STORAGE_LOCATION: StringPrefKey = "telemetry_settings_location".str

    public const val DEFAULT_RECORDING_ENABLED: Boolean = false
    public const val MIN_MAX_RECORDED_LAPS: Int = 0
    public const val MAX_MAX_RECORDED_LAPS: Int = 200
    public const val DEFAULT_MAX_RECORDED_LAPS: Int = 0

    public val KEY_RECORDING_ENABLED: BooleanPrefKey = "telemetry_settings_recording_enabled".bool
    public val KEY_MAX_RECORDED_LAPS: IntPrefKey = "telemetry_settings_max_laps".int
}
