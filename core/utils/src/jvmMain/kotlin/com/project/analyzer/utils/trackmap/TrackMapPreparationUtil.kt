package com.project.analyzer.utils.trackmap

import dev.zacsweers.metro.Inject
import java.util.Locale

public data class TrackMapPreparedPoint(val x: Float, val y: Float)

public data class TrackMapPreparedBounds(val minX: Float, val minY: Float, val maxX: Float, val maxY: Float)

public data class TrackMapPreparedData(
    val points: List<TrackMapPreparedPoint>,
    val pitPoints: List<TrackMapPreparedPoint> = emptyList(),
    val bounds: TrackMapPreparedBounds,
)

public data class TrackMapSourceData(
    val gameId: String?,
    val trackId: String?,
    val layoutId: String? = null,
    val createdAtMs: Long,
    val points: List<TrackMapPreparedPoint>,
    val pitPoints: List<TrackMapPreparedPoint> = emptyList(),
    val bounds: TrackMapPreparedBounds? = null,
)

@Inject
public class TrackMapPreparationUtil {

    public fun key(gameId: String?, trackId: String?, layoutId: String? = null): String? {
        val normalizedGameId = gameId
            ?.trim()
            ?.lowercase(Locale.US)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val normalizedTrackId = trackId
            ?.trim()
            ?.lowercase(Locale.US)
            ?.takeIf { it.isNotBlank() }
            ?: return null
        val normalizedLayoutId = layoutId
            ?.trim()
            ?.lowercase(Locale.US)
            ?.takeIf { it.isNotBlank() }
        return if (normalizedLayoutId == null) {
            "$normalizedGameId|$normalizedTrackId"
        } else {
            "$normalizedGameId|$normalizedTrackId|$normalizedLayoutId"
        }
    }

    public fun prepare(
        points: List<TrackMapPreparedPoint>,
        pitPoints: List<TrackMapPreparedPoint> = emptyList(),
        bounds: TrackMapPreparedBounds? = null,
    ): TrackMapPreparedData? {
        val cleanPoints = points.filter { point -> point.x.isFinite() && point.y.isFinite() }
        if (cleanPoints.size < 2) return null

        val cleanPitPoints = pitPoints.filter { point -> point.x.isFinite() && point.y.isFinite() }
        val resolvedBounds = bounds?.takeIf(::isBoundsValid) ?: computeBounds(cleanPoints + cleanPitPoints)
        if (resolvedBounds == null || !isBoundsValid(resolvedBounds)) return null

        return TrackMapPreparedData(
            points = cleanPoints,
            pitPoints = cleanPitPoints,
            bounds = resolvedBounds,
        )
    }

    public fun latestByKey(sources: Iterable<TrackMapSourceData>): Map<String, TrackMapPreparedData> {
        val latestByKey = linkedMapOf<String, PreparedSnapshot>()
        sources.forEach { source ->
            val sourceKey = key(
                gameId = source.gameId,
                trackId = source.trackId,
                layoutId = source.layoutId,
            ) ?: return@forEach
            val prepared = prepare(
                points = source.points,
                pitPoints = source.pitPoints,
                bounds = source.bounds,
            ) ?: return@forEach
            val existing = latestByKey[sourceKey]
            if (existing == null || source.createdAtMs >= existing.createdAtMs) {
                latestByKey[sourceKey] = PreparedSnapshot(
                    createdAtMs = source.createdAtMs,
                    data = prepared,
                )
            }
        }
        return latestByKey.mapValues { (_, snapshot) -> snapshot.data }
    }

    private fun isBoundsValid(bounds: TrackMapPreparedBounds): Boolean = bounds.minX.isFinite() &&
        bounds.minY.isFinite() &&
        bounds.maxX.isFinite() &&
        bounds.maxY.isFinite() &&
        bounds.maxX > bounds.minX &&
        bounds.maxY > bounds.minY

    private fun computeBounds(points: List<TrackMapPreparedPoint>): TrackMapPreparedBounds? {
        if (points.isEmpty()) return null

        var minX = points.first().x
        var minY = points.first().y
        var maxX = points.first().x
        var maxY = points.first().y

        points.forEach { point ->
            if (point.x < minX) minX = point.x
            if (point.y < minY) minY = point.y
            if (point.x > maxX) maxX = point.x
            if (point.y > maxY) maxY = point.y
        }

        return TrackMapPreparedBounds(
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
        )
    }

    private data class PreparedSnapshot(val createdAtMs: Long, val data: TrackMapPreparedData)
}
