package com.project.analyzer.telemetry.recording.impl.file.session

import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndex
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.impl.file.EVENTS_FILE_NAME
import com.project.analyzer.telemetry.recording.impl.file.FILE_MAGIC
import com.project.analyzer.telemetry.recording.impl.file.FILE_VERSION
import com.project.analyzer.telemetry.recording.impl.file.FRAMES_FILE_NAME
import com.project.analyzer.telemetry.recording.impl.file.FRAME_HEADER_FIXED_SIZE
import com.project.analyzer.telemetry.recording.impl.file.INDEX_FILE_NAME
import com.project.analyzer.telemetry.recording.impl.file.INDEX_FLAGS
import com.project.analyzer.telemetry.recording.impl.file.INDEX_MAGIC
import com.project.analyzer.telemetry.recording.impl.file.INDEX_RECORD_SIZE
import com.project.analyzer.telemetry.recording.impl.file.INDEX_VERSION
import com.project.analyzer.telemetry.recording.impl.file.META_FILE_NAME
import com.project.analyzer.telemetry.recording.impl.file.model.ActiveSession
import com.project.analyzer.telemetry.recording.impl.file.model.SessionEvent
import com.project.analyzer.telemetry.recording.impl.file.model.SessionMetadata
import com.project.analyzer.utils.logger.logger
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.BufferedWriter
import java.io.Closeable
import java.io.DataOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStreamWriter

internal class FileTelemetrySessionIo(private val json: Json) {

    private val logger = logger()

    fun openOutputs(dir: File, sessionId: Long): FileTelemetrySessionOutputs? {
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
            logger.warn(error) { "failed to create writers for session $sessionId" }
            dir.deleteRecursively()
            return null
        }

        return FileTelemetrySessionOutputs(
            metaFile = metaFile,
            dataOut = dataOut,
            indexOut = indexOut,
            eventsWriter = eventsWriter,
        )
    }

    fun writeIndexHeader(session: ActiveSession) {
        with(session.indexOut) {
            writeInt(INDEX_MAGIC)
            writeInt(INDEX_VERSION)
            writeInt(INDEX_RECORD_SIZE)
            writeInt(INDEX_FLAGS)
        }
    }

    fun writeHeader(session: ActiveSession, storagePayloadType: String, storagePayloadSize: Int) {
        if (session.headerWritten || storagePayloadSize <= 0) return

        val typeBytes = storagePayloadType.toByteArray(Charsets.UTF_8)
        with(session.dataOut) {
            writeInt(FILE_MAGIC)
            writeInt(FILE_VERSION)
            writeInt(typeBytes.size)
            write(typeBytes)
            writeInt(storagePayloadSize)
        }

        session.framesBytesWritten = (FRAME_HEADER_FIXED_SIZE + typeBytes.size).toLong()
        session.metadata = session.metadata.copy(
            frameStorageCodec = session.frameStorageCodec.codecId,
            frameStoragePayloadType = storagePayloadType,
            frameStoragePayloadSize = storagePayloadSize,
        )
        session.headerWritten = true
    }

    fun writeIndexRecord(
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

    fun writeEvent(session: ActiveSession, event: SessionEvent) {
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

    fun persistMetadata(session: ActiveSession) {
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

    fun writeMetadataFile(metaFile: File, metadata: SessionMetadata) {
        runCatching {
            val encoded = json.encodeToString(SessionMetadata.serializer(), metadata)
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

    fun flushSessionOutputs(session: ActiveSession) {
        session.dataOut.flush()
        session.indexOut.flush()
        session.eventsWriter.flush()
    }

    fun closeSessionOutputs(session: ActiveSession) {
        closeQuietly(session.eventsWriter)
        closeQuietly(session.indexOut)
        closeQuietly(session.dataOut)
    }

    private fun closeQuietly(closeable: Closeable?) {
        runCatching { closeable?.close() }
    }
}
