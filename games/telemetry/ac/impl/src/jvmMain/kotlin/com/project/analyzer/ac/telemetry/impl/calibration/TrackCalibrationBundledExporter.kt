package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.api.di.IO
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.charset.StandardCharsets

@Inject
class TrackCalibrationBundledExporter(
    private val json: Json,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) {

    suspend fun export(calibration: TrackCalibration): Boolean = withContext(ioDispatcher) {
        val resourcesDir = resolveResourcesDirectory() ?: return@withContext false
        resourcesDir.mkdirs()

        val bundledCalibration = calibration.normalizeForStorage(source = TrackCalibrationSource.GAME)
        val fileName = TrackCalibrationFileNameResolver.resolveFileName(
            trackId = bundledCalibration.trackId,
            layoutId = bundledCalibration.layoutId,
        )
        val targetFile = resourcesDir.resolve(fileName)
        targetFile.writeText(
            text = json.encodeToString(TrackCalibration.serializer(), bundledCalibration),
            charset = StandardCharsets.UTF_8,
        )
        updateIndex(resourcesDir = resourcesDir, fileName = fileName)
        true
    }

    private fun resolveResourcesDirectory(): File? {
        val projectRoot = File(System.getProperty("user.dir"))
        val resourcesDir = projectRoot
            .resolve("games")
            .resolve("telemetry")
            .resolve("ac")
            .resolve("impl")
            .resolve("src")
            .resolve("jvmMain")
            .resolve("resources")
            .resolve(TrackCalibrationFileNameResolver.DIRECTORY_NAME)
        return resourcesDir.takeIf { it.exists() || it.parentFile?.exists() == true }
    }

    private fun updateIndex(resourcesDir: File, fileName: String) {
        val indexFile = resourcesDir.resolve(TrackCalibrationFileNameResolver.INDEX_FILE_NAME)
        val entries = if (indexFile.isFile) {
            indexFile.readLines(StandardCharsets.UTF_8)
                .map(String::trim)
                .filter(String::isNotEmpty)
                .toMutableSet()
        } else {
            linkedSetOf()
        }
        entries += fileName
        indexFile.writeText(
            text = entries.sorted().joinToString(separator = System.lineSeparator()),
            charset = StandardCharsets.UTF_8,
        )
    }
}
