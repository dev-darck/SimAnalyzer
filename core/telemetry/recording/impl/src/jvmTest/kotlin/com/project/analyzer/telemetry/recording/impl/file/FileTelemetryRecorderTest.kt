package com.project.analyzer.telemetry.recording.impl.file

import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.api.index.TelemetryFrameIndex
import com.project.analyzer.telemetry.recording.api.payload.TelemetryFramePayload
import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.GZIPInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class FileTelemetryRecorderTest {

    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    @Test
    fun `writes frames and index records`() = runTest {
        val rootDir = Files.createTempDirectory("telemetry-recorder-test").toFile()
        val config = TelemetryAcquisitionConfig(
            samplingRateHz = 50,
            storageLocation = rootDir.absolutePath,
            recordingEnabled = true,
            maxRecordedLaps = 0,
        )
        val settings = TestSettings(config)
        val compressor = TelemetrySessionCompressor(json)
        val recorder = FileTelemetryRecorder(
            settings = settings,
            json = json,
            compressor = compressor,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        val descriptor = TelemetrySessionDescriptor(
            sessionId = 42L,
            gameId = "ac",
            startedAtMs = 1_000L,
            payloadType = PAYLOAD_TYPE,
            payloadSize = PAYLOAD_SIZE,
        )

        recorder.startSession(descriptor)
        val index = TelemetryFrameIndex(
            positionX = TEST_POS_X,
            positionZ = TEST_POS_Z,
            headingRad = TEST_HEADING,
            speedKmh = TEST_SPEED,
            trackPosition = TEST_TRACK_POS,
            lap = TEST_LAP,
            sector = TEST_SECTOR,
            flags = TEST_FLAGS,
        )

        recorder.recordFrame(
            TelemetryFramePayload(
                sessionId = 42L,
                gameId = "ac",
                timestampNs = 10L,
                frameId = 1L,
                payloadType = PAYLOAD_TYPE,
                dataSourceId = DATA_SOURCE_ID,
                payload = byteArrayOf(1, 2, 3, 4),
                index = index,
            )
        )
        recorder.recordFrame(
            TelemetryFramePayload(
                sessionId = 42L,
                gameId = "ac",
                timestampNs = 20L,
                frameId = 2L,
                payloadType = PAYLOAD_TYPE,
                dataSourceId = DATA_SOURCE_ID,
                payload = byteArrayOf(5, 6, 7, 8),
                index = index,
            )
        )
        recorder.endSession("ac", 42L, "done")
        recorder.close()

        val sessionDir = rootDir.listFiles()?.firstOrNull { it.isDirectory }
        assertNotNull(sessionDir)

        val metaFile = File(sessionDir, "session.json")
        assertTrue(metaFile.exists())

        val metadata = json.parseToJsonElement(metaFile.readText()).jsonObject
        val framesName = metadata["framesFile"]?.jsonPrimitive?.content ?: "frames.bin"
        val indexName = metadata["indexFile"]?.jsonPrimitive?.content ?: "index.bin"
        val eventsName = metadata["eventsFile"]?.jsonPrimitive?.content ?: "events.jsonl"
        assertEquals("gzip", metadata["compression"]?.jsonPrimitive?.content)

        val framesFile = File(sessionDir, framesName)
        val indexFile = File(sessionDir, indexName)
        val eventsFile = File(sessionDir, eventsName)

        assertTrue(framesFile.exists())
        assertTrue(indexFile.exists())
        assertTrue(eventsFile.exists())
        assertEquals("gz", framesFile.extension)

        DataInputStream(BufferedInputStream(openMaybeCompressed(framesFile))).use { input ->
            assertEquals(FILE_MAGIC, input.readInt())
            assertEquals(FILE_VERSION, input.readInt())
            val typeLen = input.readInt()
            val type = ByteArray(typeLen)
            input.readFully(type)
            assertEquals(PAYLOAD_TYPE, String(type, Charsets.UTF_8))
            assertEquals(PAYLOAD_SIZE, input.readInt())

            assertEquals(10L, input.readLong())
            assertEquals(1L, input.readLong())
            assertEquals(DATA_SOURCE_ID, input.readUnsignedByte())
            assertEquals(PAYLOAD_SIZE, input.readInt())
            val firstPayload = ByteArray(PAYLOAD_SIZE)
            input.readFully(firstPayload)
            assertTrue(firstPayload.contentEquals(byteArrayOf(1, 2, 3, 4)))
        }

        val typeBytes = PAYLOAD_TYPE.toByteArray(Charsets.UTF_8)
        val headerSize = FRAME_HEADER_FIXED_SIZE + typeBytes.size

        DataInputStream(BufferedInputStream(openMaybeCompressed(indexFile))).use { input ->
            assertEquals(INDEX_MAGIC, input.readInt())
            assertEquals(INDEX_VERSION, input.readInt())
            assertEquals(INDEX_RECORD_SIZE, input.readInt())
            assertEquals(0, input.readInt())

            assertEquals(10L, input.readLong())
            assertEquals(1L, input.readLong())
            assertEquals(DATA_SOURCE_ID, input.readUnsignedByte())
            input.skipBytes(3)
            assertEquals(headerSize + FRAME_RECORD_HEADER_SIZE.toLong(), input.readLong())
            assertEquals(PAYLOAD_SIZE, input.readInt())
            assertEquals(TEST_POS_X, input.readFloat(), 0.0001f)
            assertEquals(TEST_POS_Z, input.readFloat(), 0.0001f)
            assertEquals(TEST_HEADING, input.readFloat(), 0.0001f)
            assertEquals(TEST_SPEED, input.readFloat(), 0.0001f)
            assertEquals(TEST_TRACK_POS, input.readFloat(), 0.0001f)
            assertEquals(TEST_LAP, input.readInt())
            assertEquals(TEST_SECTOR, input.readInt())
            assertEquals(TEST_FLAGS, input.readInt())
        }

        assertEquals(2L, metadata["frameCount"]?.jsonPrimitive?.content?.toLong())
        assertEquals(2L, metadata["receivedFrames"]?.jsonPrimitive?.content?.toLong())
        assertEquals(0L, metadata["droppedFrames"]?.jsonPrimitive?.content?.toLong())

        val eventTypes = openMaybeCompressed(eventsFile).bufferedReader().readLines()
            .map { json.parseToJsonElement(it).jsonObject["type"]?.jsonPrimitive?.content }
        assertTrue(eventTypes.contains("start"))
        assertTrue(eventTypes.contains("end"))
    }

    @Test
    fun `pause drops frames and emits events`() = runTest {
        val rootDir = Files.createTempDirectory("telemetry-recorder-pause").toFile()
        val config = TelemetryAcquisitionConfig(
            samplingRateHz = 50,
            storageLocation = rootDir.absolutePath,
            recordingEnabled = true,
            maxRecordedLaps = 0,
        )
        val settings = TestSettings(config)
        val compressor = TelemetrySessionCompressor(json)

        val recorder = FileTelemetryRecorder(
            settings = settings,
            json = json,
            compressor = compressor,
            ioDispatcher = UnconfinedTestDispatcher(testScheduler),
        )

        val descriptor = TelemetrySessionDescriptor(
            sessionId = 7L,
            gameId = "ac",
            startedAtMs = 2_000L,
            payloadType = PAYLOAD_TYPE,
            payloadSize = PAYLOAD_SIZE,
        )

        recorder.startSession(descriptor)
        recorder.recordFrame(
            TelemetryFramePayload(
                sessionId = 7L,
                gameId = "ac",
                timestampNs = 1L,
                frameId = 1L,
                payloadType = PAYLOAD_TYPE,
                dataSourceId = DATA_SOURCE_ID,
                payload = byteArrayOf(1, 1, 1, 1),
            )
        )
        recorder.pauseSession("ac", 7L, "pit")
        recorder.recordFrame(
            TelemetryFramePayload(
                sessionId = 7L,
                gameId = "ac",
                timestampNs = 2L,
                frameId = 2L,
                payloadType = PAYLOAD_TYPE,
                dataSourceId = DATA_SOURCE_ID,
                payload = byteArrayOf(2, 2, 2, 2),
            )
        )
        recorder.resumeSession("ac", 7L)
        recorder.recordFrame(
            TelemetryFramePayload(
                sessionId = 7L,
                gameId = "ac",
                timestampNs = 3L,
                frameId = 3L,
                payloadType = PAYLOAD_TYPE,
                dataSourceId = DATA_SOURCE_ID,
                payload = byteArrayOf(3, 3, 3, 3),
            )
        )
        recorder.endSession("ac", 7L, null)
        recorder.close()

        val sessionDir = rootDir.listFiles()?.firstOrNull { it.isDirectory }
        assertNotNull(sessionDir)

        val metaFile = File(sessionDir, "session.json")
        val metadata = json.parseToJsonElement(metaFile.readText()).jsonObject
        val indexName = metadata["indexFile"]?.jsonPrimitive?.content ?: "index.bin"
        val eventsName = metadata["eventsFile"]?.jsonPrimitive?.content ?: "events.jsonl"
        val eventsFile = File(sessionDir, eventsName)
        val indexFile = File(sessionDir, indexName)
        assertTrue(eventsFile.exists())
        assertTrue(indexFile.exists())

        assertEquals(2L, metadata["frameCount"]?.jsonPrimitive?.content?.toLong())
        assertEquals(3L, metadata["receivedFrames"]?.jsonPrimitive?.content?.toLong())
        assertEquals(1L, metadata["droppedFrames"]?.jsonPrimitive?.content?.toLong())

        val eventTypes = openMaybeCompressed(eventsFile).bufferedReader().readLines()
            .map { json.parseToJsonElement(it).jsonObject["type"]?.jsonPrimitive?.content }
        assertTrue(eventTypes.contains("pause"))
        assertTrue(eventTypes.contains("resume"))

        DataInputStream(BufferedInputStream(openMaybeCompressed(indexFile))).use { input ->
            assertEquals(INDEX_MAGIC, input.readInt())
            assertEquals(INDEX_VERSION, input.readInt())
        }
    }

    private class TestSettings(
        private val config: TelemetryAcquisitionConfig
    ) : TelemetryAcquisitionSettings {

        override fun observeConfig(): Flow<TelemetryAcquisitionConfig> = flowOf(config)
        override suspend fun currentConfig(): TelemetryAcquisitionConfig = config
    }

    private fun openMaybeCompressed(file: File) =
        if (file.extension == "gz") GZIPInputStream(file.inputStream()) else file.inputStream()

    private companion object {

        private const val FILE_MAGIC = 0x5341544D
        private const val FILE_VERSION = 2
        private const val INDEX_MAGIC = 0x53414958
        private const val INDEX_VERSION = 1
        private const val INDEX_RECORD_SIZE = 64
        private const val FRAME_HEADER_FIXED_SIZE = 16
        private const val FRAME_RECORD_HEADER_SIZE = 21

        private const val PAYLOAD_TYPE = "test_payload"
        private const val PAYLOAD_SIZE = 4
        private const val DATA_SOURCE_ID = 7

        private const val TEST_POS_X = 10.5f
        private const val TEST_POS_Z = -4.25f
        private const val TEST_HEADING = 1.25f
        private const val TEST_SPEED = 123.5f
        private const val TEST_TRACK_POS = 0.42f
        private const val TEST_LAP = 3
        private const val TEST_SECTOR = 2
        private const val TEST_FLAGS = 5
    }
}
