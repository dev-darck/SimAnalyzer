package com.project.analyzer.telemetry.recording.impl.file.engine

import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession

internal fun interface TelemetryFileCommandProcessorFactory {
    fun create(onCompressionReady: (ActiveSession) -> Unit): TelemetryFileCommandProcessor
}
