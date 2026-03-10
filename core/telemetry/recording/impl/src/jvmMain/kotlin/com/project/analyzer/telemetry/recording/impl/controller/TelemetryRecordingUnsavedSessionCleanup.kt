package com.project.analyzer.telemetry.recording.impl.controller

import com.project.analyzer.api.di.IO
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.impl.file.META_FILE_NAME
import com.project.analyzer.telemetry.recording.impl.file.model.SessionMetadata
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.File

@Inject
internal class TelemetryRecordingUnsavedSessionCleanup(
    private val settings: TelemetryAcquisitionSettings,
    private val json: Json,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) {
    private val logger = logger()
    private val cleanupDispatcher: CoroutineDispatcher =
        ioDispatcher.limitedParallelism(1, "TelemetryRecordingCleanup")

    suspend fun cleanup(reason: String) {
        withContext(cleanupDispatcher) {
            val root = resolveStorageRoot() ?: return@withContext
            val dirs = root.listFiles()?.filter { it.isDirectory }.orEmpty()
            var removed = 0

            dirs.forEach { dir ->
                val metaFile = File(dir, META_FILE_NAME)
                if (!metaFile.exists()) return@forEach

                val metadata = runCatching {
                    json.decodeFromString(SessionMetadata.serializer(), metaFile.readText())
                }.getOrNull() ?: return@forEach

                if (metadata.isSaved) return@forEach
                if (metadata.endedAtMs != null) return@forEach
                if (dir.deleteRecursively()) {
                    removed += 1
                }
            }

            if (removed > 0) {
                logger.debug { "[recording] removed $removed incomplete temp sessions ($reason)" }
            }
        }
    }

    private suspend fun resolveStorageRoot(): File? {
        val path = runCatching { settings.currentConfig().storageLocation }
            .getOrNull()
            .orEmpty()
            .trim()
        if (path.isBlank()) return null

        val dir = File(path)
        if (!dir.exists() || !dir.isDirectory) return null
        if (!dir.canWrite()) return null
        return dir
    }
}
