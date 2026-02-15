package com.project.analyzer.telemetry.recording.api.acquisition

import kotlinx.coroutines.flow.Flow

public interface TelemetryAcquisitionSettings {

    public fun observeConfig(): Flow<TelemetryAcquisitionConfig>
    public suspend fun currentConfig(): TelemetryAcquisitionConfig
}
