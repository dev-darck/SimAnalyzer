package com.project.analyzer.telemetry.recording.impl.file.session.pipeline

import com.project.analyzer.telemetry.recording.impl.file.model.FrameClass
import com.project.analyzer.telemetry.recording.impl.file.session.FileTelemetrySessionStore
import com.project.analyzer.utils.logger.RATE_LIMITED

internal class FrameAdmissionStage(private val store: FileTelemetrySessionStore) : FrameWriteStage {

    override val name: String = "admission"

    override fun process(context: FrameWriteContext): Boolean {
        val session = context.session
        val payload = context.payload

        session.markReceived(payload.timestampNs)

        if (session.metadata.sessionId != payload.sessionId) {
            session.markDropped()
            return false
        }
        if (store.normalizeGameId(session.metadata.gameId) != store.normalizeGameId(payload.gameId)) {
            session.markDropped()
            return false
        }
        if (session.isPaused) {
            session.markDropped()
            return false
        }

        when (session.classifyFrame(payload.timestampNs, payload.frameId, payload.dataSourceId)) {
            FrameClass.Accepted -> Unit

            FrameClass.Duplicate -> {
                session.markDuplicate()
                store.logger.atWarn(RATE_LIMITED) {
                    message = "duplicate frame dropped for session ${session.metadata.sessionId} " +
                        "ts=${payload.timestampNs} frameId=${payload.frameId} src=${payload.dataSourceId}"
                }
                return false
            }

            FrameClass.OutOfOrder -> {
                session.markOutOfOrder()
                store.logger.atWarn(RATE_LIMITED) {
                    message = "out-of-order frame dropped for session ${session.metadata.sessionId} " +
                        "ts=${payload.timestampNs} lastTs=${session.lastFrameTimestampNs} frameId=${payload.frameId}"
                }
                return false
            }
        }

        if (!session.shouldSample(payload.timestampNs)) {
            session.markSkipped()
            return false
        }

        return true
    }
}
