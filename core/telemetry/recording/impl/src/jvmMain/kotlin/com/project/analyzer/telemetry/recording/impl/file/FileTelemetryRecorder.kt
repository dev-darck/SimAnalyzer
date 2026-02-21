package com.project.analyzer.telemetry.recording.impl.file

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndex
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.recorder.TelemetryRecorder
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionUpdate
import com.project.analyzer.telemetry.recording.impl.file.command.RecordCommand
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.model.SessionCompressionTask
import com.project.analyzer.telemetry.recording.impl.file.model.SessionEvent
import com.project.analyzer.telemetry.recording.impl.file.model.SessionKey
import com.project.analyzer.telemetry.recording.impl.file.model.SessionMetadata
import com.project.analyzer.utils.logger.RATE_LIMITED
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.Closeable
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter
import java.util.concurrent.atomic.AtomicBoolean

@Inject
@SingleIn(SessionScope::class)
class FileTelemetryRecorder(
    private val settings: TelemetryAcquisitionSettings,
    private val json: Json,
    private val compressor: TelemetrySessionCompressor,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryRecorder {

    private val logger = logger()
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val commandQueue = Channel<RecordCommand>(capacity = DEFAULT_QUEUE_CAPACITY)
    private val compressionQueue = Channel<SessionCompressionTask>(capacity = Channel.UNLIMITED)
    private val isClosed = AtomicBoolean(false)

    private val writerJob = scope.launch { processCommands() }
    private val compressionJob = scope.launch { processCompressionQueue() }

    override suspend fun startSession(descriptor: TelemetrySessionDescriptor) {
        val config = settings.currentConfig()
        sendCommand(RecordCommand.Start(descriptor, config))
    }

    override suspend fun updateSession(update: TelemetrySessionUpdate) {
        sendCommand(RecordCommand.Update(update))
    }

    override suspend fun recordFrame(payload: TelemetryFramePayload) {
        sendCommand(RecordCommand.Frame(payload))
    }

    override suspend fun pauseSession(gameId: String, sessionId: Long, reason: String?) {
        sendCommand(RecordCommand.Pause(gameId, sessionId, reason))
    }

    override suspend fun resumeSession(gameId: String, sessionId: Long) {
        sendCommand(RecordCommand.Resume(gameId, sessionId))
    }

    override suspend fun endSession(gameId: String, sessionId: Long, reason: String?) {
        sendCommand(RecordCommand.End(gameId, sessionId, reason))
    }

    override suspend fun close() {
        if (!isClosed.compareAndSet(false, true)) return

        commandQueue.send(RecordCommand.Close)
        writerJob.join()

        compressionQueue.close()
        compressionJob.join()

        scope.cancel()

        LeakCanaryRuntime.watch(this, "FileTelemetryRecorder")
    }

    private suspend fun sendCommand(command: RecordCommand) {
        if (isClosed.get()) return
        commandQueue.send(command)
    }

    private suspend fun processCommands() {
        val sessions = mutableMapOf<SessionKey, ActiveSession>()

        try {
            for (command in commandQueue) {
                if (command is RecordCommand.Close) {
                    break
                }
                handleCommand(command, sessions)
            }
        } finally {
            sessions.values.forEach { closeSession(it, endReason = "closed") }
            sessions.clear()
            commandQueue.close()
        }
    }

    private suspend fun processCompressionQueue() {
        for (task in compressionQueue) {
            runCatching {
                compressor.compress(task)
            }.onFailure { error ->
                logger.warn(error) { "compression failed for ${task.metaFile.absolutePath}" }
            }
        }
    }

    private fun handleCommand(command: RecordCommand, sessions: MutableMap<SessionKey, ActiveSession>) {
        when (command) {
            is RecordCommand.Start -> handleStart(command, sessions)
            is RecordCommand.Update -> handleUpdate(command, sessions)
            is RecordCommand.Frame -> handleFrame(command, sessions)
            is RecordCommand.Pause -> handlePause(command, sessions)
            is RecordCommand.Resume -> handleResume(command, sessions)
            is RecordCommand.End -> handleEnd(command, sessions)
            RecordCommand.Close -> Unit
        }
    }

    private fun handleStart(command: RecordCommand.Start, sessions: MutableMap<SessionKey, ActiveSession>) {
        val key = sessionKey(command.descriptor.gameId, command.descriptor.sessionId)
        val existing = sessions
            .filterKeys { it.gameId == key.gameId }
            .keys
            .toList()

        existing.forEach { oldKey ->
            sessions.remove(oldKey)?.let { closeSession(it, endReason = "replaced") }
        }

        openSession(command.descriptor, command.config)?.let { opened ->
            sessions[key] = opened
            logger.info {
                "session opened id=${opened.metadata.sessionId} " +
                    "samplingRate=${opened.metadata.samplingRateHz}Hz " +
                    "sampleIntervalNs=${opened.sampleIntervalNs} " +
                    "payloadSize=${opened.metadata.payloadSize}B " +
                    "dir=${opened.metaFile.parentFile?.absolutePath}"
            }
        }
    }

    private fun handleUpdate(command: RecordCommand.Update, sessions: MutableMap<SessionKey, ActiveSession>) {
        val key = sessionKey(command.update.gameId, command.update.sessionId)
        val session = sessions[key] ?: return
        applyUpdate(session, command.update)
    }

    private fun handleFrame(command: RecordCommand.Frame, sessions: MutableMap<SessionKey, ActiveSession>) {
        val key = sessionKey(command.payload.gameId, command.payload.sessionId)
        val session = sessions[key] ?: return
        writeFrame(session, command.payload)

        if (session.isClosed) {
            sessions.remove(key)
        }
    }

    private fun handlePause(command: RecordCommand.Pause, sessions: MutableMap<SessionKey, ActiveSession>) {
        val key = sessionKey(command.gameId, command.sessionId)
        val session = sessions[key] ?: return

        session.isPaused = true
        logger.debug { "session paused id=${command.sessionId} reason=${command.reason}" }
        writeEvent(
            session,
            SessionEvent(
                type = EVENT_PAUSED,
                atMs = System.currentTimeMillis(),
                sessionId = command.sessionId,
                reason = command.reason,
            ),
        )
    }

    private fun handleResume(command: RecordCommand.Resume, sessions: MutableMap<SessionKey, ActiveSession>) {
        val key = sessionKey(command.gameId, command.sessionId)
        val session = sessions[key] ?: return

        session.isPaused = false
        logger.debug { "session resumed id=${command.sessionId}" }
        writeEvent(
            session,
            SessionEvent(
                type = EVENT_RESUMED,
                atMs = System.currentTimeMillis(),
                sessionId = command.sessionId,
            ),
        )
    }

    private fun handleEnd(command: RecordCommand.End, sessions: MutableMap<SessionKey, ActiveSession>) {
        val key = sessionKey(command.gameId, command.sessionId)
        sessions.remove(key)?.let { session ->
            closeSession(session, endReason = command.reason)
        }
    }

    private fun openSession(
        descriptor: TelemetrySessionDescriptor,
        config: TelemetryAcquisitionConfig,
    ): ActiveSession? {
        if (!config.recordingEnabled) {
            logger.debug { "recording disabled, skipping session ${descriptor.sessionId}" }
            return null
        }

        val root = resolveStorageRoot(config.storageLocation) ?: return null
        val normalizedGameId = normalizeGameId(descriptor.gameId)
        val dir = createSessionDir(root, buildSessionDirName(descriptor))

        val framesFile = File(dir, FRAMES_FILE_NAME)
        val indexFile = File(dir, INDEX_FILE_NAME)
        val metaFile = File(dir, META_FILE_NAME)
        val eventsFile = File(dir, EVENTS_FILE_NAME)

        val dataOut: DataOutputStream
        val indexOut: DataOutputStream
        val eventsWriter: BufferedWriter

        try {
            dataOut = DataOutputStream(BufferedOutputStream(FileOutputStream(framesFile)))
            indexOut = DataOutputStream(BufferedOutputStream(FileOutputStream(indexFile)))
            eventsWriter = BufferedWriter(
                OutputStreamWriter(FileOutputStream(eventsFile), Charsets.UTF_8),
            )
        } catch (error: IOException) {
            logger.warn(error) { "failed to create writers for session ${descriptor.sessionId}" }
            dir.deleteRecursively()
            return null
        }

        val metadata = SessionMetadata(
            sessionId = descriptor.sessionId,
            gameId = normalizedGameId,
            sessionType = descriptor.sessionType,
            carModel = descriptor.carModel,
            trackId = descriptor.trackId,
            airTempC = descriptor.airTempC,
            trackTempC = descriptor.trackTempC,
            startedAtMs = descriptor.startedAtMs,
            endedAtMs = null,
            isSaved = false,
            dataSource = descriptor.dataSource,
            payloadType = descriptor.payloadType,
            payloadSize = descriptor.payloadSize,
            samplingRateHz = config.samplingRateHz,
            frameCount = 0,
            receivedFrames = 0,
            droppedFrames = 0,
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

        val session = ActiveSession(
            metadata = metadata,
            metaFile = metaFile,
            dataOut = dataOut,
            indexOut = indexOut,
            eventsWriter = eventsWriter,
        )

        return runCatching {
            writeIndexHeader(session)

            if (descriptor.payloadType.isNotBlank() && descriptor.payloadSize > 0) {
                writeHeader(session, descriptor.payloadType, descriptor.payloadSize)
            }

            persistMetadata(session)
            writeEvent(
                session,
                SessionEvent(
                    type = EVENT_STARTED,
                    atMs = System.currentTimeMillis(),
                    sessionId = descriptor.sessionId,
                    sessionType = descriptor.sessionType,
                    carModel = descriptor.carModel,
                    trackId = descriptor.trackId,
                    payloadType = descriptor.payloadType,
                    payloadSize = descriptor.payloadSize,
                ),
            )

            session
        }.onFailure { error ->
            logger.warn(error) { "failed to initialize session ${descriptor.sessionId}" }
            closeQuietly(session.eventsWriter)
            closeQuietly(session.indexOut)
            closeQuietly(session.dataOut)
            dir.deleteRecursively()
        }.getOrNull()
    }

    private fun writeFrame(session: ActiveSession, payload: TelemetryFramePayload) {
        session.markReceived()

        if (session.metadata.sessionId != payload.sessionId) {
            session.markDropped()
            return
        }
        if (normalizeGameId(session.metadata.gameId) != normalizeGameId(payload.gameId)) {
            session.markDropped()
            return
        }
        if (session.isPaused) {
            session.markDropped()
            return
        }

        val now = payload.timestampNs
        if (!session.shouldSample(now)) {
            session.markSkipped()
            return
        }

        val payloadSize = payload.payload.size
        if (payloadSize <= 0) {
            session.markDropped()
            logger.warn { "empty payload dropped for session ${session.metadata.sessionId}" }
            return
        }

        val payloadType = payload.payloadType.ifBlank { session.metadata.payloadType }
        if (!session.headerWritten) {
            if (payloadType.isBlank()) {
                session.markDropped()
                logger.warn { "payload type missing for session ${session.metadata.sessionId}" }
                return
            }
            writeHeader(session, payloadType, payloadSize)
        } else if (payload.payloadType.isNotBlank() && payload.payloadType != session.metadata.payloadType) {
            session.markDropped()
            logger.warn { "payload type mismatch for session ${session.metadata.sessionId}" }
            return
        }

        if (session.metadata.payloadSize > 0 &&
            payloadSize != session.metadata.payloadSize &&
            !session.payloadSizeMismatchLogged
        ) {
            session.payloadSizeMismatchLogged = true
            logger.warn {
                "payload size changed for session ${session.metadata.sessionId}: " +
                    "expected=${session.metadata.payloadSize} actual=$payloadSize"
            }
        }

        val recordOffset = session.framesBytesWritten
        val payloadOffset = recordOffset + FRAME_RECORD_HEADER_SIZE

        try {
            with(session.dataOut) {
                writeLong(payload.timestampNs)
                writeLong(payload.frameId)
                writeByte(payload.dataSourceId and 0xFF)
                writeInt(payloadSize)
                write(payload.payload)
            }

            session.markWritten(
                timestampNs = payload.timestampNs,
                bytesWritten = FRAME_RECORD_HEADER_SIZE + payloadSize.toLong(),
            )

            writeIndexRecord(
                session = session,
                payload = payload,
                payloadOffset = payloadOffset,
                payloadSize = payloadSize,
                index = payload.index,
            )

            if (session.shouldFlush(System.nanoTime())) {
                flushSessionOutputs(session)
                persistMetadata(session)
                logSessionStats(session)
            }
        } catch (error: IOException) {
            session.markDropped()
            logger.error(error) { "write failed for session ${session.metadata.sessionId}" }
            closeSession(session, endReason = "io_error")
        }
    }

    private fun logSessionStats(session: ActiveSession) {
        val totalDataBytes = session.framesBytesWritten
        val indexBytes = (session.frameCount * INDEX_RECORD_SIZE) + INDEX_HEADER_SIZE
        val totalBytes = totalDataBytes + indexBytes

        val durationSec = session.recordingDurationSec()
        val effectiveHz = if (durationSec > 0) session.frameCount.toDouble() / durationSec else 0.0

        logger.atInfo(RATE_LIMITED) {
            message = "recording stats id=${session.metadata.sessionId}: " +
                "written=${session.frameCount} " +
                "received=${session.receivedFrames} " +
                "skipped=${session.skippedFrames} " +
                "dropped=${session.droppedFrames} " +
                "effectiveHz=${"%.1f".format(effectiveHz)} " +
                "targetHz=${session.metadata.samplingRateHz} " +
                "frames=${formatSize(totalDataBytes)} " +
                "index=${formatSize(indexBytes)} " +
                "total=${formatSize(totalBytes)} " +
                "duration=${"%.1f".format(durationSec)}s"
        }
    }

    private fun applyUpdate(session: ActiveSession, update: TelemetrySessionUpdate) {
        if (session.metadata.sessionId != update.sessionId) return
        if (normalizeGameId(session.metadata.gameId) != normalizeGameId(update.gameId)) return

        session.metadata = session.metadata.copy(
            sessionType = update.sessionType ?: session.metadata.sessionType,
            carModel = update.carModel ?: session.metadata.carModel,
            trackId = update.trackId ?: session.metadata.trackId,
            airTempC = update.airTempC ?: session.metadata.airTempC,
            trackTempC = update.trackTempC ?: session.metadata.trackTempC,
            dataSource = update.dataSource ?: session.metadata.dataSource,
        )

        persistMetadata(session)
        writeEvent(
            session,
            SessionEvent(
                type = EVENT_UPDATED,
                atMs = System.currentTimeMillis(),
                sessionId = update.sessionId,
                sessionType = update.sessionType,
                carModel = update.carModel,
                trackId = update.trackId,
            ),
        )
    }

    private fun closeSession(session: ActiveSession, endReason: String?) {
        if (session.isClosed) return

        val durationSec = session.recordingDurationSec()
        val totalDataBytes = session.framesBytesWritten
        val indexBytes = (session.frameCount * INDEX_RECORD_SIZE) + INDEX_HEADER_SIZE

        logger.info {
            "session closing id=${session.metadata.sessionId} reason=$endReason " +
                "written=${session.frameCount} received=${session.receivedFrames} " +
                "skipped=${session.skippedFrames} dropped=${session.droppedFrames} " +
                "frames=${formatSize(totalDataBytes)} index=${formatSize(indexBytes)} " +
                "total=${formatSize(totalDataBytes + indexBytes)} " +
                "duration=${"%.1f".format(durationSec)}s " +
                "effectiveHz=${"%.1f".format(if (durationSec > 0) session.frameCount.toDouble() / durationSec else 0.0)}"
        }

        val closedSuccessfully = runCatching {
            session.metadata = session.metadata.copy(
                endedAtMs = System.currentTimeMillis(),
                frameCount = session.frameCount,
                receivedFrames = session.receivedFrames,
                droppedFrames = session.droppedFrames,
                skippedFrames = session.skippedFrames,
                firstTimestampNs = session.firstFrameTimestampNs,
                lastTimestampNs = session.lastFrameTimestampNs,
            )

            writeEvent(
                session,
                SessionEvent(
                    type = EVENT_ENDED,
                    atMs = System.currentTimeMillis(),
                    sessionId = session.metadata.sessionId,
                    reason = endReason,
                ),
            )
            writeMetadataFile(session.metaFile, session.metadata)
            flushSessionOutputs(session)
            closeQuietly(session.eventsWriter)
            closeQuietly(session.indexOut)
            closeQuietly(session.dataOut)
        }.onFailure { error ->
            logger.warn(error) {
                "failed to close session ${session.metadata.sessionId} reason=$endReason"
            }
            closeQuietly(session.eventsWriter)
            closeQuietly(session.indexOut)
            closeQuietly(session.dataOut)
        }.isSuccess

        session.isClosed = true
        if (closedSuccessfully) {
            enqueueCompression(session)
        }
    }

    private fun writeHeader(session: ActiveSession, payloadType: String, payloadSize: Int) {
        if (session.headerWritten) return
        if (payloadSize <= 0) return

        val typeBytes = payloadType.toByteArray(Charsets.UTF_8)
        with(session.dataOut) {
            writeInt(FILE_MAGIC)
            writeInt(FILE_VERSION)
            writeInt(typeBytes.size)
            write(typeBytes)
            writeInt(payloadSize)
        }

        session.framesBytesWritten = (FRAME_HEADER_FIXED_SIZE + typeBytes.size).toLong()
        session.metadata = session.metadata.copy(
            payloadType = payloadType,
            payloadSize = payloadSize,
        )
        session.headerWritten = true
    }

    private fun writeIndexHeader(session: ActiveSession) {
        with(session.indexOut) {
            writeInt(INDEX_MAGIC)
            writeInt(INDEX_VERSION)
            writeInt(INDEX_RECORD_SIZE)
            writeInt(INDEX_FLAGS)
        }
    }

    private fun writeIndexRecord(
        session: ActiveSession,
        payload: TelemetryFramePayload,
        payloadOffset: Long,
        payloadSize: Int,
        index: TelemetryFrameIndex?,
    ) {
        with(session.indexOut) {
            writeLong(payload.timestampNs)
            writeLong(payload.frameId)
            writeByte(payload.dataSourceId and 0xFF)
            writeByte(0)
            writeByte(0)
            writeByte(0)
            writeLong(payloadOffset)
            writeInt(payloadSize)

            writeFloat(index?.positionX ?: Float.NaN)
            writeFloat(index?.positionZ ?: Float.NaN)
            writeFloat(index?.headingRad ?: Float.NaN)
            writeFloat(index?.speedKmh ?: Float.NaN)
            writeFloat(index?.trackPosition ?: Float.NaN)
            writeInt(index?.lap ?: -1)
            writeInt(index?.sector ?: -1)
            writeInt(index?.flags ?: 0)
        }
    }

    private fun writeEvent(session: ActiveSession, event: SessionEvent) {
        runCatching {
            with(session.eventsWriter) {
                append(json.encodeToString(event))
                newLine()
                flush()
            }
        }.onFailure { error ->
            logger.warn(error) { "failed to write event for session ${session.metadata.sessionId}" }
        }
    }

    private fun persistMetadata(session: ActiveSession) {
        session.metadata = session.metadata.copy(
            frameCount = session.frameCount,
            receivedFrames = session.receivedFrames,
            droppedFrames = session.droppedFrames,
            skippedFrames = session.skippedFrames,
            firstTimestampNs = session.firstFrameTimestampNs,
            lastTimestampNs = session.lastFrameTimestampNs,
        )
        writeMetadataFile(session.metaFile, session.metadata)
    }

    private fun writeMetadataFile(metaFile: File, metadata: SessionMetadata) {
        runCatching {
            val encoded = json.encodeToString(metadata)
            val tmp = File(metaFile.parentFile, metaFile.name + ".tmp")
            tmp.writeText(encoded)
            if (!tmp.renameTo(metaFile)) {
                metaFile.writeText(encoded)
                tmp.delete()
            }
        }.onFailure { error ->
            logger.warn(error) { "failed to write metadata for session ${metadata.sessionId}" }
        }
    }

    private fun flushSessionOutputs(session: ActiveSession) {
        session.dataOut.flush()
        session.indexOut.flush()
        session.eventsWriter.flush()
    }

    private fun enqueueCompression(session: ActiveSession) {
        val result = compressionQueue.trySend(
            SessionCompressionTask(
                metaFile = session.metaFile,
                metadata = session.metadata,
            ),
        )
        if (result.isFailure) {
            logger.warn { "compression task dropped for session ${session.metadata.sessionId}" }
        }
    }

    private fun resolveStorageRoot(rawPath: String): File? {
        val path = rawPath.trim()
        if (path.isBlank()) {
            logger.warn { "storage location is empty; recording disabled" }
            return null
        }

        val dir = File(path)
        if (!dir.exists() && !dir.mkdirs()) {
            logger.warn { "cannot create storage directory: $path" }
            return null
        }
        if (!dir.isDirectory || !dir.canWrite()) {
            logger.warn { "storage directory not writable: $path" }
            return null
        }

        return dir
    }

    private fun createSessionDir(root: File, baseName: String): File {
        val baseDir = File(root, baseName)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
            return baseDir
        }

        var idx = 1
        while (true) {
            val candidate = File(root, "${baseName}_$idx")
            if (!candidate.exists() && candidate.mkdirs()) {
                return candidate
            }
            idx += 1
        }
    }

    private fun buildSessionDirName(descriptor: TelemetrySessionDescriptor): String {
        val safeGame = normalizeGameId(descriptor.gameId)
            .replace(UNSAFE_DIR_CHARS_REGEX, "_")
            .trim('_')
        return "${safeGame}_${descriptor.startedAtMs}_${descriptor.sessionId}"
    }

    private fun sessionKey(gameId: String, sessionId: Long): SessionKey = SessionKey(normalizeGameId(gameId), sessionId)

    private fun normalizeGameId(gameId: String): String = gameId.trim().lowercase().ifBlank { "unknown" }

    private fun closeQuietly(closeable: Closeable?) {
        runCatching { closeable?.close() }
    }

    private companion object {

        val UNSAFE_DIR_CHARS_REGEX = Regex("[^a-z0-9]+")
        const val INDEX_HEADER_SIZE = 16L

        fun formatSize(bytes: Long): String = when {
            bytes < 1024 -> "${bytes}B"
            bytes < 1024 * 1024 -> "${"%.1f".format(bytes / 1024.0)}KB"
            else -> "${"%.2f".format(bytes / (1024.0 * 1024.0))}MB"
        }
    }
}
