package com.project.analyzer.calibration.domain.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationWorkspace
import com.project.analyzer.telemetry.ac.api.model.calibration.SectorCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

@SingleIn(ScreenScope::class)
@Inject
internal class SaveTrackCalibrationUseCaseImpl(
    private val repo: TrackCalibrationRepository,
    private val calibrationWorkspace: TrackCalibrationWorkspace,
) : SaveTrackCalibrationUseCase {

    override suspend fun save(calibration: TrackCalibration) {
        calibrationWorkspace.save(calibration)
    }

    override suspend fun save(draft: TrackCalibrationDraft) {
        val sectorCount = draft.sectorStartMarks.size + 1
        val sectors = (1..sectorCount).map { index ->
            SectorCalibration(
                index = index,
                start = requireNotNull(draft.sectorStart(index)) { "Sector S$index start missing" },
                finish = requireNotNull(draft.sectorFinish(index)) { "Sector S$index finish missing" },
            )
        }

        save(
            TrackCalibration(
                trackId = draft.trackId,
                trackName = draft.trackName,
                layoutId = null,
                createdAtEpochMs = System.currentTimeMillis(),
                source = TrackCalibrationSource.USER,
                referencePoint = draft.referencePoint,
                startFinish = draft.startFinish,
                sectors = sectors,
            ),
        )
    }

    override suspend fun loadAll(): List<String> = repo.loadAll()
        .map { calibration ->
            buildString {
                append(calibration.trackId)
                calibration.layoutId?.takeIf { it.isNotBlank() }?.let { layoutId ->
                    append(" [")
                    append(layoutId)
                    append(']')
                }
            }
        }
        .distinct()

    private fun TrackCalibrationDraft.sectorStart(index: Int) = when (index) {
        1 -> startFinish
        else -> sectorStartMarks.getOrNull(index - 2)
    }

    private fun TrackCalibrationDraft.sectorFinish(index: Int) = when {
        sectorStartMarks.isEmpty() -> startFinish
        index < sectorStartMarks.size + 1 -> sectorStart(index + 1)
        index == sectorStartMarks.size + 1 -> startFinish
        else -> null
    }
}
