package com.project.analyzer.telemetry.ac.api.contract

import kotlinx.coroutines.flow.StateFlow

public interface TelemetryRecorder {

    public val state: StateFlow<RecorderState>

    public suspend fun start()
    public suspend fun stop()
}

public sealed interface RecorderState {
    public data object Idle : RecorderState
    public data object Recording : RecorderState
    public data class Error(
        val message: String,
        val cause: Throwable? = null
    ) : RecorderState
}
