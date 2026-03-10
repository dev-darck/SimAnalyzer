package com.project.analyzer.telemetry.recording.impl.file

import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.leak.api.LeakCanaryRuntime
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.recorder.TelemetryRecorder
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionUpdate
import com.project.analyzer.telemetry.recording.impl.file.command.RecordCommand
import com.project.analyzer.telemetry.recording.impl.file.engine.TelemetryFileCommandProcessorFactory
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.model.SessionCompressionTask
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * File recorder runtime.
 *
 * Accepts recorder commands, serializes writes on a single writer coroutine and runs compression as a
 * separate side-effect worker so file I/O does not block the main command path.
 */
@Inject
@SingleIn(SessionScope::class)
internal class FileTelemetryRecorder(
    private val settings: TelemetryAcquisitionSettings,
    private val commandProcessorFactory: TelemetryFileCommandProcessorFactory,
    private val compressionService: TelemetrySessionCompressionService,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TelemetryRecorder {

    private val logger = logger()
    private val scope = CoroutineScope(SupervisorJob() + ioDispatcher + CoroutineName("FileTelemetryRecorder"))
    private val commandQueue = Channel<RecordCommand>(capacity = DEFAULT_QUEUE_CAPACITY)
    private val compressionQueue = Channel<SessionCompressionTask>(capacity = Channel.UNLIMITED)
    private val isClosed = AtomicBoolean(false)

    private val writerJob = scope.launch(CoroutineName("FileTelemetryWriter")) { processCommands() }
    private val compressionJob = scope.launch(CoroutineName("FileTelemetryCompression")) { processCompressionQueue() }

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
        val commandProcessor = commandProcessorFactory.create(onCompressionReady = ::enqueueCompression)

        try {
            for (command in commandQueue) {
                if (command is RecordCommand.Close) break
                commandProcessor.handle(command)
            }
        } finally {
            commandProcessor.closeAll(endReason = "closed")
            commandQueue.close()
        }
    }

    private suspend fun processCompressionQueue() {
        for (task in compressionQueue) {
            runCatching {
                compressionService.compress(task)
            }.onFailure { error ->
                logger.warn(error) { "compression failed for ${task.metaFile.absolutePath}" }
            }
        }
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
}
