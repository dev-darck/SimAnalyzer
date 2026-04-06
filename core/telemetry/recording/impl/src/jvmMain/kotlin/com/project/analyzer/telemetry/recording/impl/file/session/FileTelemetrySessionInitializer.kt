package com.project.analyzer.telemetry.recording.impl.file.session

import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.impl.file.EVENTS_FILE_NAME
import com.project.analyzer.telemetry.recording.impl.file.EVENT_STARTED
import com.project.analyzer.telemetry.recording.impl.file.FILE_VERSION
import com.project.analyzer.telemetry.recording.impl.file.FRAMES_FILE_NAME
import com.project.analyzer.telemetry.recording.impl.file.INDEX_FIELD_NAMES
import com.project.analyzer.telemetry.recording.impl.file.INDEX_FILE_NAME
import com.project.analyzer.telemetry.recording.impl.file.INDEX_RECORD_SIZE
import com.project.analyzer.telemetry.recording.impl.file.INDEX_VERSION
import com.project.analyzer.telemetry.recording.impl.file.codec.FrameStorageCodecFactory
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.model.SessionEvent
import com.project.analyzer.utils.logger.logger

internal class FileTelemetrySessionInitializer(
    private val layout: FileTelemetrySessionLayout,
    private val sessionIo: FileTelemetrySessionIo,
    private val frameStorageCodecFactory: FrameStorageCodecFactory,
) {

    private val logger = logger()

    fun openSession(descriptor: TelemetrySessionDescriptor, config: TelemetryAcquisitionConfig): ActiveSession? {
        if (!config.recordingEnabled) {
            logger.debug { "recording disabled, skipping session ${descriptor.sessionId}" }
            return null
        }

        val root = layout.resolveStorageRoot(config.storageLocation) ?: return null
        val normalizedGameId = layout.normalizeGameId(descriptor.gameId)
        val dir = layout.createSessionDir(root, layout.buildSessionDirName(descriptor))
        val outputs = sessionIo.openOutputs(dir, descriptor.sessionId) ?: return null

        val sessionFrameStorageCodec = frameStorageCodecFactory.create(
            semanticPayloadTypeHint = descriptor.payloadType.takeIf { it.isNotBlank() },
        )
        val metadata = buildMetadata(
            descriptor = descriptor,
            config = config,
            normalizedGameId = normalizedGameId,
            codecId = sessionFrameStorageCodec.codecId,
        )
        val session = createSession(metadata, outputs, sessionFrameStorageCodec)

        return runCatching {
            sessionIo.writeIndexHeader(session)
            initializeHeaderIfDescriptorHasPayload(session, descriptor)
            sessionIo.persistMetadata(session)
            sessionIo.writeEvent(
                session = session,
                event = SessionEvent(
                    type = EVENT_STARTED,
                    atMs = System.currentTimeMillis(),
                    sessionId = descriptor.sessionId,
                    sessionType = descriptor.sessionType,
                    carModel = descriptor.carModel,
                    carName = descriptor.carName,
                    carId = descriptor.carId,
                    trackId = descriptor.trackId,
                    trackName = descriptor.trackName,
                    layoutId = descriptor.layoutId,
                    payloadType = descriptor.payloadType,
                    payloadSize = descriptor.payloadSize,
                ),
            )
            session
        }.onFailure { error ->
            logger.warn(error) { "failed to initialize session ${descriptor.sessionId}" }
            sessionIo.closeSessionOutputs(session)
            dir.deleteRecursively()
        }.getOrNull()
    }

    private fun initializeHeaderIfDescriptorHasPayload(
        session: ActiveSession,
        descriptor: TelemetrySessionDescriptor,
    ) {
        if (descriptor.payloadType.isBlank() || descriptor.payloadSize <= 0) return

        val storageDescriptor = session.frameStorageCodec.describeStorage(
            semanticPayloadType = descriptor.payloadType,
            semanticPayloadSize = descriptor.payloadSize,
        )
        sessionIo.writeHeader(
            session = session,
            storagePayloadType = storageDescriptor.storagePayloadType,
            storagePayloadSize = storageDescriptor.storagePayloadSize,
        )
    }

    private fun createSession(
        metadata: RecordedTelemetrySessionMetadata,
        outputs: FileTelemetrySessionOutputs,
        sessionFrameStorageCodec: com.project.analyzer.telemetry.recording.impl.file.codec.FrameStorageSessionCodec,
    ): ActiveSession = ActiveSession(
        metadata = metadata,
        metaFile = outputs.metaFile,
        frameStorageCodec = sessionFrameStorageCodec,
        dataOut = outputs.dataOut,
        indexOut = outputs.indexOut,
        eventsWriter = outputs.eventsWriter,
    )

    private fun buildMetadata(
        descriptor: TelemetrySessionDescriptor,
        config: TelemetryAcquisitionConfig,
        normalizedGameId: String,
        codecId: String,
    ): RecordedTelemetrySessionMetadata = RecordedTelemetrySessionMetadata(
        sessionId = descriptor.sessionId,
        gameId = normalizedGameId,
        sessionGroupId = descriptor.sessionGroupId,
        sessionType = descriptor.sessionType,
        carModel = descriptor.carModel,
        carName = descriptor.carName,
        carId = descriptor.carId,
        trackId = descriptor.trackId,
        trackName = descriptor.trackName,
        layoutId = descriptor.layoutId,
        airTempC = descriptor.airTempC,
        trackTempC = descriptor.trackTempC,
        startedAtMs = descriptor.startedAtMs,
        endedAtMs = null,
        isSaved = false,
        dataSource = descriptor.dataSource,
        payloadType = descriptor.payloadType,
        payloadSize = descriptor.payloadSize,
        frameStorageCodec = codecId,
        frameStoragePayloadType = null,
        frameStoragePayloadSize = null,
        samplingRateHz = config.samplingRateHz,
        frameCount = 0,
        receivedFrames = 0,
        droppedFrames = 0,
        skippedFrames = 0,
        firstTimestampNs = null,
        lastTimestampNs = null,
        fileVersion = FILE_VERSION,
        indexVersion = INDEX_VERSION,
        indexRecordSize = INDEX_RECORD_SIZE,
        indexFields = INDEX_FIELD_NAMES,
        framesFile = FRAMES_FILE_NAME,
        indexFile = INDEX_FILE_NAME,
        eventsFile = EVENTS_FILE_NAME,
        compression = null,
    )
}
