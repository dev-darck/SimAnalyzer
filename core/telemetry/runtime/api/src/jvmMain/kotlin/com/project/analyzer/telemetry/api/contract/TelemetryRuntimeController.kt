package com.project.analyzer.telemetry.api.contract

public interface TelemetryRuntimeController {

    public suspend fun finishTelemetry()
    public suspend fun launchTelemetry()
}
