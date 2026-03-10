package com.project.analyzer.telemetry.recording.impl.file.session

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndex
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionUpdate
import com.project.analyzer.telemetry.recording.impl.file.EVENT_PAUSED
import com.project.analyzer.telemetry.recording.impl.file.EVENT_RESUMED
import com.project.analyzer.telemetry.recording.impl.file.EVENT_UPDATED
import com.project.analyzer.telemetry.recording.impl.file.codec.FrameStorageCodecFactory
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.model.SessionEvent
import com.project.analyzer.telemetry.recording.impl.file.model.SessionKey
import com.project.analyzer.telemetry.recording.impl.file.pipeline.FileTelemetryFrameWriteAdapter
import com.project.analyzer.telemetry.recording.impl.file.session.pipeline.FrameWritePipeline
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json

/**
 * Session-level file persistence orchestrator.
 *
 * Delegates:
 * - session open/init -> initializer
 * - metadata/events/index/file IO -> sessionIo
 * - close/finalize/log summary -> closer
 * - frame writes -> frameWritePipeline
 */
@Inject
@SingleIn(SessionScope::class)
internal class FileTelemetrySessionStore(
    json: Json,
    frameStorageCodecFactory: FrameStorageCodecFactory,
    frameWriteAdapters: List<FileTelemetryFrameWriteAdapter>,
) : TelemetryFileSessionStore {

    internal val logger = logger()
    private val layout = FileTelemetrySessionLayout()
    private val sessionIo = FileTelemetrySessionIo(json)
    private val initializer = FileTelemetrySessionInitializer(layout, sessionIo, frameStorageCodecFactory)
    private val closer = FileTelemetrySessionCloser(sessionIo)
    private val frameWritePipeline = FrameWritePipeline(this, frameWriteAdapters)

    override fun keyFor(gameId: String, sessionId: Long): SessionKey = SessionKey(normalizeGameId(gameId), sessionId)

    override fun openSession(
        descriptor: TelemetrySessionDescriptor,
        config: TelemetryAcquisitionConfig,
    ): ActiveSession? = initializer.openSession(descriptor, config)

    override fun pauseSession(session: ActiveSession, reason: String?) {
        session.isPaused = true
        logger.debug { "session paused id=${session.metadata.sessionId} reason=$reason" }
        sessionIo.writeEvent(
            session = session,
            event = SessionEvent(
                type = EVENT_PAUSED,
                atMs = System.currentTimeMillis(),
                sessionId = session.metadata.sessionId,
                reason = reason,
            ),
        )
    }

    override fun resumeSession(session: ActiveSession) {
        session.isPaused = false
        logger.debug { "session resumed id=${session.metadata.sessionId}" }
        sessionIo.writeEvent(
            session = session,
            event = SessionEvent(
                type = EVENT_RESUMED,
                atMs = System.currentTimeMillis(),
                sessionId = session.metadata.sessionId,
            ),
        )
    }

    override fun applyUpdate(session: ActiveSession, update: TelemetrySessionUpdate) {
        if (session.metadata.sessionId != update.sessionId) return
        if (normalizeGameId(session.metadata.gameId) != normalizeGameId(update.gameId)) return

        session.metadata = session.metadata.copy(
            sessionType = update.sessionType ?: session.metadata.sessionType,
            carModel = update.carModel ?: session.metadata.carModel,
            carName = update.carName ?: session.metadata.carName,
            carId = update.carId ?: session.metadata.carId,
            trackId = update.trackId ?: session.metadata.trackId,
            trackName = update.trackName ?: session.metadata.trackName,
            layoutId = update.layoutId ?: session.metadata.layoutId,
            airTempC = update.airTempC ?: session.metadata.airTempC,
            trackTempC = update.trackTempC ?: session.metadata.trackTempC,
            dataSource = update.dataSource ?: session.metadata.dataSource,
        )

        persistMetadata(session)
        sessionIo.writeEvent(
            session = session,
            event = SessionEvent(
                type = EVENT_UPDATED,
                atMs = System.currentTimeMillis(),
                sessionId = update.sessionId,
                sessionType = update.sessionType,
                carModel = update.carModel,
                carName = update.carName,
                carId = update.carId,
                trackId = update.trackId,
                trackName = update.trackName,
                layoutId = update.layoutId,
            ),
        )
    }

    /**
     * @return true if write failed and the session was closed successfully and should be compressed now.
     */
    override fun writeFrame(session: ActiveSession, payload: TelemetryFramePayload): Boolean =
        frameWritePipeline.write(session, payload)

    /**
     * @return true if session was closed successfully and can be compressed.
     */
    override fun closeSession(session: ActiveSession, endReason: String?): Boolean =
        closer.closeSession(session, endReason)

    internal fun logSessionStats(session: ActiveSession) {
        closer.logSessionStats(session)
    }

    internal fun updateSemanticPayloadInfo(
        session: ActiveSession,
        semanticPayloadType: String,
        semanticPayloadSize: Int,
    ) {
        val current = session.metadata
        val resolvedType = current.payloadType.ifBlank { semanticPayloadType }
        val resolvedSize = if (current.payloadSize <= 0) semanticPayloadSize else current.payloadSize
        if (resolvedType == current.payloadType && resolvedSize == current.payloadSize) return

        session.metadata = current.copy(
            payloadType = resolvedType,
            payloadSize = resolvedSize,
        )
    }

    internal fun writeHeader(session: ActiveSession, storagePayloadType: String, storagePayloadSize: Int) {
        sessionIo.writeHeader(session, storagePayloadType, storagePayloadSize)
    }

    internal fun writeIndexRecord(
        session: ActiveSession,
        payload: TelemetryFramePayload,
        payloadOffset: Long,
        payloadSize: Int,
        index: TelemetryFrameIndex?,
    ) {
        sessionIo.writeIndexRecord(
            session = session,
            payload = payload,
            payloadOffset = payloadOffset,
            payloadSize = payloadSize,
            index = index,
        )
    }

    internal fun persistMetadata(session: ActiveSession) {
        sessionIo.persistMetadata(session)
    }

    internal fun flushSessionOutputs(session: ActiveSession) {
        sessionIo.flushSessionOutputs(session)
    }

    internal fun normalizeGameId(gameId: String): String = layout.normalizeGameId(gameId)
}
