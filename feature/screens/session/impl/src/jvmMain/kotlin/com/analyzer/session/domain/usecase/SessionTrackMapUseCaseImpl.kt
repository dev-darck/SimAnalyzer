package com.analyzer.session.domain.usecase

import com.analyzer.session.domain.model.SessionListDomainItem
import com.project.analyzer.api.di.IO
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.collections.immutable.toImmutableList
import java.util.Locale

@Inject
@SingleIn(ScreenScope::class)
internal class SessionTrackMapUseCaseImpl(
    private val trackMapRepository: TrackMapRepository,
    private val trackMapPreparationUtil: TrackMapPreparationUtil,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : SessionTrackMapUseCase {

    override suspend fun loadTrackMaps(items: Collection<SessionListDomainItem>): Map<String, TrackMapData> =
        withContext(
            ioDispatcher,
        ) {
            if (items.isEmpty()) return@withContext emptyMap()

            val identitiesByKey = linkedMapOf<String, Triple<String, String, String?>>()
            items.forEach { item ->
                val normalizedGameId =
                    item.gameId.trim().lowercase(Locale.US).takeIf { it.isNotBlank() } ?: return@forEach
                val normalizedTrackId = item.trackId.trim().lowercase(
                    Locale.US,
                ).takeIf { it.isNotBlank() } ?: return@forEach
                val normalizedLayoutId = item.layoutId?.trim()?.lowercase(Locale.US)?.takeIf { it.isNotBlank() }
                val key = trackMapPreparationUtil.key(
                    gameId = normalizedGameId,
                    trackId = normalizedTrackId,
                    layoutId = normalizedLayoutId,
                ) ?: return@forEach
                identitiesByKey.putIfAbsent(key, Triple(normalizedGameId, normalizedTrackId, normalizedLayoutId))
            }

            if (identitiesByKey.isEmpty()) return@withContext emptyMap()

            val result = linkedMapOf<String, TrackMapData>()
            identitiesByKey.forEach { (key, identity) ->
                val map = runCatching {
                    trackMapRepository.load(
                        gameId = identity.first,
                        trackId = identity.second,
                        layoutId = identity.third,
                    )
                }.getOrNull() ?: return@forEach

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

                result[key] = TrackMapData(
                    points = prepared.points.map { point -> TrackMapPoint(x = point.x, y = point.y) }.toImmutableList(),
                    pitPoints = prepared.pitPoints.map { point -> TrackMapPoint(x = point.x, y = point.y) }.toImmutableList(),
                    bounds = TrackMapBounds(
                        minX = prepared.bounds.minX,
                        minY = prepared.bounds.minY,
                        maxX = prepared.bounds.maxX,
                        maxY = prepared.bounds.maxY,
                    ),
                )
            }
            result
        }

    override fun resolveTrackMap(
        trackMapsByKey: Map<String, TrackMapData>,
        gameId: String,
        trackId: String,
        layoutId: String?,
    ): TrackMapData? = trackMapPreparationUtil.key(
        gameId = gameId,
        trackId = trackId,
        layoutId = layoutId,
    )?.let(trackMapsByKey::get)
}
