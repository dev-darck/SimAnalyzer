package com.project.analyzer.telemetry.recording.api.session

public interface RecordedTelemetryPayloadDecoderRegistry {

    public fun decode(payloadType: String, payload: ByteArray): RecordedTelemetryPayload?
}
