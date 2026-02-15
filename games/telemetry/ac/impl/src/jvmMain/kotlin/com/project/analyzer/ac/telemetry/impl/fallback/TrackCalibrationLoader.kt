package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.calibration.TrackCalibrationStore
import com.project.analyzer.ac.telemetry.impl.calibration.TrackCalibrationStoreRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.utils.AppDirectories
import dev.zacsweers.metro.Inject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.File

@Inject
class TrackCalibrationLoader(
    private val json: Json,
    private val appDirectories: AppDirectories,
) {

    @OptIn(ExperimentalSerializationApi::class)
    fun load(trackId: String): TrackCalibration? {
        loadFromStore(trackId)?.let { return it }

        val resourcePath = "/track_calibrations/$trackId.json"
        val stream = javaClass.getResourceAsStream(resourcePath) ?: return null
        return runCatching { json.decodeFromStream(TrackCalibration.serializer(), stream) }.getOrNull()
    }

    private fun loadFromStore(trackId: String): TrackCalibration? {
        val file = File(appDirectories.preferencesDir, TrackCalibrationStoreRepository.FILE_NAME)
        if (!file.exists()) return null

        val bytes = runCatching { file.readBytes() }.getOrNull() ?: return null
        if (bytes.isEmpty()) return null

        val store = runCatching {
            ProtoBuf.decodeFromByteArray(TrackCalibrationStore.serializer(), bytes)
        }.getOrNull() ?: return null

        return store.calibrations.firstOrNull { it.trackId == trackId }
    }
}
