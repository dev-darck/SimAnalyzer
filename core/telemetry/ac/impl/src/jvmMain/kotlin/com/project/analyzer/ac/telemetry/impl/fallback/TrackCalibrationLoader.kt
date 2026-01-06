package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream

@Inject
class TrackCalibrationLoader(
    private val json: Json
) {

    @OptIn(ExperimentalSerializationApi::class)
    fun load(trackId: String): TrackCalibration? {
        val resourcePath = "/track_calibrations/$trackId.json"
        val stream = javaClass.getResourceAsStream(resourcePath) ?: return null
        return runCatching { json.decodeFromStream(TrackCalibration.serializer(), stream) }.getOrNull()
    }
}
