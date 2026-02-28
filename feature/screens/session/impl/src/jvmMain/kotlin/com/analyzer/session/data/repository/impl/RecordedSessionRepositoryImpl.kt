package com.analyzer.session.data.repository.impl

import com.analyzer.session.data.model.RecordedSessionDetail
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.util.zip.GZIPInputStream

@Inject
@SingleIn(ScreenScope::class)
internal class RecordedSessionRepositoryImpl(
    private val settings: TelemetryAcquisitionSettings,
    private val json: Json,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : RecordedSessionRepository {

    private val logger = logger()
    private val sessionIndex = mutableMapOf<Long, SessionBundleLocation>()

    override suspend fun loadSessions(): List<RecordedSessionSummary> = withContext(ioDispatcher) {
        val root = resolveRoot() ?: return@withContext emptyList()
        rebuildIndex(root)
            .map { it.summary }
            .sortedByDescending { it.startedAtMs }
    }

    override suspend fun loadSessionDetails(sessionId: Long): RecordedSessionDetail? = withContext(ioDispatcher) {
        val bundle = findBundle(sessionId) ?: return@withContext null
        loadBundleDetails(bundle)
    }

    override suspend fun saveSession(sessionId: Long): Boolean = withContext(ioDispatcher) {
        val bundle = findBundle(sessionId) ?: return@withContext false
        if (bundle.summary.isSaved) return@withContext true

        val saved = bundle.locations.all { location ->
            if (location.metadata.isSaved) {
                true
            } else {
                val updatedMetadata = location.metadata.copy(isSaved = true)
                writeMetadata(File(location.dir, META_FILE_NAME), updatedMetadata)
            }
        }
        if (saved) {
            resolveRoot()?.let(::rebuildIndex)
        }
        saved
    }

    override suspend fun deleteSession(sessionId: Long): Boolean = withContext(ioDispatcher) {
        val bundle = findBundle(sessionId) ?: return@withContext false
        val deleted = bundle.locations.all { location ->
            runCatching {
                location.dir.deleteRecursively()
            }.getOrElse { error ->
                logger.error(error) { "failed to delete session dir: ${location.dir.absolutePath}" }
                false
            }
        }

        if (deleted) {
            sessionIndex.remove(sessionId)
            logger.info {
                "deleted session bundle $sessionId (${bundle.locations.size} dirs)"
            }
            resolveRoot()?.let(::rebuildIndex)
        }

        deleted
    }

    private suspend fun findBundle(sessionId: Long): SessionBundleLocation? {
        sessionIndex[sessionId]?.let { return it }
        val root = resolveRoot() ?: return null
        rebuildIndex(root)
        return sessionIndex[sessionId]
    }

    private fun rebuildIndex(root: File): List<SessionBundleLocation> {
        sessionIndex.clear()

        val locations = loadLocations(root)
        val duplicatedRuntimeSessionIds = locations
            .groupBy { it.metadata.sessionId }
            .filterValues { it.size > 1 }

        duplicatedRuntimeSessionIds.forEach { (runtimeSessionId, collisions) ->
            logger.debug {
                "duplicate runtime sessionId=$runtimeSessionId detected (${collisions.size} entries), " +
                    "keeping all entries via stable persisted IDs"
            }
        }

        val bundles = buildSessionBundles(locations)
        bundles.forEach { bundle ->
            val previous = sessionIndex.put(bundle.summary.sessionId, bundle)
            if (previous != null) {
                logger.warn {
                    "stable session bundle id collision for ${bundle.summary.sessionId}; replacing previous bundle"
                }
            }
        }

        return bundles
    }

    private fun loadLocations(root: File): List<SessionLocation> = root.listFiles()
        ?.asSequence()
        ?.filter { it.isDirectory }
        ?.mapNotNull(::readSessionLocation)
        ?.toList()
        .orEmpty()

    private fun readSessionLocation(dir: File): SessionLocation? {
        val metaFile = File(dir, META_FILE_NAME)
        if (!metaFile.exists()) return null

        val metadata = readMetadata(metaFile) ?: return null
        val analysis = readAnalysis(metadata, dir)
        val summary = buildSummary(
            metadata = metadata,
            analysis = analysis,
            persistedSessionId = stablePersistedSessionId(metadata, dir),
        )

        return SessionLocation(
            summary = summary,
            dir = dir,
            metadata = metadata,
            analysis = analysis,
        )
    }

    private fun buildSessionBundles(locations: List<SessionLocation>): List<SessionBundleLocation> {
        if (locations.isEmpty()) return emptyList()

        val sorted = locations.sortedBy { it.summary.startedAtMs }
        val explicitByGroupId = linkedMapOf<String, MutableList<SessionLocation>>()
        val legacyLocations = mutableListOf<SessionLocation>()

        sorted.forEach { location ->
            val groupId = location.metadata.sessionGroupId
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            if (groupId == null) {
                legacyLocations += location
            } else {
                explicitByGroupId.getOrPut(groupId) { mutableListOf() } += location
            }
        }

        val explicitBundles = explicitByGroupId.values.map(::buildSessionBundle)
        val legacyBundles = buildLegacySessionBundles(legacyLocations)
        return mergeSplitWeekendBundles(explicitBundles + legacyBundles)
            .sortedByDescending { it.summary.startedAtMs }
    }

    private fun mergeSplitWeekendBundles(bundles: List<SessionBundleLocation>): List<SessionBundleLocation> {
        if (bundles.isEmpty()) return emptyList()
        val merged = mutableListOf<MutableList<SessionLocation>>()
        bundles.sortedBy { it.summary.startedAtMs }.forEach { bundle ->
            val current = merged.lastOrNull()
            if (current == null || !shouldMergeSplitWeekend(current.last(), bundle)) {
                merged += bundle.locations.toMutableList()
            } else {
                current += bundle.locations
            }
        }
        return merged.map(::buildSessionBundle)
    }

    private fun buildLegacySessionBundles(locations: List<SessionLocation>): List<SessionBundleLocation> {
        if (locations.isEmpty()) return emptyList()
        val grouped = mutableListOf<MutableList<SessionLocation>>()
        locations.sortedBy { it.summary.startedAtMs }.forEach { location ->
            val current = grouped.lastOrNull()
            if (current == null || !canMergeIntoSameBundle(current.last(), location)) {
                grouped += mutableListOf(location)
            } else {
                current += location
            }
        }
        return grouped.map(::buildSessionBundle)
    }

    private fun buildSessionBundle(locations: List<SessionLocation>): SessionBundleLocation {
        val ordered = locations.sortedBy { it.summary.startedAtMs }
        val summary = buildBundleSummary(ordered)
        return SessionBundleLocation(
            summary = summary,
            locations = ordered,
        )
    }

    private fun canMergeIntoSameBundle(previous: SessionLocation, next: SessionLocation): Boolean {
        if (!sameBundleIdentity(previous, next)) return false

        val previousEnd = previous.summary.endedAtMs ?: previous.summary.startedAtMs
        val gapMs = next.summary.startedAtMs - previousEnd
        return gapMs in 0..SESSION_BUNDLE_GAP_MAX_MS
    }

    private fun sameBundleIdentity(a: SessionLocation, b: SessionLocation): Boolean =
        normalizeGameId(a.summary.gameId) == normalizeGameId(b.summary.gameId) &&
            normalizeBundleLabel(a.summary.trackId) == normalizeBundleLabel(b.summary.trackId) &&
            normalizeBundleCarId(a.summary.carId, a.summary.carModel) ==
            normalizeBundleCarId(b.summary.carId, b.summary.carModel)

    private fun buildBundleSummary(locations: List<SessionLocation>): RecordedSessionSummary {
        val first = locations.first()
        val latest = locations.maxByOrNull { it.summary.startedAtMs } ?: first
        val bundleId = stableBundleSessionId(locations)
        val bestLap = locations.asSequence().mapNotNull { it.summary.bestLapTimeMs }.minOrNull()
        val endedAt = locations.asSequence().mapNotNull { it.summary.endedAtMs }.maxOrNull()
        val carName = locations.asSequence()
            .mapNotNull { it.summary.carName?.takeIf { name -> name.isNotBlank() } }
            .lastOrNull()
        val trackName = locations.asSequence()
            .mapNotNull { it.summary.trackName?.takeIf { name -> name.isNotBlank() } }
            .lastOrNull()

        return RecordedSessionSummary(
            sessionId = bundleId,
            startedAtMs = first.summary.startedAtMs,
            endedAtMs = endedAt ?: latest.summary.endedAtMs,
            gameId = latest.summary.gameId,
            sessionType = latest.summary.sessionType,
            carModel = latest.summary.carModel,
            carName = carName,
            carId = latest.summary.carId,
            trackId = latest.summary.trackId,
            trackName = trackName,
            lapCount = locations.sumOf { it.summary.lapCount },
            bestLapTimeMs = bestLap,
            totalIncidents = locations.sumOf { it.summary.totalIncidents },
            distanceKm = locations.sumOf { it.summary.distanceKm },
            isSaved = locations.all { it.summary.isSaved },
            airTempC = latest.summary.airTempC,
            trackTempC = latest.summary.trackTempC,
        )
    }

    private fun loadBundleDetails(bundle: SessionBundleLocation): RecordedSessionDetail {
        val mergedLaps = bundle.locations.flatMap { location ->
            val analysis = location.analysis ?: readAnalysis(location.metadata, location.dir)
            if (shouldSkipPlaceholderLocation(bundle, location, analysis)) {
                return@flatMap emptyList()
            }
            val sessionType = location.metadata.sessionType
            analysis
                ?.laps
                .orEmpty()
                .map { lap -> lap.copy(sessionType = sessionType) }
        }
        return RecordedSessionDetail(
            summary = bundle.summary,
            laps = mergedLaps,
        )
    }

    private fun readAnalysis(metadata: RecordedSessionMetadata, sessionDir: File): IndexAnalysis? {
        val indexFile = resolveIndexFile(metadata, sessionDir) ?: return null
        return analyzeIndex(indexFile)
    }

    private fun readMetadata(metaFile: File): RecordedSessionMetadata? = runCatching {
        json.decodeFromString(RecordedSessionMetadata.serializer(), metaFile.readText())
    }.onFailure { error ->
        logger.warn(error) { "failed to decode metadata: ${metaFile.absolutePath}" }
    }.getOrNull()

    private fun writeMetadata(metaFile: File, metadata: RecordedSessionMetadata): Boolean = runCatching {
        val encoded = json.encodeToString(RecordedSessionMetadata.serializer(), metadata)
        val tmp = File(metaFile.parentFile, metaFile.name + ".tmp")
        tmp.writeText(encoded)
        if (!tmp.renameTo(metaFile)) {
            metaFile.writeText(encoded)
            tmp.delete()
        }
        true
    }.getOrElse { error ->
        logger.error(error) { "failed to write metadata: ${metaFile.absolutePath}" }
        false
    }

    private fun buildSummary(
        metadata: RecordedSessionMetadata,
        analysis: IndexAnalysis?,
        persistedSessionId: Long,
    ): RecordedSessionSummary {
        val laps = analysis?.laps.orEmpty()
        val completedLaps = laps.count { it.complete }
        val bestLapMs = laps
            .asSequence()
            .filter { it.complete && !it.invalid && !it.inPit }
            .mapNotNull { it.totalTimeMs }
            .minOrNull()
        val incidents = laps.count { it.invalid }

        return RecordedSessionSummary(
            sessionId = persistedSessionId,
            startedAtMs = metadata.startedAtMs,
            endedAtMs = metadata.endedAtMs,
            gameId = metadata.gameId,
            sessionType = metadata.sessionType,
            carModel = metadata.carModel,
            carName = metadata.carName,
            carId = metadata.carId,
            trackId = metadata.trackId,
            trackName = metadata.trackName,
            lapCount = completedLaps,
            bestLapTimeMs = bestLapMs,
            totalIncidents = incidents,
            distanceKm = analysis?.distanceKm ?: 0.0,
            isSaved = metadata.isSaved,
            airTempC = metadata.airTempC,
            trackTempC = metadata.trackTempC,
        )
    }

    private fun resolveIndexFile(metadata: RecordedSessionMetadata, dir: File): File? {
        val names = linkedSetOf<String>()
        names += metadata.indexFile

        if (metadata.compression.equals(COMPRESSION_GZIP, ignoreCase = true)) {
            if (!metadata.indexFile.endsWith(".gz", ignoreCase = true)) {
                names += "${metadata.indexFile}.gz"
            }
            names += "$INDEX_FILE_NAME.gz"
        }

        names += INDEX_FILE_NAME
        names += "$INDEX_FILE_NAME.gz"

        return names
            .map { File(dir, it) }
            .firstOrNull { it.exists() && it.isFile }
    }

    private fun analyzeIndex(file: File): IndexAnalysis? {
        val input = openIndexStream(file) ?: return null

        DataInputStream(BufferedInputStream(input)).use { data ->
            val header = readIndexHeader(file, data) ?: return null
            val analyzer = IndexAnalyzer()

            while (true) {
                val record = tryReadRecord(data, header.recordSize) ?: break
                analyzer.consume(record)
            }
            return analyzer.build()
        }
    }

    private fun openIndexStream(file: File) = runCatching {
        val stream = file.inputStream()
        if (file.extension.equals("gz", ignoreCase = true)) {
            GZIPInputStream(stream)
        } else {
            stream
        }
    }.onFailure { error ->
        logger.warn(error) { "failed to open index: ${file.absolutePath}" }
    }.getOrNull()

    private fun readIndexHeader(file: File, data: DataInputStream): ParsedIndexHeader? {
        val magic = runCatching { data.readInt() }.getOrNull() ?: return null
        val version = runCatching { data.readInt() }.getOrNull() ?: return null
        val recordSize = runCatching { data.readInt() }.getOrNull() ?: return null
        runCatching { data.readInt() }.getOrNull() ?: return null

        if (magic != INDEX_MAGIC) {
            logger.warn { "invalid index magic in ${file.absolutePath}: $magic" }
            return null
        }
        if (recordSize < INDEX_RECORD_SIZE) {
            logger.warn { "invalid index recordSize=$recordSize in ${file.absolutePath}" }
            return null
        }

        return ParsedIndexHeader(version = version, recordSize = recordSize)
    }

    private fun tryReadRecord(data: DataInputStream, recordSize: Int): IndexRecord? = try {
        val timestampNs = data.readLong()
        data.readLong()
        data.readUnsignedByte()
        data.skipBytes(3)
        data.readLong()
        data.readInt()
        data.readFloat()
        data.readFloat()
        data.readFloat()
        val speedKmh = data.readFloat()
        data.readFloat()
        val lap = data.readInt()
        val sector = data.readInt()
        val flags = data.readInt()

        val extraBytes = recordSize - INDEX_RECORD_SIZE
        if (extraBytes > 0 && !skipFully(data, extraBytes)) return null

        IndexRecord(
            timestampNs = timestampNs,
            speedKmh = speedKmh.takeIf { it.isFinite() },
            lap = lap,
            sector = sector,
            flags = flags,
        )
    } catch (_: EOFException) {
        null
    }

    private fun skipFully(data: DataInputStream, byteCount: Int): Boolean {
        var remaining = byteCount
        while (remaining > 0) {
            val skipped = data.skipBytes(remaining)
            if (skipped <= 0) return false
            remaining -= skipped
        }
        return true
    }

    private suspend fun resolveRoot(): File? {
        val path = runCatching { settings.currentConfig().storageLocation }
            .getOrNull()
            .orEmpty()
            .trim()
        if (path.isBlank()) return null

        val dir = File(path)
        return dir.takeIf { it.exists() && it.isDirectory }
    }

    private fun stablePersistedSessionId(metadata: RecordedSessionMetadata, dir: File): Long {
        val source = buildString(96) {
            append(normalizeGameId(metadata.gameId))
            append('|')
            append(metadata.startedAtMs)
            append('|')
            append(metadata.sessionId)
            append('|')
            append(dir.absolutePath)
        }
        val hash = fnv1a64(source)
        return (hash and Long.MAX_VALUE).let { if (it == 0L) 1L else it }
    }

    private fun stableBundleSessionId(locations: List<SessionLocation>): Long {
        val source = buildString(locations.size * 64) {
            locations.forEach { location ->
                append(location.summary.sessionId)
                append('|')
                append(location.summary.startedAtMs)
                append('|')
                append(location.dir.absolutePath)
                append('\n')
            }
        }
        val hash = fnv1a64(source)
        return (hash and Long.MAX_VALUE).let { if (it == 0L) 1L else it }
    }

    private fun normalizeGameId(gameId: String?): String = gameId.orEmpty().trim().lowercase()

    private fun normalizeBundleLabel(value: String?): String = value.orEmpty().trim().lowercase()

    private fun normalizeSessionType(value: String?): String = value.orEmpty().trim().uppercase()

    private fun normalizeBundleCarId(carId: Int?, carModel: String?): String = carId
        ?.takeIf { it > 0 }
        ?.toString()
        ?: normalizeBundleLabel(carModel)

    private fun shouldMergeSplitWeekend(
        previous: SessionLocation,
        nextBundle: SessionBundleLocation,
    ): Boolean {
        val next = nextBundle.locations.firstOrNull() ?: return false
        if (!sameBundleIdentity(previous, next)) return false

        val previousEnd = previous.summary.endedAtMs ?: previous.summary.startedAtMs
        val gapMs = next.summary.startedAtMs - previousEnd
        if (gapMs !in 0..SESSION_BUNDLE_GAP_MAX_MS) return false

        val previousType = normalizeSessionType(previous.metadata.sessionType)
        val nextType = normalizeSessionType(next.metadata.sessionType)
        if (previousType.isBlank() || previousType != nextType) return false

        return isBoundaryPlaceholder(next, next.analysis)
    }

    private fun shouldSkipPlaceholderLocation(
        bundle: SessionBundleLocation,
        location: SessionLocation,
        analysis: IndexAnalysis?,
    ): Boolean {
        if (bundle.locations.size <= 1) return false
        return isBoundaryPlaceholder(location, analysis)
    }

    private fun isBoundaryPlaceholder(location: SessionLocation, analysis: IndexAnalysis?): Boolean {
        if (location.metadata.frameCount > DETAIL_PLACEHOLDER_MAX_FRAMES) return false
        val laps = analysis?.laps.orEmpty()
        if (laps.isEmpty()) return true
        return laps.none { it.complete } &&
            laps.all { lap -> lap.lap == 1 && !lap.complete && lap.totalTimeMs == null }
    }

    private fun fnv1a64(value: String): Long {
        var hash = -0x340d631b7bdddcdbL // 1469598103934665603UL as signed long
        value.forEach { ch ->
            hash = hash xor ch.code.toLong()
            hash *= 0x100000001b3L
        }
        return hash
    }

    private data class ParsedIndexHeader(val version: Int, val recordSize: Int)

    private companion object {

        const val META_FILE_NAME = "session.json"
        const val INDEX_FILE_NAME = "index.bin"
        const val COMPRESSION_GZIP = "gzip"
        const val INDEX_MAGIC = 0x53414958
        const val INDEX_RECORD_SIZE = 64
        const val SESSION_BUNDLE_GAP_MAX_MS = 20 * 60 * 1000L
        const val DETAIL_PLACEHOLDER_MAX_FRAMES = 5L
    }
}
