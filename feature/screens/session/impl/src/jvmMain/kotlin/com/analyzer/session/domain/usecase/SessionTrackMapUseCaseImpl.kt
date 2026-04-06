package com.analyzer.session.domain.usecase

import com.analyzer.session.domain.mapper.toTrackMapData
import com.analyzer.session.domain.model.NormalizedTrackMapRequest
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import com.project.analyzer.ui.components.TrackMapData
import com.project.analyzer.utils.trackmap.TrackMapPreparationUtil
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.Locale

@Inject
@SingleIn(ScreenScope::class)
internal class SessionTrackMapUseCaseImpl(
    private val trackMapRepository: TrackMapRepository,
    private val trackMapPreparationUtil: TrackMapPreparationUtil,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : SessionTrackMapUseCase {

    override suspend fun loadTrackMaps(items: Collection<SessionTrackMapIdentity>): Map<String, TrackMapData> =
        withContext(ioDispatcher) {
            val requests = normalizeRequests(items)
            if (requests.isEmpty()) return@withContext emptyMap()

            coroutineScope {
                val resolved = requests.map { request ->
                    async {
                        request.requestedKey to loadTrackMap(request)
                    }
                }.awaitAll()

                buildMap {
                    resolved.forEach { (requestedKey, map) ->
                        if (map != null) {
                            put(requestedKey, map)
                        }
                    }
                }
            }
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

    private fun normalizeRequests(items: Collection<SessionTrackMapIdentity>): List<NormalizedTrackMapRequest> {
        if (items.isEmpty()) return emptyList()

        val requestsByKey = linkedMapOf<String, NormalizedTrackMapRequest>()
        items.forEach { item ->
            val normalizedGameId = item.gameId.normalizedGameId() ?: return@forEach
            val normalizedTrackId = item.trackId.normalizedTrackId() ?: return@forEach
            val normalizedLayoutId = item.layoutId.normalizedLayoutId()
            val requestedKey = trackMapPreparationUtil.key(
                gameId = normalizedGameId,
                trackId = normalizedTrackId,
                layoutId = normalizedLayoutId,
            ) ?: return@forEach
            requestsByKey.putIfAbsent(
                requestedKey,
                NormalizedTrackMapRequest(
                    requestedKey = requestedKey,
                    gameId = normalizedGameId,
                    trackId = normalizedTrackId,
                    layoutId = normalizedLayoutId,
                ),
            )
        }
        return requestsByKey.values.toList()
    }

    private suspend fun loadTrackMap(request: NormalizedTrackMapRequest): TrackMapData? =
        loadRepositoryTrackMap(request)

    private suspend fun loadRepositoryTrackMap(request: NormalizedTrackMapRequest): TrackMapData? = runCatching {
        trackMapRepository.load(
            gameId = request.gameId,
            trackId = request.trackId,
            layoutId = request.layoutId,
        )
    }.getOrNull()?.toTrackMapData(trackMapPreparationUtil)

    private fun String.normalizedGameId(): String? = trim().lowercase(Locale.US).takeIf { it.isNotBlank() }

    private fun String.normalizedTrackId(): String? = trim().lowercase(Locale.US).takeIf { it.isNotBlank() }

    private fun String?.normalizedLayoutId(): String? = this
        ?.trim()
        ?.lowercase(Locale.US)
        ?.takeIf { it.isNotBlank() }
}
