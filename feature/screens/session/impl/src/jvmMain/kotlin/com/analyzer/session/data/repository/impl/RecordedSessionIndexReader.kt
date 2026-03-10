package com.analyzer.session.data.repository.impl

import com.project.analyzer.utils.logger.logger
import java.io.BufferedInputStream
import java.io.DataInputStream
import java.io.EOFException
import java.io.File
import java.util.zip.GZIPInputStream

internal class RecordedSessionIndexReader {

    private val logger = logger()

    fun readAnalysis(metadata: RecordedSessionMetadata, sessionDir: File): IndexAnalysis? {
        val indexFile = resolveIndexFile(metadata, sessionDir) ?: return null
        return analyzeIndex(indexFile)
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
            .map { name -> File(dir, name) }
            .firstOrNull { file -> file.exists() && file.isFile }
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

    private data class ParsedIndexHeader(val version: Int, val recordSize: Int)
}
