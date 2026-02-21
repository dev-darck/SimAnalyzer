package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.utils.AppDirectories
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.first

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TrackCalibrationRepository>())
class TrackCalibrationStoreRepository(appDirectories: AppDirectories) : TrackCalibrationRepository {

    private val store = createTrackCalibrationStoreDataStore(
        directory = appDirectories.preferencesDir,
        fileName = FILE_NAME,
    )

    override suspend fun save(calibration: TrackCalibration) {
        val trackId = calibration.trackId.trim()
        if (trackId.isBlank()) return

        store.updateData { current ->
            val filtered = current.calibrations.filterNot { it.trackId == trackId }
            current.copy(calibrations = filtered + calibration.copy(trackId = trackId))
        }
    }

    override suspend fun load(trackId: String): TrackCalibration? =
        store.data.first().calibrations.firstOrNull { it.trackId == trackId }

    override suspend fun loadAll(): List<TrackCalibration> = store.data.first().calibrations

    companion object {

        const val FILE_NAME = "track_calibrations.pb"
    }
}
