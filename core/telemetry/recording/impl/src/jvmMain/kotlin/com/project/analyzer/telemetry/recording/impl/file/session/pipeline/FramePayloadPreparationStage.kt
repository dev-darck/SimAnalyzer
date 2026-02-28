package com.project.analyzer.telemetry.recording.impl.file.session.pipeline

import com.project.analyzer.telemetry.recording.impl.file.session.FileTelemetrySessionStore

internal class FramePayloadPreparationStage(private val store: FileTelemetrySessionStore) : FrameWriteStage {

    override val name: String = "prepare-payload"

    override fun process(context: FrameWriteContext): Boolean {
        val session = context.session
        val payload = context.payload

        val semanticPayloadSize = payload.payload.size
        if (semanticPayloadSize <= 0) {
            session.markDropped()
            store.logger.warn { "empty payload dropped for session ${session.metadata.sessionId}" }
            return false
        }

        val semanticPayloadType = payload.payloadType.ifBlank { session.metadata.payloadType }
        if (semanticPayloadType.isBlank()) {
            session.markDropped()
            store.logger.warn { "payload type missing for session ${session.metadata.sessionId}" }
            return false
        }

        store.updateSemanticPayloadInfo(
            session = session,
            semanticPayloadType = semanticPayloadType,
            semanticPayloadSize = semanticPayloadSize,
        )

        val encodedPayload = session.frameStorageCodec.encode(
            semanticPayloadType = semanticPayloadType,
            semanticPayload = payload.payload,
        )
        val storagePayloadType = encodedPayload.storagePayloadType
        val storagePayloadBytes = encodedPayload.storagePayload
        val storagePayloadSize = storagePayloadBytes.size

        if (storagePayloadType.isBlank()) {
            session.markDropped()
            store.logger.warn { "storage payload type missing for session ${session.metadata.sessionId}" }
            return false
        }
        if (storagePayloadSize <= 0) {
            session.markDropped()
            store.logger.warn { "empty storage payload dropped for session ${session.metadata.sessionId}" }
            return false
        }

        if (!session.headerWritten) {
            store.writeHeader(
                session = session,
                storagePayloadType = storagePayloadType,
                storagePayloadSize = storagePayloadSize,
            )
        } else if (payload.payloadType.isNotBlank() && payload.payloadType != session.metadata.payloadType) {
            session.markDropped()
            store.logger.warn { "payload type mismatch for session ${session.metadata.sessionId}" }
            return false
        }

        val expectedStorageType = session.metadata.frameStoragePayloadType
        if (!expectedStorageType.isNullOrBlank() && storagePayloadType != expectedStorageType) {
            session.markDropped()
            store.logger.warn {
                "storage payload type mismatch for session ${session.metadata.sessionId}: " +
                    "expected=$expectedStorageType actual=$storagePayloadType"
            }
            return false
        }

        val expectedStorageSize = session.metadata.frameStoragePayloadSize ?: 0
        if (expectedStorageSize > 0 && storagePayloadSize != expectedStorageSize) {
            session.markDropped()
            store.logger.warn {
                "storage payload size mismatch for session ${session.metadata.sessionId}: " +
                    "expected=$expectedStorageSize actual=$storagePayloadSize"
            }
            return false
        }

        if (session.metadata.payloadSize > 0 &&
            semanticPayloadSize != session.metadata.payloadSize &&
            !session.payloadSizeMismatchLogged
        ) {
            session.payloadSizeMismatchLogged = true
            store.logger.warn {
                "payload size changed for session ${session.metadata.sessionId}: " +
                    "expected=${session.metadata.payloadSize} actual=$semanticPayloadSize"
            }
        }

        context.storagePayloadType = storagePayloadType
        context.storagePayloadBytes = storagePayloadBytes
        context.storagePayloadSize = storagePayloadSize
        return true
    }
}
