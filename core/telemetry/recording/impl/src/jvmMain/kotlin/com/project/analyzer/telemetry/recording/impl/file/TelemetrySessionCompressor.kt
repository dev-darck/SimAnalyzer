package com.project.analyzer.telemetry.recording.impl.file

import com.project.analyzer.api.di.SessionScope
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import com.project.analyzer.telemetry.recording.impl.file.model.SessionCompressionTask
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.Deflater
import java.util.zip.GZIPOutputStream

@Inject
@SingleIn(SessionScope::class)
internal class TelemetrySessionCompressor(private val json: Json) : TelemetrySessionCompressionService {

    override fun compress(task: SessionCompressionTask) {
        val metaFile = task.metaFile
        val dir = metaFile.parentFile ?: return
        val metadata = task.metadata

        val framesName = compressFileIfNeeded(File(dir, metadata.framesFile))
        val indexName = compressFileIfNeeded(File(dir, metadata.indexFile))
        val eventsName = compressFileIfNeeded(File(dir, metadata.eventsFile))

        if (framesName == null && indexName == null && eventsName == null) return

        val updated = metadata.copy(
            framesFile = framesName ?: metadata.framesFile,
            indexFile = indexName ?: metadata.indexFile,
            eventsFile = eventsName ?: metadata.eventsFile,
            compression = COMPRESSION_GZIP,
        )
        writeMetadata(metaFile, updated)
    }

    private fun compressFileIfNeeded(source: File): String? {
        if (!source.exists() || !source.isFile) return null
        if (source.extension.equals("gz", ignoreCase = true)) return source.name

        val target = File(source.parentFile, source.name + ".gz")
        val tmp = File(source.parentFile, source.name + ".gz.tmp")

        return try {
            source.inputStream().use { input ->
                GZIPOutputStream(
                    BufferedOutputStream(FileOutputStream(tmp)),
                    Deflater.BEST_COMPRESSION,
                ).use { output ->
                    input.copyTo(output)
                }
            }

            if (!replaceFile(tmp = tmp, target = target)) {
                tmp.delete()
                return null
            }

            if (!source.delete()) {
                source.deleteOnExit()
            }

            target.name
        } catch (error: Exception) {
            tmp.delete()
            logger.warn(error) { "[recording] failed to compress ${source.absolutePath}" }
            null
        }
    }

    private fun replaceFile(tmp: File, target: File): Boolean {
        if (tmp.renameTo(target)) return true
        if (target.exists() && !target.delete()) return false
        return tmp.renameTo(target)
    }

    private fun writeMetadata(metaFile: File, metadata: RecordedTelemetrySessionMetadata) {
        runCatching {
            val tmp = File(metaFile.parentFile, metaFile.name + ".tmp")
            tmp.writeText(json.encodeToString(RecordedTelemetrySessionMetadata.serializer(), metadata))
            if (!tmp.renameTo(metaFile)) {
                metaFile.writeText(json.encodeToString(RecordedTelemetrySessionMetadata.serializer(), metadata))
                tmp.delete()
            }
        }.onFailure { e ->
            logger.warn(e) { "[recording] failed to write compressed metadata for ${metaFile.absolutePath}" }
        }
    }
}
