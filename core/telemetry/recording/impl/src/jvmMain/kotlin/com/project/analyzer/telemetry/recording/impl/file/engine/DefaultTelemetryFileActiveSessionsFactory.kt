package com.project.analyzer.telemetry.recording.impl.file.engine

internal object DefaultTelemetryFileActiveSessionsFactory : TelemetryFileActiveSessionsFactory {
    override fun create(): TelemetryFileActiveSessions = DefaultTelemetryFileActiveSessions()
}
