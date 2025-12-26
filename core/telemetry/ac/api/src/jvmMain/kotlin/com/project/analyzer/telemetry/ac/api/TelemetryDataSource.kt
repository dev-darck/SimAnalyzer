package com.project.analyzer.telemetry.ac.api

import com.project.analyzer.telemetry.ac.api.model.TelemetryFrame
import kotlinx.coroutines.flow.Flow

public interface TelemetryDataSource {

    public fun frames(): Flow<TelemetryFrame>

    public suspend fun close(): Unit = Unit
}
