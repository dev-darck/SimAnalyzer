package com.project.analyzer.telemetry.recording.impl.file.engine

internal fun interface TelemetryFileActiveSessionsFactory {
    fun create(): TelemetryFileActiveSessions
}
