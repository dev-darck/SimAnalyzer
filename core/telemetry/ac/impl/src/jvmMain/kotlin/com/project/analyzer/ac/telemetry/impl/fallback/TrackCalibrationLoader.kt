package com.project.analyzer.ac.telemetry.impl.fallback

import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

@Inject
class TrackCalibrationLoader(
    private val json: Json
) {
    fun load(trackId: String): TrackCalibrationDto? {
        loadById(trackId)?.let { return it }

        val normalized = normalizeTrackId(trackId)
        if (normalized != trackId) loadById(normalized)?.let { return it }

        return null
    }

    fun normalizeTrackId(track: String): String {
        return track
            .trim()
            .lowercase()
            .replace(Regex("""\s+"""), "_")
            .replace(Regex("""[^a-z0-9_]+"""), "")
            .replace(Regex("""_+"""), "_")
            .trim('_')
    }

    private fun loadById(id: String): TrackCalibrationDto? {
        // 1) Try user-generated calibrations (same path as FileTrackCalibrationRepository default)
        val userDir = Path(System.getProperty("user.home"), "Downloads", "track calibrations")
        val file = userDir.resolve("$id.json")
        if (file.exists()) {
            return runCatching {
                json.decodeFromString(TrackCalibrationDto.serializer(), file.readText())
            }.getOrNull()
        }

        // 2) Try bundled resources under /track_calibrations/{id}.json
        val resourcePath = "/track_calibrations/$id.json"
        val stream = javaClass.getResourceAsStream(resourcePath) ?: return null
        return runCatching { json.decodeFromStream(TrackCalibrationDto.serializer(), stream) }.getOrNull()
    }
}
