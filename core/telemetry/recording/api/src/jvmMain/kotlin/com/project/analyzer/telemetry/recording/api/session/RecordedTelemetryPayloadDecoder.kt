package com.project.analyzer.telemetry.recording.api.session

public interface RecordedTelemetryPayloadDecoder {

    public val payloadType: String

    public fun decode(payload: ByteArray): RecordedTelemetryPayload?
}
