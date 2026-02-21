package com.project.analyzer.ac.telemetry.impl.trackmap

import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import com.project.analyzer.utils.AppDirectories
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.first

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TrackMapRepository>())
class TrackMapStoreRepository(appDirectories: AppDirectories) : TrackMapRepository {

    private val store = createTrackMapStoreDataStore(
        directory = appDirectories.preferencesDir,
        fileName = FILE_NAME,
    )

    override suspend fun save(trackMap: TrackMap) {
        val normalized = sanitize(trackMap)
        val trackId = normalized.trackId.trim()
        val gameId = normalized.gameId.trim()
        if (trackId.isBlank() || gameId.isBlank()) return

        store.updateData { current ->
            val normalizedMaps = current.maps.map(::sanitize)
            val filtered = normalizedMaps.filterNot {
                it.gameId == gameId && it.trackId == trackId && it.layoutId == normalized.layoutId
            }
            current.copy(maps = filtered + normalized.copy(gameId = gameId, trackId = trackId))
        }
    }

    override suspend fun load(gameId: String, trackId: String): TrackMap? =
        store.data.first().maps.firstOrNull { it.gameId == gameId && it.trackId == trackId }

    override suspend fun loadAll(gameId: String?): List<TrackMap> {
        val maps = store.data.first().maps.map(::sanitize)
        return if (gameId.isNullOrBlank()) maps else maps.filter { it.gameId == gameId }
    }

    companion object {

        const val FILE_NAME = "track_maps.pb"
    }

    private fun sanitize(map: TrackMap): TrackMap {
        val normalizedLayout = map.layoutId?.trim().orEmpty()
        val normalizedPoints = map.points.map(::sanitizePoint)
        val normalizedPitPoints = map.pitPoints.map(::sanitizePoint)
        val normalizedBounds = map.bounds ?: computeBounds(normalizedPoints)
        ?: TrackMapBounds(0f, 0f, 0f, 0f)
        val maxIndex = normalizedPoints.lastIndex
        val pitEntry = map.pitEntryIndex.takeIf { it in 0..maxIndex } ?: -1
        val pitExit = map.pitExitIndex.takeIf { it in 0..maxIndex } ?: -1
        if (normalizedLayout == map.layoutId &&
            normalizedPoints == map.points &&
            normalizedPitPoints == map.pitPoints &&
            normalizedBounds == map.bounds &&
            pitEntry == map.pitEntryIndex &&
            pitExit == map.pitExitIndex
        ) {
            return map
        }
        return map.copy(
            layoutId = normalizedLayout,
            points = normalizedPoints,
            pitPoints = normalizedPitPoints,
            bounds = normalizedBounds,
            pitEntryIndex = pitEntry,
            pitExitIndex = pitExit,
        )
    }

    private fun sanitizePoint(point: TrackMapPoint): TrackMapPoint {
        val safeLeft = point.leftWidthMeters
            .takeIf { it.isFinite() && it >= 0f }
            ?: 0f
        val safeRight = point.rightWidthMeters
            .takeIf { it.isFinite() && it >= 0f }
            ?: 0f
        if (safeLeft == point.leftWidthMeters && safeRight == point.rightWidthMeters) {
            return point
        }
        return point.copy(
            leftWidthMeters = safeLeft,
            rightWidthMeters = safeRight,
        )
    }

    private fun computeBounds(points: List<TrackMapPoint>): TrackMapBounds? {
        if (points.isEmpty()) return null
        var minX = points[0].x
        var minY = points[0].y
        var maxX = points[0].x
        var maxY = points[0].y
        for (point in points) {
            if (point.x < minX) minX = point.x
            if (point.y < minY) minY = point.y
            if (point.x > maxX) maxX = point.x
            if (point.y > maxY) maxY = point.y
        }
        return TrackMapBounds(
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
        )
    }
}
