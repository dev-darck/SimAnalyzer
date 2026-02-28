package com.project.analyzer.telemetry.recording.impl.file.session

import com.project.analyzer.telemetry.recording.api.session.TelemetrySessionDescriptor
import com.project.analyzer.utils.logger.logger
import java.io.File

internal class FileTelemetrySessionLayout {

    private val logger = logger()

    fun resolveStorageRoot(rawPath: String): File? {
        val path = rawPath.trim()
        if (path.isBlank()) {
            logger.warn { "storage location is empty; recording disabled" }
            return null
        }

        val dir = File(path)
        if (!dir.exists() && !dir.mkdirs()) {
            logger.warn { "cannot create storage directory: $path" }
            return null
        }
        if (!dir.isDirectory || !dir.canWrite()) {
            logger.warn { "storage directory not writable: $path" }
            return null
        }

        return dir
    }

    fun createSessionDir(root: File, baseName: String): File {
        val baseDir = File(root, baseName)
        if (!baseDir.exists()) {
            baseDir.mkdirs()
            return baseDir
        }

        var idx = 1
        while (true) {
            val candidate = File(root, "${baseName}_$idx")
            if (!candidate.exists() && candidate.mkdirs()) {
                return candidate
            }
            idx += 1
        }
    }

    fun buildSessionDirName(descriptor: TelemetrySessionDescriptor): String {
        val safeGame = normalizeGameId(descriptor.gameId)
            .replace(UNSAFE_DIR_CHARS_REGEX, "_")
            .trim('_')
        return "${safeGame}_${descriptor.startedAtMs}_${descriptor.sessionId}"
    }

    fun normalizeGameId(gameId: String): String = gameId.trim().lowercase().ifBlank { "unknown" }

    private companion object {
        val UNSAFE_DIR_CHARS_REGEX = Regex("[^a-z0-9]+")
    }
}
