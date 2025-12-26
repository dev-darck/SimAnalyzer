package com.project.analyzer.ac.telemetry.impl.fallback

import dev.zacsweers.metro.Inject
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

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
        val resourcePath = "/track_calibrations/red_bull_ring_gp.json"
        val stream = javaClass.getResourceAsStream(resourcePath) ?: return null
        return runCatching { json.decodeFromStream(TrackCalibrationDto.serializer(), stream) }.getOrNull()
    }
}
