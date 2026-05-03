package com.project.analyzer.telemetry.recording.impl.file.session.pipeline

import com.project.analyzer.telemetry.recording.impl.file.FRAME_RECORD_HEADER_SIZE
import com.project.analyzer.telemetry.recording.impl.file.pipeline.FileTelemetryFrameWriteAdapter
import com.project.analyzer.telemetry.recording.impl.file.session.FileTelemetrySessionStore
import java.io.IOException

internal class FrameEncodeAndPersistStage(
    private val store: FileTelemetrySessionStore,
    private val adapters: List<FileTelemetryFrameWriteAdapter>,
) : FrameWriteStage {

    override val name: String = "write"

    override fun process(context: FrameWriteContext): Boolean {
        val session = context.session
        val payload = context.payload
        val storagePayloadBytes = context.storagePayloadBytes ?: return false
        val storagePayloadSize = context.storagePayloadSize ?: return false
        val recordOffset = session.framesBytesWritten
        val payloadOffset = recordOffset + FRAME_RECORD_HEADER_SIZE

        try {
            with(session.dataOut) {
                writeLong(payload.timestampNs)
                writeLong(payload.frameId)
                writeByte(payload.dataSourceId and 0xFF)
                writeInt(storagePayloadSize)
                write(storagePayloadBytes)
            }

            session.markWritten(
                timestampNs = payload.timestampNs,
                frameId = payload.frameId,
                dataSourceId = payload.dataSourceId,
                bytesWritten = FRAME_RECORD_HEADER_SIZE + storagePayloadSize.toLong(),
            )

            store.writeIndexRecord(
                session = session,
                payload = payload,
                payloadOffset = payloadOffset,
                payloadSize = storagePayloadSize,
                index = payload.index,
            )

            val nowNs = System.nanoTime()
            if (session.shouldFlushOutputs(nowNs)) {
                store.flushSessionOutputs(session)
                store.logSessionStats(session)
            }
            store.persistMetadataIfDue(session, nowNs)
            adapters.forEach { it.onFrameWritten(session, payload) }
            return false
        } catch (error: IOException) {
            session.markDropped()
            store.logger.error(error) { "write failed for session ${session.metadata.sessionId}" }
            adapters.forEach { it.onFrameIoError(session, payload, error) }
            context.shouldEnqueueCompression = store.closeSession(session, endReason = "io_error")
            return false
        }
    }
}
