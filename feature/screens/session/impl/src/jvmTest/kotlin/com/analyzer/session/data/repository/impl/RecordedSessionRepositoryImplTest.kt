package com.analyzer.session.data.repository.impl

import com.analyzer.session.data.repository.RecordedSessionDetailRequest
import com.analyzer.session.data.repository.RecordedSessionListRequest
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionConfig
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.DataOutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals

class RecordedSessionRepositoryImplTest {

    private val json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    @Test
    fun `loadSessions merges weekend split by placeholder practice bundle`() = runBlocking {
        val root = createTempDirectory("session-repository").toFile()
        writeSessionDir(
            root = root,
            dirName = "practice",
            metadata = sessionMetadata(
                sessionId = 1L,
                sessionGroupId = "group-practice",
                sessionType = "PRACTICE",
                startedAtMs = 1_000L,
                endedAtMs = 200_000L,
                frameCount = 10_000L,
            ),
        )
        writeSessionDir(
            root = root,
            dirName = "practice-placeholder",
            metadata = sessionMetadata(
                sessionId = 2L,
                sessionGroupId = "group-weekend",
                sessionType = "PRACTICE",
                startedAtMs = 200_100L,
                endedAtMs = 201_000L,
                frameCount = 1L,
            ),
        )
        writeSessionDir(
            root = root,
            dirName = "qualifying",
            metadata = sessionMetadata(
                sessionId = 3L,
                sessionGroupId = "group-weekend",
                sessionType = "QUALIFYING",
                startedAtMs = 201_100L,
                endedAtMs = 420_000L,
                frameCount = 12_000L,
            ),
        )

        val repository = repository(root)

        val sessions = repository.loadSessionListPage(RecordedSessionListRequest()).items

        assertEquals(1, sessions.size)
        assertEquals("QUALIFYING", sessions.single().sessionType)
    }

    @Test
    fun `loadSessionDetails skips placeholder laps from micro session`() = runBlocking {
        val root = createTempDirectory("session-details").toFile()
        writeSessionDir(
            root = root,
            dirName = "qualifying-main",
            metadata = sessionMetadata(
                sessionId = 10L,
                sessionGroupId = "group-weekend",
                sessionType = "QUALIFYING",
                startedAtMs = 1_000L,
                endedAtMs = 300_000L,
                frameCount = 500L,
            ),
            records = listOf(
                record(timestampNs = 1_000_000_000L, lap = 1, sector = 0),
                record(timestampNs = 2_000_000_000L, lap = 1, sector = 1),
                record(timestampNs = 3_000_000_000L, lap = 1, sector = 2),
                record(timestampNs = 4_000_000_000L, lap = 2, sector = 0),
            ),
        )
        writeSessionDir(
            root = root,
            dirName = "qualifying-placeholder",
            metadata = sessionMetadata(
                sessionId = 11L,
                sessionGroupId = "group-weekend",
                sessionType = "QUALIFYING",
                startedAtMs = 300_100L,
                endedAtMs = 300_900L,
                frameCount = 1L,
            ),
            records = listOf(
                record(timestampNs = 5_000_000_000L, lap = 1, sector = 0),
            ),
        )

        val repository = repository(root)
        val sessionId = repository.loadSessionListPage(RecordedSessionListRequest()).items.single().sessionId

        val details = repository.loadSessionDetailPage(sessionId, RecordedSessionDetailRequest())

        requireNotNull(details)
        assertEquals(1, details.laps.count { it.sessionType == "QUALIFYING" && it.lap == 1 })
    }

    private fun repository(root: java.io.File): RecordedSessionRepositoryImpl = RecordedSessionRepositoryImpl(
        settings = object : TelemetryAcquisitionSettings {
            private val config = TelemetryAcquisitionConfig(
                samplingRateHz = 100,
                storageLocation = root.absolutePath,
                recordingEnabled = true,
                maxRecordedLaps = 0,
            )

            override fun observeConfig(): Flow<TelemetryAcquisitionConfig> = flowOf(config)

            override suspend fun currentConfig(): TelemetryAcquisitionConfig = config
        },
        json = json,
        ioDispatcher = kotlinx.coroutines.Dispatchers.IO,
    )

    private fun writeSessionDir(
        root: java.io.File,
        dirName: String,
        metadata: RecordedSessionMetadata,
        records: List<TestIndexRecord> = emptyList(),
    ) {
        val dir = root.resolve(dirName).apply { mkdirs() }
        dir.resolve("session.json").writeText(json.encodeToString(RecordedSessionMetadata.serializer(), metadata))
        if (records.isNotEmpty()) {
            writeIndex(dir.resolve("index.bin"), records)
        }
    }

    private fun sessionMetadata(
        sessionId: Long,
        sessionGroupId: String,
        sessionType: String,
        startedAtMs: Long,
        endedAtMs: Long,
        frameCount: Long,
    ): RecordedSessionMetadata = RecordedSessionMetadata(
        sessionId = sessionId,
        gameId = "ac",
        sessionGroupId = sessionGroupId,
        sessionType = sessionType,
        carModel = "ks_bmw_m4_gt3",
        carName = "BMW M4 GT3 Evo",
        carId = 1333049901,
        trackId = "donington_park_national",
        trackName = "Donington Park National",
        airTempC = 20f,
        trackTempC = 24f,
        startedAtMs = startedAtMs,
        endedAtMs = endedAtMs,
        isSaved = false,
        dataSource = "NATIVE",
        payloadType = "ac_shm_v1",
        payloadSize = 3208,
        samplingRateHz = 100,
        frameCount = frameCount,
        receivedFrames = frameCount,
        droppedFrames = 0,
        firstTimestampNs = null,
        lastTimestampNs = null,
        fileVersion = 1,
        indexVersion = 1,
        indexRecordSize = 64,
        indexFields = emptyList(),
        framesFile = "frames.bin",
        indexFile = "index.bin",
        eventsFile = "events.json",
        compression = null,
    )

    private fun writeIndex(file: java.io.File, records: List<TestIndexRecord>) {
        DataOutputStream(file.outputStream().buffered()).use { output ->
            output.writeInt(0x53414958)
            output.writeInt(1)
            output.writeInt(64)
            output.writeInt(0)
            records.forEachIndexed { index, record ->
                output.writeLong(record.timestampNs)
                output.writeLong(index.toLong())
                output.writeByte(0)
                output.writeByte(0)
                output.writeByte(0)
                output.writeByte(0)
                output.writeLong(0L)
                output.writeInt(0)
                output.writeFloat(0f)
                output.writeFloat(0f)
                output.writeFloat(0f)
                output.writeFloat(record.speedKmh)
                output.writeFloat(0f)
                output.writeInt(record.lap)
                output.writeInt(record.sector)
                output.writeInt(record.flags)
            }
        }
    }

    private fun record(
        timestampNs: Long,
        lap: Int,
        sector: Int,
        speedKmh: Float = 120f,
        flags: Int = 0,
    ): TestIndexRecord = TestIndexRecord(
        timestampNs = timestampNs,
        lap = lap,
        sector = sector,
        speedKmh = speedKmh,
        flags = flags,
    )

    private data class TestIndexRecord(
        val timestampNs: Long,
        val lap: Int,
        val sector: Int,
        val speedKmh: Float,
        val flags: Int,
    )
}
