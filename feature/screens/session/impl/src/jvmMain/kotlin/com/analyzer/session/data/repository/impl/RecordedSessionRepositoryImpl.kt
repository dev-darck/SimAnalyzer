package com.analyzer.session.data.repository.impl

import com.analyzer.session.data.model.RecordedSessionDetail
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.repository.RecordedSessionRepository
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.utils.logger
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
class RecordedSessionRepositoryImpl(
    private val settings: TelemetryAcquisitionSettings,
    private val json: Json,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : RecordedSessionRepository {

    private val sessionIndex = mutableMapOf<Long, SessionLocation>()

    override suspend fun loadSessions(): List<RecordedSessionSummary> = withContext(ioDispatcher) {
        val root = resolveRoot() ?: return@withContext emptyList()
        rebuildIndex(root)
            .map { it.summary }
            .sortedByDescending { it.startedAtMs }
    }

    override suspend fun loadSessionDetails(sessionId: Long): RecordedSessionDetail? = withContext(ioDispatcher) {
        val location = findLocation(sessionId) ?: return@withContext null
        val analysis = location.analysis ?: readAnalysis(location.metadata, location.dir)
        RecordedSessionDetail(
            summary = location.summary,
            laps = analysis?.laps.orEmpty(),
        )
    }

    override suspend fun saveSession(sessionId: Long): Boolean = withContext(ioDispatcher) {
        val location = findLocation(sessionId) ?: return@withContext false
        if (location.metadata.isSaved) return@withContext true

        val updatedMetadata = location.metadata.copy(isSaved = true)
        val metaFile = File(location.dir, META_FILE_NAME)
        if (!writeMetadata(metaFile, updatedMetadata)) return@withContext false

        val updatedSummary = location.summary.copy(isSaved = true)
        sessionIndex[sessionId] = location.copy(
            summary = updatedSummary,
            metadata = updatedMetadata,
        )
        true
    }

    override suspend fun deleteSession(sessionId: Long): Boolean = withContext(ioDispatcher) {
        val location = findLocation(sessionId) ?: return@withContext false
        val deleted = runCatching {
            location.dir.deleteRecursively()
        }.getOrElse { error ->
            logger.error(error) { "[sessions] failed to delete session dir: ${location.dir.absolutePath}" }
            false
        }

        if (deleted) {
            sessionIndex.remove(sessionId)
            logger.info { "[sessions] deleted session $sessionId at ${location.dir.absolutePath}" }
        }

        deleted
    }

    private suspend fun findLocation(sessionId: Long): SessionLocation? {
        sessionIndex[sessionId]?.let { return it }
        val root = resolveRoot() ?: return null
        rebuildIndex(root)
        return sessionIndex[sessionId]
    }

    private fun rebuildIndex(root: File): List<SessionLocation> {
        sessionIndex.clear()

        val deduped = loadLocations(root)
            .groupBy { it.summary.sessionId }
            .map { (sessionId, collisions) ->
                val selected = collisions.maxByOrNull { it.summary.startedAtMs }!!
                if (collisions.size > 1) {
                    logger.warn {
                        "[sessions] duplicate sessionId=$sessionId detected (${collisions.size} entries), " +
                            "using latest at ${selected.dir.absolutePath}"
                    }
                }
                selected
            }

        deduped.forEach { location ->
            sessionIndex[location.summary.sessionId] = location
        }

        return deduped
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
        val summary = buildSummary(metadata, analysis)

        return SessionLocation(
            summary = summary,
            dir = dir,
            metadata = metadata,
            analysis = analysis,
        )
    }

    private fun readAnalysis(metadata: RecordedSessionMetadata, sessionDir: File): IndexAnalysis? {
        val indexFile = resolveIndexFile(metadata, sessionDir) ?: return null
        return analyzeIndex(indexFile)
    }

    private fun readMetadata(metaFile: File): RecordedSessionMetadata? = runCatching {
        json.decodeFromString(RecordedSessionMetadata.serializer(), metaFile.readText())
    }.onFailure { error ->
        logger.warn(error) { "[sessions] failed to decode metadata: ${metaFile.absolutePath}" }
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
        logger.error(error) { "[sessions] failed to write metadata: ${metaFile.absolutePath}" }
        false
    }

    private fun buildSummary(metadata: RecordedSessionMetadata, analysis: IndexAnalysis?): RecordedSessionSummary {
        val laps = analysis?.laps.orEmpty()
        val completedLaps = laps.count { it.complete }
        val bestLapMs = laps
            .asSequence()
            .filter { it.complete && !it.invalid }
            .mapNotNull { it.totalTimeMs }
            .minOrNull()
        val incidents = laps.count { it.invalid }

        return RecordedSessionSummary(
            sessionId = metadata.sessionId,
            startedAtMs = metadata.startedAtMs,
            endedAtMs = metadata.endedAtMs,
            gameId = metadata.gameId,
            sessionType = metadata.sessionType,
            carModel = metadata.carModel,
            trackId = metadata.trackId,
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
        logger.warn(error) { "[sessions] failed to open index: ${file.absolutePath}" }
    }.getOrNull()

    private fun readIndexHeader(file: File, data: DataInputStream): ParsedIndexHeader? {
        val magic = runCatching { data.readInt() }.getOrNull() ?: return null
        val version = runCatching { data.readInt() }.getOrNull() ?: return null
        val recordSize = runCatching { data.readInt() }.getOrNull() ?: return null
        runCatching { data.readInt() }.getOrNull() ?: return null

        if (magic != INDEX_MAGIC) {
            logger.warn { "[sessions] invalid index magic in ${file.absolutePath}: $magic" }
            return null
        }
        if (recordSize < INDEX_RECORD_SIZE) {
            logger.warn { "[sessions] invalid index recordSize=$recordSize in ${file.absolutePath}" }
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

    private data class ParsedIndexHeader(val version: Int, val recordSize: Int)

    private companion object {

        const val META_FILE_NAME = "session.json"
        const val INDEX_FILE_NAME = "index.bin"
        const val COMPRESSION_GZIP = "gzip"
        const val INDEX_MAGIC = 0x53414958
        const val INDEX_RECORD_SIZE = 64
    }
}
