package com.analyzer.trackmap.domain.usecase.editor

import com.analyzer.trackmap.data.calibration.TrackMapCalibrationRepository
import com.analyzer.trackmap.data.library.TrackMapLibraryRepository
import com.analyzer.trackmap.data.selection.TrackMapEditorSelectionCache
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorContent
import com.analyzer.trackmap.domain.model.TrackMapCalibrationEditorSnapshot
import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout

@SingleIn(ScreenScope::class)
@ContributesBinding(ScreenScope::class, binding = binding<TrackMapCalibrationEditorUseCase>())
@Inject
class TrackMapCalibrationEditorUseCaseImpl(
    private val libraryRepository: TrackMapLibraryRepository,
    private val calibrationRepository: TrackMapCalibrationRepository,
    private val snapshotFactory: TrackMapCalibrationEditorSnapshotFactory,
    private val selectionCache: TrackMapEditorSelectionCache,
) : TrackMapCalibrationEditorUseCase {

    private val logger = logger()

    override suspend fun load(gameId: String, trackId: String, layoutId: String?): TrackMapCalibrationEditorContent? {
        logger.info {
            "TrackMapEditorUseCase.load start gameId=$gameId trackId=$trackId layoutId=${layoutId.orEmpty()}"
        }
        val cachedItem = selectionCache.find(
            gameId = gameId,
            trackId = trackId,
            layoutId = layoutId,
        )
        if (cachedItem != null) {
            logger.info {
                "TrackMapEditorUseCase.load selectionCache hit trackId=${cachedItem.map.trackId} layoutId=${cachedItem.map.layoutId.orEmpty()}"
            }
        } else {
            logger.warn {
                "TrackMapEditorUseCase.load selectionCache miss trackId=$trackId layoutId=${layoutId.orEmpty()}"
            }
        }
        val item = cachedItem ?: try {
            logger.info {
                "TrackMapEditorUseCase.load fallback loadItem start trackId=$trackId layoutId=${layoutId.orEmpty()}"
            }
            withTimeout(EDITOR_ITEM_LOAD_TIMEOUT_MS) {
                libraryRepository.loadItem(
                    gameId = gameId,
                    trackId = trackId,
                    layoutId = layoutId,
                )
            }
        } catch (_: TimeoutCancellationException) {
            throw IllegalStateException("Timed out loading track map $trackId")
        } ?: return null
        selectionCache.remember(item)
        logger.info {
            "TrackMapEditorUseCase.load item ready trackId=${item.map.trackId} layoutId=${item.map.layoutId.orEmpty()} points=${item.points.size}"
        }
        val calibration = calibrationRepository.load(
            trackId = item.map.trackId,
            layoutId = item.map.layoutId,
        )
        logger.info {
            "TrackMapEditorUseCase.load calibration ${if (calibration == null) "miss" else "hit"} trackId=${item.map.trackId} layoutId=${item.map.layoutId.orEmpty()}"
        }
        val snapshot = if (calibration == null) {
            snapshotFactory.createEmpty(item)
        } else {
            snapshotFactory.createLoaded(item, calibration)
        }
        val message = calibration?.let {
            "Loaded ${it.source.name.lowercase()} calibration"
        } ?: "Calibration not found for ${item.map.trackId}. Create Start / Finish to begin."
        return TrackMapCalibrationEditorContent(
            item = item,
            snapshot = snapshot,
            message = message,
        )
    }

    override suspend fun save(
        item: TrackMapLibraryItem,
        snapshot: TrackMapCalibrationEditorSnapshot,
    ): TrackMapCalibrationEditorSnapshot {
        calibrationRepository.save(
            calibration = snapshotFactory.toTrackCalibration(
                item = item,
                snapshot = snapshot,
            ),
        )
        return snapshot.copy(source = TrackCalibrationSource.USER)
    }

    private companion object {

        const val EDITOR_ITEM_LOAD_TIMEOUT_MS = 5_000L
    }
}
