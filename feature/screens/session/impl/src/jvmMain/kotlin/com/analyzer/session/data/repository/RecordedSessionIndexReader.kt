package com.analyzer.session.data.repository

import com.analyzer.session.data.analysis.IndexAnalysis
import com.analyzer.session.data.analysis.IndexAnalyzer
import com.analyzer.session.data.analysis.IndexRecord
import com.analyzer.session.data.repository.cache.AnalysisCacheKey
import com.analyzer.session.data.repository.index.ParsedIndexHeader
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_INDEX_MAGIC
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_INDEX_RECORD_SIZE
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionLocation
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import java.util.zip.GZIPInputStream

@Inject
internal class RecordedSessionIndexReader(private val storage: RecordedTelemetrySessionStorage) {

    private val logger = logger()

    fun readAnalysis(location: RecordedTelemetrySessionLocation): IndexAnalysis? {
        val indexFile = storage.resolveIndexFile(location) ?: return null
        val cacheKey = AnalysisCacheKey(
            absolutePath = indexFile.absolutePath,
            lastModified = indexFile.lastModified(),
            sizeBytes = indexFile.length(),
        )
        peekSharedCache(cacheKey)?.let { return it }
        val analysis = analyzeIndex(indexFile) ?: return null
        storeSharedCache(cacheKey, analysis)
        return analysis
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

        if (magic != TELEMETRY_SESSION_INDEX_MAGIC) {
            logger.warn { "invalid index magic in ${file.absolutePath}: $magic" }
            return null
        }
        if (recordSize < TELEMETRY_SESSION_INDEX_RECORD_SIZE) {
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

        val extraBytes = recordSize - TELEMETRY_SESSION_INDEX_RECORD_SIZE
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

    private fun peekSharedCache(key: AnalysisCacheKey): IndexAnalysis? = sharedAnalysisCache.get()[key]

    private fun storeSharedCache(key: AnalysisCacheKey, analysis: IndexAnalysis) {
        while (true) {
            val current = sharedAnalysisCache.get()
            val updated = LinkedHashMap<AnalysisCacheKey, IndexAnalysis>(current.size + 1, 0.75f, true)
            updated.putAll(current)
            updated[key] = analysis
            while (updated.size > MAX_SHARED_ANALYSIS_CACHE_SIZE) {
                val eldestKey = updated.entries.firstOrNull()?.key ?: break
                updated.remove(eldestKey)
            }
            if (sharedAnalysisCache.compareAndSet(current, updated)) {
                return
            }
        }
    }

    private companion object {

        const val MAX_SHARED_ANALYSIS_CACHE_SIZE = 256

        val sharedAnalysisCache = AtomicReference(
            linkedMapOf<AnalysisCacheKey, IndexAnalysis>(),
        )
    }
}
