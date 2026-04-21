package com.project.analyzer.telemetry.recording.impl.reader.registry

import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayload
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayloadDecoder
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayloadDecoderRegistry
import dev.zacsweers.metro.Inject

@Inject
internal class RecordedTelemetryPayloadDecoderRegistryImpl(decoders: Set<RecordedTelemetryPayloadDecoder>) :
    RecordedTelemetryPayloadDecoderRegistry {

    private val decodersByPayloadType = decoders.associateBy(RecordedTelemetryPayloadDecoder::payloadType)

    override fun decode(payloadType: String, payload: ByteArray): RecordedTelemetryPayload? =
        decodersByPayloadType[payloadType]?.decode(payload)
}
