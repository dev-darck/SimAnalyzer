package com.project.analyzer.telemetry.recording.impl.file.session

import com.project.analyzer.telemetry.recording.impl.file.EVENT_ENDED
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.model.SessionEvent
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger

internal class FileTelemetrySessionCloser(private val sessionIo: FileTelemetrySessionIo) {

    private val logger = logger()

    fun closeSession(session: ActiveSession, endReason: String?): Boolean {
        if (session.isClosed) return false

        logCloseSummary(session, endReason)

        val closedSuccessfully = runCatching {
            val endedAtMs = System.currentTimeMillis()
            session.metadata = session.metadata.copy(
                endedAtMs = endedAtMs,
            )
            session.markMetadataDirty()

            sessionIo.writeEvent(
                session = session,
                event = SessionEvent(
                    type = EVENT_ENDED,
                    atMs = endedAtMs,
                    sessionId = session.metadata.sessionId,
                    reason = endReason,
                ),
            )
            if (sessionIo.persistMetadata(session)) {
                session.markMetadataPersisted()
            }
            sessionIo.flushSessionOutputs(session)
            sessionIo.closeSessionOutputs(session)
        }.onFailure { error ->
            logger.warn(error) { "failed to close session ${session.metadata.sessionId} reason=$endReason" }
            sessionIo.closeSessionOutputs(session)
        }.isSuccess

        session.isClosed = true
        return closedSuccessfully
    }

    fun logSessionStats(session: ActiveSession) {
        val metrics = FileTelemetrySessionMetrics.from(session)
        logger.atDebug(RATE_LIMITED) {
            message = "recording stats id=${session.metadata.sessionId}: " +
                "written=${session.frameCount} " +
                "received=${session.receivedFrames} " +
                "skipped=${session.skippedFrames} " +
                "dropped=${session.droppedFrames} " +
                "dup=${session.duplicateFrames} " +
                "ooo=${session.outOfOrderFrames} " +
                "effectiveHz=${"%.1f".format(metrics.effectiveHz)} " +
                "targetHz=${session.metadata.samplingRateHz} " +
                "sourceHz=${"%.1f".format(metrics.sourceHz)} " +
                "sourceLimited=${metrics.sourceLimited} " +
                "frames=${formatSize(metrics.totalDataBytes)} " +
                "index=${formatSize(metrics.indexBytes)} " +
                "total=${formatSize(metrics.totalBytes)} " +
                "duration=${"%.1f".format(metrics.durationSec)}s"
        }
    }

    private fun logCloseSummary(session: ActiveSession, endReason: String?) {
        val metrics = FileTelemetrySessionMetrics.from(session)
        logger.info {
            "session closing id=${session.metadata.sessionId} reason=$endReason " +
                "written=${session.frameCount} received=${session.receivedFrames} " +
                "skipped=${session.skippedFrames} dropped=${session.droppedFrames} " +
                "dup=${session.duplicateFrames} ooo=${session.outOfOrderFrames} " +
                "frames=${formatSize(metrics.totalDataBytes)} index=${formatSize(metrics.indexBytes)} " +
                "total=${formatSize(metrics.totalBytes)} " +
                "duration=${"%.1f".format(metrics.durationSec)}s " +
                "effectiveHz=${"%.1f".format(metrics.effectiveHz)} " +
                "targetHz=${session.metadata.samplingRateHz} " +
                "sourceHz=${"%.1f".format(metrics.sourceHz)}" +
                if (metrics.sourceLimited) " sourceLimited=true" else ""
        }
    }

    private companion object {
        fun formatSize(bytes: Long): String = when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "%.1fKB".format(bytes / 1024.0)
            else -> "%.2fMB".format(bytes / (1024.0 * 1024.0))
        }
    }
}
