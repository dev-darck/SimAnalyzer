package com.project.analyzer.telemetry.recording.impl.file.engine

import com.project.analyzer.telemetry.recording.impl.file.command.RecordCommand
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.session.TelemetryFileSessionStore
import com.project.analyzer.utils.logger.logger

internal class DefaultTelemetryFileCommandProcessor(
    private val sessionStore: TelemetryFileSessionStore,
    private val sessions: TelemetryFileActiveSessions,
    private val onCompressionReady: (ActiveSession) -> Unit,
) : TelemetryFileCommandProcessor {

    private val logger = logger()

    override fun handle(command: RecordCommand) {
        when (command) {
            is RecordCommand.Start -> handleStart(command)
            is RecordCommand.Update -> handleUpdate(command)
            is RecordCommand.Frame -> handleFrame(command)
            is RecordCommand.Pause -> handlePause(command)
            is RecordCommand.Resume -> handleResume(command)
            is RecordCommand.End -> handleEnd(command)
            RecordCommand.Close -> Unit
        }
    }

    override fun closeAll(endReason: String) {
        sessions.values().forEach { session ->
            if (sessionStore.closeSession(session, endReason = endReason)) {
                onCompressionReady(session)
            }
        }
        sessions.clear()
    }

    private fun handleStart(command: RecordCommand.Start) {
        val key = sessionStore.keyFor(command.descriptor.gameId, command.descriptor.sessionId)
        sessions.keysForGame(key.gameId).forEach { oldKey ->
            val oldSession = sessions.remove(oldKey) ?: return@forEach
            if (sessionStore.closeSession(oldSession, endReason = "replaced")) {
                onCompressionReady(oldSession)
            }
        }

        val opened = sessionStore.openSession(command.descriptor, command.config) ?: return
        sessions.put(key, opened)
        logger.info {
            "session opened id=${opened.metadata.sessionId} " +
                "samplingRate=${opened.metadata.samplingRateHz}Hz " +
                "sampleIntervalNs=${opened.sampleIntervalNs} " +
                "storageCodec=${opened.metadata.frameStorageCodec} " +
                "storagePayloadType=${opened.metadata.frameStoragePayloadType ?: "-"} " +
                "storagePayloadSize=${opened.metadata.frameStoragePayloadSize ?: -1}B " +
                "payloadSize=${opened.metadata.payloadSize}B " +
                "dir=${opened.metaFile.parentFile?.absolutePath}"
        }
    }

    private fun handleUpdate(command: RecordCommand.Update) {
        val key = sessionStore.keyFor(command.update.gameId, command.update.sessionId)
        val session = sessions.get(key) ?: return
        sessionStore.applyUpdate(session, command.update)
    }

    private fun handleFrame(command: RecordCommand.Frame) {
        val key = sessionStore.keyFor(command.payload.gameId, command.payload.sessionId)
        val session = sessions.get(key) ?: return
        val shouldEnqueueCompression = sessionStore.writeFrame(session, command.payload)
        if (!session.isClosed) return

        sessions.remove(key)
        if (shouldEnqueueCompression) {
            onCompressionReady(session)
        }
    }

    private fun handlePause(command: RecordCommand.Pause) {
        val key = sessionStore.keyFor(command.gameId, command.sessionId)
        val session = sessions.get(key) ?: return
        sessionStore.pauseSession(session, command.reason)
    }

    private fun handleResume(command: RecordCommand.Resume) {
        val key = sessionStore.keyFor(command.gameId, command.sessionId)
        val session = sessions.get(key) ?: return
        sessionStore.resumeSession(session)
    }

    private fun handleEnd(command: RecordCommand.End) {
        val key = sessionStore.keyFor(command.gameId, command.sessionId)
        val session = sessions.remove(key) ?: return
        if (sessionStore.closeSession(session, endReason = command.reason)) {
            onCompressionReady(session)
        }
    }
}
