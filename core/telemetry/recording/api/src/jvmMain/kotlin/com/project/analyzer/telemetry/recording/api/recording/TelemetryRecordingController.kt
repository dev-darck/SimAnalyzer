package com.project.analyzer.telemetry.recording.api.recording

public interface TelemetryRecordingController {

    public suspend fun start()
    public suspend fun stop()
}
