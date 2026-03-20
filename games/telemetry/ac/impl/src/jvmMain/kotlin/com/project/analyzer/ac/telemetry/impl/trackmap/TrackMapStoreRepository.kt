package com.project.analyzer.ac.telemetry.impl.trackmap

import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.game.api.AC_KEY
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import com.project.analyzer.utils.AppDirectories
import com.project.analyzer.utils.TrackIdentityAliasMatcher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.flow.first

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TrackMapRepository>())
@Inject
class TrackMapStoreRepository internal constructor(
    appDirectories: AppDirectories,
    private val gameProvider: TrackMapGameProvider,
) : TrackMapRepository {

    private val store = createTrackMapStoreDataStore(
        directory = appDirectories.preferencesDir,
        fileName = FILE_NAME,
    )

    override suspend fun save(trackMap: TrackMap) {
        val normalized = sanitize(trackMap)
        val trackId = normalized.trackId.trim()
        val gameId = normalized.gameId.normalizedGameId()
        val layoutId = normalized.layoutId.normalizedLayoutId()
        if (trackId.isBlank() || gameId.isBlank()) return

        store.updateData { current ->
            val normalizedMaps = current.maps.map(::sanitize)
            val filtered = normalizedMaps.filterNot {
                it.gameId == gameId &&
                    it.trackId == trackId &&
                    it.layoutId.normalizedLayoutId() == layoutId
            }
            current.copy(
                maps = filtered + normalized.copy(
                    gameId = gameId,
                    trackId = trackId,
                    layoutId = layoutId,
                ),
            )
        }
    }

    override suspend fun load(gameId: String, trackId: String, layoutId: String?): TrackMap? {
        val request = TrackMapLookupRequest(
            gameId = gameId.normalizedGameId(),
            trackId = trackId.trim(),
            layoutId = layoutId.normalizedLayoutId(),
        )
        return resolveUserMap(request) ?: resolveGameMap(request)
    }

    override suspend fun loadAll(gameId: String?): List<TrackMap> {
        val normalizedGameId = gameId?.normalizedGameId()
        val userMaps = store.data.first().maps.map(::sanitize)
        val filteredUserMaps = if (normalizedGameId.isNullOrBlank()) {
            userMaps
        } else {
            userMaps.filter { it.gameId == normalizedGameId }
        }
        val gameMaps = gameProvider.loadAll(normalizedGameId).map(::sanitize)
        if (gameMaps.isEmpty()) return filteredUserMaps
        if (filteredUserMaps.isEmpty()) return gameMaps

        val merged = linkedMapOf<String, TrackMap>()
        gameMaps.forEach { map ->
            merged[map.mergeKey()] = map
        }
        filteredUserMaps.forEach { map ->
            merged[map.mergeKey()] = map
        }
        return merged.values.toList()
    }

    private fun String?.normalizedLayoutId(): String = TrackIdNormalizer.normalizeLayoutId(this).orEmpty()

    private fun String.normalizedGameId(): String = trim()
        .lowercase()
        .let { normalized ->
            when (normalized) {
                "ac", "ace", AC_KEY -> AC_KEY
                else -> normalized
            }
        }

    private suspend fun resolveUserMap(request: TrackMapLookupRequest): TrackMap? {
        val candidates = selectCandidates(
            maps = store.data.first().maps.map(::sanitize),
            gameId = request.gameId,
            trackId = request.trackId,
            layoutId = request.layoutId,
        )
        return candidates.resolvePreferredMap(layoutId = request.layoutId)
    }

    private fun List<TrackMap>.resolvePreferredMap(layoutId: String): TrackMap? = when {
        isEmpty() -> null

        layoutId.isNotEmpty() -> firstOrNull { it.layoutId.normalizedLayoutId() == layoutId }
            ?: firstOrNull { it.layoutId.normalizedLayoutId().isEmpty() }

        else -> firstOrNull { it.layoutId.normalizedLayoutId().isEmpty() }
            ?: maxByOrNull(TrackMap::createdAtEpochMs)
    }

    private suspend fun resolveGameMap(request: TrackMapLookupRequest): TrackMap? {
        gameProvider.load(
            gameId = request.gameId,
            trackId = request.trackId,
            layoutId = request.layoutId.ifBlank { null },
        )?.let(::sanitize)?.let { return it }

        val candidates = selectCandidates(
            maps = gameProvider.loadAll(request.gameId).map(::sanitize),
            gameId = request.gameId,
            trackId = request.trackId,
            layoutId = request.layoutId,
        )
        return candidates.resolvePreferredMap(layoutId = request.layoutId)
    }

    private fun selectCandidates(
        maps: List<TrackMap>,
        gameId: String,
        trackId: String,
        layoutId: String,
    ): List<TrackMap> {
        val sameGameMaps = maps.filter { it.gameId == gameId }
        if (sameGameMaps.isEmpty()) return emptyList()

        val exact = sameGameMaps.filter { it.trackId == trackId }
        if (exact.isNotEmpty()) return exact

        val combinedTrackId = layoutId.takeIf(String::isNotBlank)?.let { normalizedLayoutId ->
            TrackIdNormalizer.normalize(track = trackId, layout = normalizedLayoutId)
        }
        if (!combinedTrackId.isNullOrBlank() && combinedTrackId != trackId) {
            val combined = sameGameMaps.filter { it.trackId == combinedTrackId }
            if (combined.isNotEmpty()) return combined
        }

        return selectAliasedCandidates(
            maps = sameGameMaps,
            trackId = trackId,
            layoutId = layoutId.ifEmpty { null },
        )
    }

    private fun selectAliasedCandidates(maps: List<TrackMap>, trackId: String, layoutId: String?): List<TrackMap> {
        val scoredCandidates = maps.mapNotNull { map ->
            scoreAliasedTrackMapMatch(
                requestedTrackId = trackId,
                requestedLayoutId = layoutId,
                map = map,
            ).takeIf { it > 0 }?.let { score ->
                score to map
            }
        }
        val bestScore = scoredCandidates.maxOfOrNull { (score, _) -> score } ?: return emptyList()
        return scoredCandidates
            .filter { (score, _) -> score == bestScore }
            .map { (_, map) -> map }
    }

    private fun scoreAliasedTrackMapMatch(requestedTrackId: String, requestedLayoutId: String?, map: TrackMap): Int {
        val requestedKeys = buildTrackAliasKeys(
            trackId = requestedTrackId,
            layoutId = requestedLayoutId,
            extraLayoutId = map.layoutId,
        )
        if (requestedKeys.isEmpty()) return 0

        val mapKeys = buildTrackAliasKeys(
            trackId = map.trackId,
            layoutId = map.layoutId,
            extraLayoutId = requestedLayoutId,
        )
        if (mapKeys.isEmpty() || requestedKeys.none(mapKeys::contains)) return 0

        val baseScore = when {
            map.trackId == requestedTrackId &&
                map.layoutId.normalizedLayoutId() == requestedLayoutId.normalizedLayoutId() -> 5

            map.trackId == requestedTrackId -> 4

            map.layoutId.normalizedLayoutId() == requestedLayoutId.normalizedLayoutId() -> 3

            TrackIdentityAliasMatcher.areEquivalent(
                trackId = requestedTrackId,
                layoutId = requestedLayoutId,
                otherTrackId = map.trackId,
                otherLayoutId = map.layoutId,
            ) -> 2

            else -> 1
        }
        return baseScore * 10 + explicitAliasPreference(requestedTrackId, map.trackId)
    }

    private fun buildTrackAliasKeys(trackId: String, layoutId: String?, extraLayoutId: String?): Set<String> =
        buildSet {
            addAll(
                TrackIdentityAliasMatcher.buildAliasKeys(
                    trackId = trackId,
                    layoutId = layoutId,
                    extraLayouts = listOf(extraLayoutId),
                ),
            )
            explicitTrackIdAliases(trackId).forEach { aliasTrackId ->
                addAll(
                    TrackIdentityAliasMatcher.buildAliasKeys(
                        trackId = aliasTrackId,
                        layoutId = layoutId,
                        extraLayouts = listOf(extraLayoutId),
                    ),
                )
            }
        }

    private fun explicitTrackIdAliases(trackId: String): List<String> =
        EXPLICIT_TRACK_ID_ALIASES[trackId.trim().lowercase()].orEmpty()

    private fun explicitAliasPreference(requestedTrackId: String, candidateTrackId: String): Int {
        val aliases = explicitTrackIdAliases(requestedTrackId)
        if (aliases.isEmpty()) return 0
        val aliasIndex = aliases.indexOf(candidateTrackId.trim().lowercase())
        return if (aliasIndex >= 0) aliases.size - aliasIndex else 0
    }

    companion object {

        const val FILE_NAME = "track_maps.pb"

        private val EXPLICIT_TRACK_ID_ALIASES: Map<String, List<String>> = mapOf(
            "spa_camera_sequence_practice" to listOf("spa_gp"),
            "red_bull_ring_gp" to listOf("redbull_ring_gp"),
            "red_bull_ring_national" to listOf("redbull_ring_national"),
            "brands_hatch_camera_sequence_pra" to listOf("brands_hatch_gp", "brands_hatch_indy"),
            "watkins_glen_common_inner_loop" to listOf(
                "watkins_glen_short_inner_loop",
                "watkins_glen_gp_inner_loop",
            ),
            "watkins_glen_short_inner_loop" to listOf("watkins_glen_common_inner_loop"),
            "watkins_glen_gp_inner_loop" to listOf("watkins_glen_common_inner_loop"),
            "watkins_glen_common_no_inner_loo" to listOf("watkins_glen_gp"),
            "watkins_glen_common_no_inner_loop" to listOf("watkins_glen_gp"),
            "watkins_glen_gp" to listOf(
                "watkins_glen_common_no_inner_loo",
                "watkins_glen_common_no_inner_loop",
            ),
        )
    }

    private fun sanitize(map: TrackMap): TrackMap {
        val normalizedGameId = map.gameId.normalizedGameId()
        val normalizedLayout = map.layoutId.normalizedLayoutId()
        val normalizedPoints = map.points.map(::sanitizePoint)
        val normalizedPitPoints = map.pitPoints.map(::sanitizePoint)
        val normalizedBounds = map.bounds ?: computeBounds(normalizedPoints) ?: TrackMapBounds(0f, 0f, 0f, 0f)
        val maxIndex = normalizedPoints.lastIndex
        val pitEntry = map.pitEntryIndex.takeIf { it in 0..maxIndex } ?: -1
        val pitExit = map.pitExitIndex.takeIf { it in 0..maxIndex } ?: -1
        val sanitizedMap = map.copy(
            gameId = normalizedGameId,
            layoutId = normalizedLayout,
            points = normalizedPoints,
            pitPoints = normalizedPitPoints,
            bounds = normalizedBounds,
            pitEntryIndex = pitEntry,
            pitExitIndex = pitExit,
        )
        return if (sanitizedMap == map) map else sanitizedMap
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

    private fun TrackMap.mergeKey(): String = buildString {
        append(gameId.normalizedGameId())
        append('|')
        append(trackId.trim())
        append('|')
        append(layoutId.normalizedLayoutId())
    }
}

private data class TrackMapLookupRequest(val gameId: String, val trackId: String, val layoutId: String)
