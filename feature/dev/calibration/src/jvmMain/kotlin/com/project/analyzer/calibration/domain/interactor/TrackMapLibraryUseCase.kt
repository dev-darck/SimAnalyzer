package com.project.analyzer.calibration.domain.interactor

import com.project.analyzer.api.di.IO
import com.project.analyzer.calibration.presentation.trackmap.TrackMapLibraryItem
import com.project.analyzer.calibration.trackmap.TrackMapStatsCalculator
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Inject
class TrackMapLibraryUseCase(
    private val repository: TrackMapRepository,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val stats = TrackMapStatsCalculator()

    suspend fun loadItems(): List<TrackMapLibraryItem> {
        val maps = withContext(ioDispatcher) {
            repository.loadAll().sortedByDescending { it.createdAtEpochMs }
        }
        return maps.map { map ->
            val points = map.points.map { Vec2(it.x, it.y) }
            val leftWidths = map.points.map { it.leftWidthMeters }
            val rightWidths = map.points.map { it.rightWidthMeters }
            val pitPoints = map.pitPoints.map { Vec2(it.x, it.y) }
            val bounds = map.bounds ?: stats.computeBounds(points)
            val distance = stats.computeDistance(points)
            val pitEntryPoint = map.pitEntryIndex.takeIf { it in points.indices }?.let { points[it] }
            val pitExitPoint = map.pitExitIndex.takeIf { it in points.indices }?.let { points[it] }
            TrackMapLibraryItem(
                map = map,
                points = points,
                leftWidthsMeters = leftWidths,
                rightWidthsMeters = rightWidths,
                pitPoints = pitPoints,
                bounds = bounds,
                distanceMeters = distance,
                pitEntryPoint = pitEntryPoint,
                pitExitPoint = pitExitPoint,
            )
        }
    }
}
