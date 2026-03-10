package com.analyzer.trackmap.data.library

import com.analyzer.trackmap.domain.model.TrackMapLibraryItem
import com.project.analyzer.api.di.IO
import com.project.analyzer.math.Vec2
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TrackMapLibraryRepository>())
@Inject
class TrackMapLibraryRepositoryImpl(
    private val repository: TrackMapRepository,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TrackMapLibraryRepository {

    private val stats = TrackMapStatsCalculator()

    override suspend fun loadItems(): List<TrackMapLibraryItem> {
        val maps = withContext(ioDispatcher) {
            repository.loadAll().sortedByDescending(TrackMap::createdAtEpochMs)
        }
        return maps.map(::toLibraryItem)
    }

    override suspend fun loadItem(gameId: String, trackId: String, layoutId: String?): TrackMapLibraryItem? =
        withContext(ioDispatcher) {
            repository.load(
                gameId = gameId,
                trackId = trackId,
                layoutId = layoutId,
            )?.let(::toLibraryItem)
        }

    private fun toLibraryItem(map: TrackMap): TrackMapLibraryItem {
        val points = map.points.map { Vec2(it.x, it.y) }
        val leftWidths = map.points.map { it.leftWidthMeters }
        val rightWidths = map.points.map { it.rightWidthMeters }
        val pitPoints = map.pitPoints.map { Vec2(it.x, it.y) }
        val bounds = map.bounds ?: stats.computeBounds(points)
        val distance = stats.computeDistance(points)
        val pitEntryPoint = map.pitEntryIndex.takeIf { it in points.indices }?.let(points::get)
        val pitExitPoint = map.pitExitIndex.takeIf { it in points.indices }?.let(points::get)
        return TrackMapLibraryItem(
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
