package com.project.analyzer.telemetry.recording.impl.settings

import com.project.analyzer.preference.api.Preference
import com.project.analyzer.preference.api.UserPref
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionDefaults
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.utils.AppDirectories
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TelemetryAcquisitionSettings>())
class PreferenceTelemetryAcquisitionSettings(
    @param:UserPref
    private val preferences: Preference,
    private val appDirectories: AppDirectories,
) : TelemetryAcquisitionSettings {

    override fun observeConfig(): Flow<TelemetryAcquisitionConfig> = combine(
        preferences.observe(
            TelemetryAcquisitionDefaults.KEY_SAMPLING_RATE,
            TelemetryAcquisitionDefaults.DEFAULT_SAMPLING_RATE_HZ,
        ),
        preferences.observe(
            TelemetryAcquisitionDefaults.KEY_STORAGE_LOCATION,
            defaultStorageLocation(),
        ),
        preferences.observe(
            TelemetryAcquisitionDefaults.KEY_RECORDING_ENABLED,
            TelemetryAcquisitionDefaults.DEFAULT_RECORDING_ENABLED,
        ),
        preferences.observe(
            TelemetryAcquisitionDefaults.KEY_MAX_RECORDED_LAPS,
            TelemetryAcquisitionDefaults.DEFAULT_MAX_RECORDED_LAPS,
        ),
    ) { rate, location, enabled, maxLaps ->
        TelemetryAcquisitionConfig(
            samplingRateHz = clampRate(rate),
            storageLocation = location,
            recordingEnabled = enabled,
            maxRecordedLaps = clampMaxLaps(maxLaps),
        )
    }

    override suspend fun currentConfig(): TelemetryAcquisitionConfig {
        val rate = preferences.get(
            TelemetryAcquisitionDefaults.KEY_SAMPLING_RATE,
            TelemetryAcquisitionDefaults.DEFAULT_SAMPLING_RATE_HZ,
        )
        val location = preferences.get(
            TelemetryAcquisitionDefaults.KEY_STORAGE_LOCATION,
            defaultStorageLocation(),
        )
        val recordingEnabled = preferences.get(
            TelemetryAcquisitionDefaults.KEY_RECORDING_ENABLED,
            TelemetryAcquisitionDefaults.DEFAULT_RECORDING_ENABLED,
        )
        val maxRecordedLaps = preferences.get(
            TelemetryAcquisitionDefaults.KEY_MAX_RECORDED_LAPS,
            TelemetryAcquisitionDefaults.DEFAULT_MAX_RECORDED_LAPS,
        )
        return TelemetryAcquisitionConfig(
            samplingRateHz = clampRate(rate),
            storageLocation = location,
            recordingEnabled = recordingEnabled,
            maxRecordedLaps = clampMaxLaps(maxRecordedLaps),
        )
    }

    private fun defaultStorageLocation(): String = appDirectories.cacheDir.absolutePath

    private fun clampRate(hz: Int): Int = hz.coerceIn(
        TelemetryAcquisitionDefaults.MIN_SAMPLING_RATE_HZ,
        TelemetryAcquisitionDefaults.MAX_SAMPLING_RATE_HZ,
    )

    private fun clampMaxLaps(value: Int): Int = value.coerceIn(
        TelemetryAcquisitionDefaults.MIN_MAX_RECORDED_LAPS,
        TelemetryAcquisitionDefaults.MAX_MAX_RECORDED_LAPS,
    )
}
