package com.analyzer.session.data.repository

import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_INDEX_FILE_NAME
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionBundle
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionLocation
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import java.io.DataOutputStream
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    @Test
    fun `loadSessionDetails forceRefresh invalidates detail cache even with unchanged dir timestamp`() = runBlocking {
        val root = createTempDirectory("session-details-refresh").toFile()
        writeSessionDir(
            root = root,
            dirName = "qualifying-main",
            metadata = sessionMetadata(
                sessionId = 20L,
                sessionGroupId = "group-refresh",
                sessionType = "QUALIFYING",
                startedAtMs = 1_000L,
                endedAtMs = 300_000L,
                frameCount = 800L,
            ),
            records = listOf(
                record(timestampNs = 1_000_000_000L, lap = 1, sector = 0),
                record(timestampNs = 2_000_000_000L, lap = 1, sector = 1),
                record(timestampNs = 3_000_000_000L, lap = 1, sector = 2),
                record(timestampNs = 4_000_000_000L, lap = 2, sector = 0),
            ),
        )

        val repository = repository(root)
        val sessionId = repository.loadSessionListPage(RecordedSessionListRequest()).items.single().sessionId
        val initial = requireNotNull(
            repository.loadSessionDetailPage(
                sessionId = sessionId,
                request = RecordedSessionDetailRequest(),
                forceRefresh = false,
            ),
        )
        assertEquals(1, initial.laps.count { it.totalTimeMs != null })

        val dir = root.resolve("qualifying-main")
        val originalDirLastModified = dir.lastModified()
        writeIndex(
            dir.resolve("index.bin"),
            listOf(
                record(timestampNs = 1_000_000_000L, lap = 1, sector = 0),
                record(timestampNs = 2_000_000_000L, lap = 1, sector = 1),
                record(timestampNs = 3_000_000_000L, lap = 1, sector = 2),
                record(timestampNs = 4_000_000_000L, lap = 2, sector = 0),
                record(timestampNs = 5_000_000_000L, lap = 2, sector = 1),
                record(timestampNs = 6_000_000_000L, lap = 2, sector = 2),
                record(timestampNs = 7_000_000_000L, lap = 3, sector = 0),
            ),
        )
        assertTrue(dir.setLastModified(originalDirLastModified))

        val refreshed = requireNotNull(
            repository.loadSessionDetailPage(
                sessionId = sessionId,
                request = RecordedSessionDetailRequest(),
                forceRefresh = true,
            ),
        )
        assertEquals(2, refreshed.laps.count { it.totalTimeMs != null })
    }

    @Test
    fun `loadSessions splits explicit group when track identity changes`() = runBlocking {
        val root = createTempDirectory("session-group-track-split").toFile()
        writeSessionDir(
            root = root,
            dirName = "red-bull",
            metadata = sessionMetadata(
                sessionId = 40L,
                sessionGroupId = "group-broken",
                sessionType = "PRACTICE",
                startedAtMs = 1_000L,
                endedAtMs = 120_000L,
                frameCount = 12_000L,
                trackId = "redbull_ring_gp",
                trackName = "Red Bull Ring GP",
                layoutId = "gp",
            ),
        )
        writeSessionDir(
            root = root,
            dirName = "watkins",
            metadata = sessionMetadata(
                sessionId = 41L,
                sessionGroupId = "group-broken",
                sessionType = "PRACTICE",
                startedAtMs = 121_000L,
                endedAtMs = 240_000L,
                frameCount = 11_000L,
                trackId = "watkins_glen_gp",
                trackName = "Watkins Glen GP",
                layoutId = "gp",
            ),
        )

        val repository = repository(root)

        val sessions = repository.loadSessionListPage(RecordedSessionListRequest()).items

        assertEquals(2, sessions.size)
        assertEquals("watkins_glen_gp", sessions[0].trackId)
        assertEquals("redbull_ring_gp", sessions[1].trackId)
    }

    @Test
    fun `loadSummaries reuses cached summary list instance`() = runBlocking {
        val root = createTempDirectory("session-summary-cache").toFile()
        writeSessionDir(
            root = root,
            dirName = "practice",
            metadata = sessionMetadata(
                sessionId = 30L,
                sessionGroupId = "group-summaries",
                sessionType = "PRACTICE",
                startedAtMs = 1_000L,
                endedAtMs = 200_000L,
                frameCount = 10_000L,
            ),
        )

        val store = bundleStore(root)

        val first = store.loadSummaries(forceRefresh = false)
        val second = store.loadSummaries(forceRefresh = false)

        assertTrue(first === second)
    }

    private fun repository(root: java.io.File): RecordedSessionRepositoryImpl = RecordedSessionRepositoryImpl(
        bundleStore = bundleStore(root),
        pageFactory = pageFactory(),
        ioDispatcher = kotlinx.coroutines.Dispatchers.IO,
    )

    private fun bundleStore(root: java.io.File): RecordedSessionBundleStore {
        val storage = TestRecordedTelemetrySessionStorage(
            root = root,
            json = json,
        )
        return RecordedSessionBundleStore(
            storage = storage,
            indexReader = RecordedSessionIndexReader(storage),
        )
    }

    private fun pageFactory(): RecordedSessionPageFactory = RecordedSessionPageFactory(
        listPageFactory = com.analyzer.session.data.repository.page.list.RecordedSessionListPageFactory(),
        detailPageFactory = com.analyzer.session.data.repository.page.detail.RecordedSessionDetailPageFactory(),
    )

    private fun writeSessionDir(
        root: java.io.File,
        dirName: String,
        metadata: RecordedTelemetrySessionMetadata,
        records: List<TestIndexRecord> = emptyList(),
    ) {
        val dir = root.resolve(dirName).apply { mkdirs() }
        dir.resolve("session.json")
            .writeText(json.encodeToString(RecordedTelemetrySessionMetadata.serializer(), metadata))
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
        trackId: String = "donington_park_national",
        trackName: String = "Donington Park National",
        layoutId: String? = null,
    ): RecordedTelemetrySessionMetadata = RecordedTelemetrySessionMetadata(
        sessionId = sessionId,
        gameId = "ac",
        sessionGroupId = sessionGroupId,
        sessionType = sessionType,
        carModel = "ks_bmw_m4_gt3",
        carName = "BMW M4 GT3 Evo",
        carId = 1333049901,
        trackId = trackId,
        trackName = trackName,
        layoutId = layoutId,
        airTempC = 20f,
        trackTempC = 24f,
        startedAtMs = startedAtMs,
        endedAtMs = endedAtMs,
        isSaved = false,
        dataSource = "NATIVE",
        payloadType = "ac_shm_v1",
        payloadSize = 3208,
        frameStorageCodec = "raw",
        frameStoragePayloadType = null,
        frameStoragePayloadSize = null,
        samplingRateHz = 100,
        frameCount = frameCount,
        receivedFrames = frameCount,
        droppedFrames = 0,
        skippedFrames = 0,
        firstTimestampNs = null,
        lastTimestampNs = null,
        fileVersion = 2,
        indexVersion = 1,
        indexRecordSize = 64,
        indexFields = emptyList(),
        framesFile = "frames.bin",
        indexFile = "index.bin",
        eventsFile = "events.jsonl",
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

}

private data class TestIndexRecord(
    val timestampNs: Long,
    val lap: Int,
    val sector: Int,
    val speedKmh: Float,
    val flags: Int,
)

private class TestRecordedTelemetrySessionStorage(
    private val root: File,
    private val json: Json,
) : RecordedTelemetrySessionStorage {

    override suspend fun loadBundles(forceRefresh: Boolean): List<RecordedTelemetrySessionBundle> = loadIndex()

    override suspend fun findBundle(
        sessionId: Long,
        forceRefresh: Boolean,
    ): RecordedTelemetrySessionBundle? = loadIndex().firstOrNull { bundle -> bundle.sessionId == sessionId }

    override suspend fun saveSession(sessionId: Long): Boolean = false

    override suspend fun deleteSession(sessionId: Long): Boolean = false

    override fun resolveFramesFile(location: RecordedTelemetrySessionLocation): File? =
        location.dir.resolve(location.metadata.framesFile).takeIf(File::exists)

    override fun resolveIndexFile(location: RecordedTelemetrySessionLocation): File? {
        val preferred = location.dir.resolve(location.metadata.indexFile)
        if (preferred.exists()) return preferred
        return location.dir.resolve(TELEMETRY_SESSION_INDEX_FILE_NAME).takeIf(File::exists)
    }

    private fun loadIndex(): List<RecordedTelemetrySessionBundle> = buildBundles(readLocations())

    private fun readLocations(): List<RecordedTelemetrySessionLocation> = root.listFiles()
        ?.asSequence()
        ?.filter(File::isDirectory)
        ?.mapNotNull(::readLocation)
        ?.toList()
        .orEmpty()

    private fun readLocation(dir: File): RecordedTelemetrySessionLocation? {
        val metaFile = dir.resolve("session.json")
        if (!metaFile.exists()) return null
        val metadata = json.decodeFromString(RecordedTelemetrySessionMetadata.serializer(), metaFile.readText())
        return RecordedTelemetrySessionLocation(
            persistedSessionId = metadata.sessionId,
            dir = dir,
            metadata = metadata,
        )
    }

    private fun buildBundles(locations: List<RecordedTelemetrySessionLocation>): List<RecordedTelemetrySessionBundle> {
        if (locations.isEmpty()) return emptyList()

        val sorted = locations.sortedBy { it.metadata.startedAtMs }
        val explicitByGroupId = linkedMapOf<String, MutableList<RecordedTelemetrySessionLocation>>()
        val legacyLocations = mutableListOf<RecordedTelemetrySessionLocation>()

        sorted.forEach { location ->
            val groupId = location.metadata.sessionGroupId
                ?.trim()
                ?.takeIf(String::isNotBlank)
            if (groupId == null) {
                legacyLocations += location
            } else {
                explicitByGroupId.getOrPut(groupId) { mutableListOf() } += location
            }
        }

        val explicitBundles = explicitByGroupId.values.flatMap(::buildExplicitBundles)
        val legacyBundles = buildLegacyBundles(legacyLocations)
        return mergeSplitWeekendBundles(explicitBundles + legacyBundles)
            .sortedByDescending { bundle -> bundle.metadata.startedAtMs }
    }

    private fun buildExplicitBundles(
        locations: List<RecordedTelemetrySessionLocation>,
    ): List<RecordedTelemetrySessionBundle> {
        if (locations.isEmpty()) return emptyList()

        val grouped = mutableListOf<MutableList<RecordedTelemetrySessionLocation>>()
        locations.sortedBy { it.metadata.startedAtMs }.forEach { location ->
            val current = grouped.lastOrNull()
            if (current == null || !sameBundleIdentity(current.last(), location)) {
                grouped += mutableListOf(location)
            } else {
                current += location
            }
        }
        return grouped.map(::buildBundle)
    }

    private fun buildLegacyBundles(
        locations: List<RecordedTelemetrySessionLocation>,
    ): List<RecordedTelemetrySessionBundle> {
        if (locations.isEmpty()) return emptyList()

        val grouped = mutableListOf<MutableList<RecordedTelemetrySessionLocation>>()
        locations.sortedBy { it.metadata.startedAtMs }.forEach { location ->
            val current = grouped.lastOrNull()
            if (current == null || !canMergeIntoSameBundle(current.last(), location)) {
                grouped += mutableListOf(location)
            } else {
                current += location
            }
        }
        return grouped.map(::buildBundle)
    }

    private fun mergeSplitWeekendBundles(
        bundles: List<RecordedTelemetrySessionBundle>,
    ): List<RecordedTelemetrySessionBundle> {
        if (bundles.isEmpty()) return emptyList()

        val merged = mutableListOf<MutableList<RecordedTelemetrySessionLocation>>()
        bundles.sortedBy { it.metadata.startedAtMs }.forEach { bundle ->
            val current = merged.lastOrNull()
            if (current == null || !shouldMergeSplitWeekend(current.last(), bundle)) {
                merged += bundle.locations.toMutableList()
            } else {
                current += bundle.locations
            }
        }
        return merged.map(::buildBundle)
    }

    private fun buildBundle(locations: List<RecordedTelemetrySessionLocation>): RecordedTelemetrySessionBundle {
        val ordered = locations.sortedBy { it.metadata.startedAtMs }
        return RecordedTelemetrySessionBundle(
            sessionId = stableBundleSessionId(ordered),
            metadata = ordered.last().metadata,
            locations = ordered,
        )
    }

    private fun canMergeIntoSameBundle(
        previous: RecordedTelemetrySessionLocation,
        next: RecordedTelemetrySessionLocation,
    ): Boolean {
        if (!sameBundleIdentity(previous, next)) return false
        val previousEnd = previous.metadata.endedAtMs ?: previous.metadata.startedAtMs
        val gapMs = next.metadata.startedAtMs - previousEnd
        return gapMs in 0..SESSION_BUNDLE_GAP_MAX_MS
    }

    private fun shouldMergeSplitWeekend(
        previous: RecordedTelemetrySessionLocation,
        nextBundle: RecordedTelemetrySessionBundle,
    ): Boolean {
        val next = nextBundle.locations.firstOrNull() ?: return false
        if (!sameBundleIdentity(previous, next)) return false

        val previousEnd = previous.metadata.endedAtMs ?: previous.metadata.startedAtMs
        val gapMs = next.metadata.startedAtMs - previousEnd
        if (gapMs !in 0..SESSION_BUNDLE_GAP_MAX_MS) return false

        val previousType = normalizeSessionType(previous.metadata.sessionType)
        val nextType = normalizeSessionType(next.metadata.sessionType)
        if (previousType.isBlank() || previousType != nextType) return false

        return next.metadata.frameCount <= DETAIL_PLACEHOLDER_MAX_FRAMES
    }

    private fun sameBundleIdentity(
        first: RecordedTelemetrySessionLocation,
        second: RecordedTelemetrySessionLocation,
    ): Boolean = normalizeGameId(first.metadata.gameId) == normalizeGameId(second.metadata.gameId) &&
        normalizeLabel(first.metadata.trackId) == normalizeLabel(second.metadata.trackId) &&
        normalizeLabel(first.metadata.layoutId) == normalizeLabel(second.metadata.layoutId) &&
        normalizeCarId(first.metadata.carId, first.metadata.carModel) ==
        normalizeCarId(second.metadata.carId, second.metadata.carModel)

    private fun stableBundleSessionId(locations: List<RecordedTelemetrySessionLocation>): Long {
        val source = buildString(locations.size * 64) {
            locations.forEach { location ->
                append(location.persistedSessionId)
                append('|')
                append(location.metadata.startedAtMs)
                append('|')
                append(location.dir.absolutePath)
                append('\n')
            }
        }
        val hash = fnv1a64(source)
        return (hash and Long.MAX_VALUE).let { value -> if (value == 0L) 1L else value }
    }

    private fun normalizeGameId(gameId: String?): String = gameId.orEmpty().trim().lowercase()

    private fun normalizeLabel(value: String?): String = value.orEmpty().trim().lowercase()

    private fun normalizeSessionType(value: String?): String = value.orEmpty().trim().uppercase()

    private fun normalizeCarId(carId: Int?, carModel: String?): String = carId
        ?.takeIf { it > 0 }
        ?.toString()
        ?: normalizeLabel(carModel)

    private fun fnv1a64(value: String): Long {
        var hash = FNV1A_64_OFFSET
        value.forEach { ch ->
            hash = hash xor ch.code.toLong()
            hash *= FNV1A_64_PRIME
        }
        return hash
    }

    private companion object {

        private const val DETAIL_PLACEHOLDER_MAX_FRAMES: Long = 5L
        private const val SESSION_BUNDLE_GAP_MAX_MS: Long = 20 * 60 * 1000L
        private const val FNV1A_64_OFFSET: Long = -0x340d631b7bdddcdbL
        private const val FNV1A_64_PRIME: Long = 0x100000001b3L
    }
}
