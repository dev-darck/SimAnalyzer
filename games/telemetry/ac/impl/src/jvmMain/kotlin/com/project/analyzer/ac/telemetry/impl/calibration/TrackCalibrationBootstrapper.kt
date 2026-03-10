package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.api.di.IO
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

@Inject
@SingleIn(AppScope::class)
class TrackCalibrationBootstrapper(
    private val json: Json,
    private val repository: TrackCalibrationRepository,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) {

    @OptIn(ExperimentalSerializationApi::class)
    suspend fun ensureBundledCalibrationsInstalled() = withContext(ioDispatcher) {
        loadBundledEntries().forEach { fileName ->
            val calibration = loadBundledCalibration(fileName) ?: return@forEach
            val existing = repository.load(
                trackId = calibration.trackId,
                layoutId = calibration.layoutId,
            )
            if (existing == null) {
                repository.save(
                    calibration = calibration.copy(source = TrackCalibrationSource.GAME),
                    source = TrackCalibrationSource.GAME,
                )
            }
        }
    }

    private fun loadBundledIndex(): List<String> {
        val resourcePath = "/${TrackCalibrationFileNameResolver.DIRECTORY_NAME}/${TrackCalibrationFileNameResolver.INDEX_FILE_NAME}"
        val stream = javaClass.getResourceAsStream(resourcePath) ?: return emptyList()
        return stream.bufferedReader().useLines { lines ->
            lines.map(String::trim)
                .filter(String::isNotEmpty)
                .filterNot { it.startsWith("#") }
                .toList()
        }
    }

    private fun loadDevResourceFileNames(): List<String> {
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
        if (!resourcesDir.isDirectory) return emptyList()
        return resourcesDir.listFiles()
            ?.asSequence()
            ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
            ?.map(File::getName)
            ?.sorted()
            ?.toList()
            ?: emptyList()
    }

    private fun loadBundledEntries(): List<String> {
        val indexedEntries = loadBundledIndex()
        if (indexedEntries.isNotEmpty()) {
            return indexedEntries
        }
        return loadDevResourceFileNames()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private fun loadBundledCalibration(fileName: String): TrackCalibration? {
        val resourcePath = "/${TrackCalibrationFileNameResolver.DIRECTORY_NAME}/$fileName"
        val stream = javaClass.getResourceAsStream(resourcePath) ?: return null
        return runCatching {
            json.decodeFromStream(TrackCalibration.serializer(), stream)
        }.getOrNull()
    }
}
