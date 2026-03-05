package com.analyzer.session.presentation.usecase

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import com.project.analyzer.ui.components.TrackMapBounds
import com.project.analyzer.ui.components.TrackMapData
import com.project.analyzer.ui.components.TrackMapPoint
import com.project.analyzer.utils.trackmap.TrackMapPreparationUtil
import com.project.analyzer.utils.trackmap.TrackMapPreparedBounds
import com.project.analyzer.utils.trackmap.TrackMapPreparedPoint
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.Locale

@Inject
@SingleIn(ScreenScope::class)
internal class SessionTrackMapUseCaseImpl(
    private val trackMapRepository: TrackMapRepository,
    private val trackMapPreparationUtil: TrackMapPreparationUtil,
) : SessionTrackMapUseCase {

    override fun request(gameId: String, trackId: String): SessionTrackMapRequest? {
        val normalizedGameId = gameId.trim().lowercase(Locale.US).takeIf { it.isNotBlank() } ?: return null
        val normalizedTrackId = trackId.trim().lowercase(Locale.US).takeIf { it.isNotBlank() } ?: return null
        return SessionTrackMapRequest(
            gameId = normalizedGameId,
            trackId = normalizedTrackId,
        )
    }

    override suspend fun loadTrackMapsByRequest(requests: Set<SessionTrackMapRequest>): Map<String, TrackMapData> {
        if (requests.isEmpty()) return emptyMap()
        val result = linkedMapOf<String, TrackMapData>()
        requests.forEach { request ->
            val map =
                runCatching { trackMapRepository.load(request.gameId, request.trackId) }.getOrNull() ?: return@forEach
            val prepared = trackMapPreparationUtil.prepare(
                points = map.points.map { point -> TrackMapPreparedPoint(x = point.x, y = point.y) },
                pitPoints = map.pitPoints.map { point -> TrackMapPreparedPoint(x = point.x, y = point.y) },
                bounds = map.bounds?.let { bounds ->
                    TrackMapPreparedBounds(
                        minX = bounds.minX,
                        minY = bounds.minY,
                        maxX = bounds.maxX,
                        maxY = bounds.maxY,
                    )
                },
            ) ?: return@forEach

            val key = trackMapPreparationUtil.key(
                gameId = request.gameId,
                trackId = request.trackId,
            ) ?: return@forEach

            result[key] = TrackMapData(
                points = prepared.points.map { point -> TrackMapPoint(x = point.x, y = point.y) },
                pitPoints = prepared.pitPoints.map { point -> TrackMapPoint(x = point.x, y = point.y) },
                bounds = TrackMapBounds(
                    minX = prepared.bounds.minX,
                    minY = prepared.bounds.minY,
                    maxX = prepared.bounds.maxX,
                    maxY = prepared.bounds.maxY,
                ),
            )
        }
        return result
    }

    override fun resolveTrackMap(
        trackMapsByKey: Map<String, TrackMapData>,
        gameId: String,
        trackId: String,
    ): TrackMapData? = trackMapPreparationUtil.key(gameId = gameId, trackId = trackId)?.let(trackMapsByKey::get)
}
