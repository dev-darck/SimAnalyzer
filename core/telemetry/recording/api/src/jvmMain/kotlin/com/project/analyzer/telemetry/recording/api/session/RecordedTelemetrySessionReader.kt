package com.project.analyzer.telemetry.recording.api.session

public interface RecordedTelemetrySessionReader {

    public suspend fun readSession(sessionId: Long, forceRefresh: Boolean = false): DecodedRecordedTelemetrySession?
}
