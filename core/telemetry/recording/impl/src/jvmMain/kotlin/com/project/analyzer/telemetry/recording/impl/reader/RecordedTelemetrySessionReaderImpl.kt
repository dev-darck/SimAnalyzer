package com.project.analyzer.telemetry.recording.impl.reader

import com.project.analyzer.api.di.IO
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_FILE_MAGIC
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_INDEX_MAGIC
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_INDEX_RECORD_SIZE
import com.project.analyzer.telemetry.recording.api.session.DecodedRecordedTelemetryFrame
import com.project.analyzer.telemetry.recording.api.session.DecodedRecordedTelemetrySession
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetryPayloadDecoderRegistry
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionLocation
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionReader
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import com.project.analyzer.telemetry.recording.impl.reader.codec.FrameStoragePayloadReader
import com.project.analyzer.telemetry.recording.impl.reader.mapper.buildSegmentLayout
import com.project.analyzer.telemetry.recording.impl.reader.model.RecordedTelemetryFrameRecord
import com.project.analyzer.telemetry.recording.impl.reader.model.RecordedTelemetryFramesHeader
import com.project.analyzer.telemetry.recording.impl.reader.model.RecordedTelemetryIndexRecord
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.util.zip.GZIPInputStream

@Inject
@SingleIn(AppScope::class)
internal class RecordedTelemetrySessionReaderImpl(
    private val storage: RecordedTelemetrySessionStorage,
    private val decoderRegistry: RecordedTelemetryPayloadDecoderRegistry,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : RecordedTelemetrySessionReader {

    override suspend fun readSession(
        sessionId: Long,
        forceRefresh: Boolean,
    ): DecodedRecordedTelemetrySession? = withContext(ioDispatcher) {
        val bundle = storage.findBundle(
            sessionId = sessionId,
            forceRefresh = forceRefresh,
        ) ?: return@withContext null
        val segmentLayout = buildSegmentLayout(bundle.locations)
        val decodedFrames = mutableListOf<DecodedRecordedTelemetryFrame>()

        bundle.locations.forEach { location ->
            val segmentId = segmentLayout.segmentIdByLocationId[location.persistedSessionId]
                ?: location.persistedSessionId
            decodedFrames += readLocation(
                location = location,
                segmentId = segmentId,
            )
        }

        if (decodedFrames.isEmpty()) return@withContext null

        DecodedRecordedTelemetrySession(
            sessionId = sessionId,
            metadata = bundle.metadata,
            segments = segmentLayout.segments,
            frames = decodedFrames.sortedBy(DecodedRecordedTelemetryFrame::timestampNs),
        )
    }

    private fun readLocation(
        location: RecordedTelemetrySessionLocation,
        segmentId: Long,
    ): List<DecodedRecordedTelemetryFrame> {
        val framesFile = storage.resolveFramesFile(location) ?: return emptyList()
        val indexFile = storage.resolveIndexFile(location) ?: return emptyList()
        val decodedFrames = mutableListOf<DecodedRecordedTelemetryFrame>()

        DataInputStream(BufferedInputStream(openMaybeCompressed(framesFile))).use { framesInput ->
            DataInputStream(BufferedInputStream(openMaybeCompressed(indexFile))).use { indexInput ->
                val framesHeader = readFramesHeader(framesInput) ?: return emptyList()
                val indexRecordSize = readIndexHeader(indexInput) ?: return emptyList()
                val storageDecoder = FrameStoragePayloadReader(framesHeader.storagePayloadType)
                val semanticPayloadType = storageDecoder.semanticPayloadType

                while (true) {
                    val frameRecord = readFrameRecord(framesInput) ?: break
                    val indexRecord = readIndexRecord(indexInput, indexRecordSize) ?: break
                    val semanticPayload = storageDecoder.decode(frameRecord.payload) ?: continue
                    val decodedPayload = decoderRegistry.decode(
                        payloadType = semanticPayloadType,
                        payload = semanticPayload,
                    ) ?: continue

                    decodedFrames += DecodedRecordedTelemetryFrame(
                        segmentId = segmentId,
                        frameId = frameRecord.frameId,
                        timestampNs = frameRecord.timestampNs,
                        lapNumber = normalizeLapNumber(indexRecord.lap),
                        sectorIndex = normalizeSectorIndex(indexRecord.sector),
                        trackPosition = indexRecord.trackPosition,
                        trackX = indexRecord.positionX,
                        trackY = indexRecord.positionY,
                        headingRad = indexRecord.headingRad,
                        payload = decodedPayload,
                        flags = indexRecord.flags,
                    )
                }
            }
        }

        return decodedFrames
    }

    private fun openMaybeCompressed(file: File) =
        if (file.extension.equals("gz", ignoreCase = true)) GZIPInputStream(file.inputStream()) else file.inputStream()

    private fun readFramesHeader(input: DataInputStream): RecordedTelemetryFramesHeader? = runCatching {
        val magic = input.readInt()
        val version = input.readInt()
        if (magic != TELEMETRY_SESSION_FILE_MAGIC || version <= 0) {
            return null
        }

        val typeLength = input.readInt()
        if (typeLength <= 0) return null
        val typeBytes = ByteArray(typeLength)
        input.readFully(typeBytes)

        RecordedTelemetryFramesHeader(
            storagePayloadType = String(typeBytes, Charsets.UTF_8),
            storagePayloadSize = input.readInt(),
        )
    }.getOrNull()

    private fun readIndexHeader(input: DataInputStream): Int? = runCatching {
        val magic = input.readInt()
        if (magic != TELEMETRY_SESSION_INDEX_MAGIC) {
            return null
        }

        input.readInt()
        val recordSize = input.readInt()
        input.readInt()

        if (recordSize < TELEMETRY_SESSION_INDEX_RECORD_SIZE) {
            return null
        }

        recordSize
    }.getOrNull()

    private fun readFrameRecord(input: DataInputStream): RecordedTelemetryFrameRecord? = try {
        val timestampNs = input.readLong()
        val frameId = input.readLong()
        val dataSourceId = input.readUnsignedByte()
        val payloadSize = input.readInt()
        if (payloadSize <= 0) return null

        val payload = ByteArray(payloadSize)
        input.readFully(payload)

        RecordedTelemetryFrameRecord(
            timestampNs = timestampNs,
            frameId = frameId,
            dataSourceId = dataSourceId,
            payload = payload,
        )
    } catch (_: EOFException) {
        null
    }

    private fun readIndexRecord(
        input: DataInputStream,
        recordSize: Int,
    ): RecordedTelemetryIndexRecord? = try {
        val timestampNs = input.readLong()
        val frameId = input.readLong()
        val dataSourceId = input.readUnsignedByte()
        input.skipBytes(3)
        val payloadOffset = input.readLong()
        val payloadSize = input.readInt()
        val positionX = input.readFloat().takeIf(Float::isFinite)
        val positionY = input.readFloat().takeIf(Float::isFinite)
        val headingRad = input.readFloat().takeIf(Float::isFinite)
        val speedKmh = input.readFloat().takeIf(Float::isFinite)
        val trackPosition = input.readFloat().takeIf(Float::isFinite)
        val lap = input.readInt()
        val sector = input.readInt()
        val flags = input.readInt()

        val extraBytes = recordSize - TELEMETRY_SESSION_INDEX_RECORD_SIZE
        if (extraBytes > 0) {
            input.skipBytes(extraBytes)
        }

        RecordedTelemetryIndexRecord(
            timestampNs = timestampNs,
            frameId = frameId,
            dataSourceId = dataSourceId,
            payloadOffset = payloadOffset,
            payloadSize = payloadSize,
            positionX = positionX,
            positionY = positionY,
            headingRad = headingRad,
            speedKmh = speedKmh,
            trackPosition = trackPosition,
            lap = lap,
            sector = sector,
            flags = flags,
        )
    } catch (_: EOFException) {
        null
    }
}

private fun normalizeLapNumber(lapNumber: Int): Int = lapNumber.takeIf { it > 0 } ?: -1

private fun normalizeSectorIndex(sectorIndex: Int): Int = sectorIndex.takeIf { it in 0..2 } ?: -1
