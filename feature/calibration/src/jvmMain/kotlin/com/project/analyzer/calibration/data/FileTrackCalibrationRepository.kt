package com.project.analyzer.calibration.data

import com.project.analyzer.api.di.IO
import com.project.analyzer.calibration.data.model.TrackCalibration
import com.project.analyzer.calibration.domain.TrackCalibrationRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readText
import kotlin.io.path.writeText

@Inject
@SingleIn(AppScope::class)
class FileTrackCalibrationRepository(
    private val json: Json,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TrackCalibrationRepository {

    private var baseDir: Path? = null

    override fun setPath(path: String) {
        println("this file setPath ${this@FileTrackCalibrationRepository}")

        baseDir = Path(path)
    }

    override suspend fun save(calibration: TrackCalibration) {
        withContext(ioDispatcher) {
            val baseDir = baseDir ?: setDefaultPath().also {
                baseDir = it
            }

            Files.createDirectories(baseDir)

            val file = baseDir.resolve("${calibration.trackId}.json")
            val tmp = baseDir.resolve("${calibration.trackId}.json.tmp")

            val text = json.encodeToString(calibration)

            tmp.writeText(text)
            Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        }
    }

    override suspend fun load(trackId: String): TrackCalibration? = withContext(ioDispatcher) {
        println("this file load ${this@FileTrackCalibrationRepository}")
        val baseDir = baseDir ?: setDefaultPath().also {
            baseDir = it
        }

        val file = baseDir.resolve("$trackId.json")
        if (!file.exists()) return@withContext null

        val text = file.readText()
        json.decodeFromString<TrackCalibration>(text)
    }

    override suspend fun loadAll(): List<TrackCalibration> = withContext(ioDispatcher) {
        val baseDir = baseDir ?: setDefaultPath().also {
            baseDir = it
        }

        if (!baseDir.exists()) return@withContext emptyList()

        baseDir.listDirectoryEntries("*.json")
            .mapNotNull { path ->
                runCatching {
                    json.decodeFromString<TrackCalibration>(path.readText())
                }.getOrNull()
            }
    }

    private fun setDefaultPath(): Path {
        val home = System.getProperty("user.home")
        return Path(home, "Downloads", "track calibrations").also {
            if (it.exists()) return@also
            Files.createDirectories(it)
        }
    }
}
