package com.project.analyzer.ac.telemetry.impl.fallback

import com.project.analyzer.ac.telemetry.impl.calibration.TrackCalibrationFileNameResolver
import com.project.analyzer.api.di.IO
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

@Inject
@SingleIn(AppScope::class)
class TrackCalibrationLoader(
    private val repository: TrackCalibrationRepository,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val cacheMutex = Mutex()
    private val cache = ConcurrentHashMap<String, CachedCalibration>()
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<TrackCalibration?>>()

    suspend fun load(trackId: String, layoutId: String? = null): TrackCalibration? {
        val cacheKey = buildCacheKey(trackId = trackId, layoutId = layoutId)
        return when (val lookup = acquireLookup(cacheKey)) {
            is CacheLookup.Cached -> lookup.calibration

            is CacheLookup.AwaitExisting -> lookup.deferred.await()

            is CacheLookup.OwnLoad -> loadAndCache(
                cacheKey = cacheKey,
                deferred = lookup.deferred,
                trackId = trackId,
                layoutId = layoutId,
            )
        }
    }

    fun peek(trackId: String, layoutId: String? = null): TrackCalibration? {
        val cacheKey = buildCacheKey(trackId = trackId, layoutId = layoutId)
        return cache[cacheKey]?.calibration
    }

    fun isCached(trackId: String, layoutId: String? = null): Boolean {
        val cacheKey = buildCacheKey(trackId = trackId, layoutId = layoutId)
        return cache.containsKey(cacheKey)
    }

    suspend fun cache(calibration: TrackCalibration) {
        val cacheKeys = buildCacheKeys(
            trackId = calibration.trackId,
            layoutId = calibration.layoutId,
        )
        cacheMutex.withLock {
            cacheKeys.forEach { cacheKey ->
                cache[cacheKey] = CachedCalibration(calibration)
                inFlight.remove(cacheKey)?.complete(calibration)
            }
        }
    }

    private suspend fun acquireLookup(cacheKey: String): CacheLookup = cacheMutex.withLock {
        cache[cacheKey]?.let { return CacheLookup.Cached(it.calibration) }
        inFlight[cacheKey]?.let { return CacheLookup.AwaitExisting(it) }
        val deferred = CompletableDeferred<TrackCalibration?>()
        inFlight[cacheKey] = deferred
        CacheLookup.OwnLoad(deferred)
    }

    private suspend fun loadAndCache(
        cacheKey: String,
        deferred: CompletableDeferred<TrackCalibration?>,
        trackId: String,
        layoutId: String?,
    ): TrackCalibration? = try {
        val calibration = withContext(ioDispatcher) {
            repository.load(trackId = trackId, layoutId = layoutId)
        }
        completeOwnedLoad(cacheKey = cacheKey, deferred = deferred, calibration = calibration)
        calibration
    } catch (error: Throwable) {
        failOwnedLoad(cacheKey = cacheKey, deferred = deferred, error = error)
        throw error
    }

    private suspend fun completeOwnedLoad(
        cacheKey: String,
        deferred: CompletableDeferred<TrackCalibration?>,
        calibration: TrackCalibration?,
    ) {
        cacheMutex.withLock {
            if (inFlight[cacheKey] === deferred) {
                inFlight.remove(cacheKey)
                cache[cacheKey] = CachedCalibration(calibration)
            }
        }
        deferred.complete(calibration)
    }

    private suspend fun failOwnedLoad(
        cacheKey: String,
        deferred: CompletableDeferred<TrackCalibration?>,
        error: Throwable,
    ) {
        cacheMutex.withLock {
            if (inFlight[cacheKey] === deferred) {
                inFlight.remove(cacheKey)
            }
        }
        deferred.completeExceptionally(error)
    }

    private fun buildCacheKey(trackId: String, layoutId: String?): String =
        TrackCalibrationFileNameResolver.buildCacheKey(
            trackId = trackId,
            layoutId = layoutId,
        )

    private fun buildCacheKeys(trackId: String, layoutId: String?): Set<String> {
        val normalizedTrackId = trackId.trim().takeIf { it.isNotBlank() } ?: return emptySet()
        val resolvedTrackId = TrackCalibrationFileNameResolver.resolveStorageTrackId(
            trackId = normalizedTrackId,
            layoutId = layoutId,
        ).ifBlank { normalizedTrackId }
        return linkedSetOf(
            TrackCalibrationFileNameResolver.buildCacheKey(
                trackId = normalizedTrackId,
                layoutId = layoutId,
            ),
            TrackCalibrationFileNameResolver.buildCacheKey(
                trackId = resolvedTrackId,
                layoutId = null,
            ),
        )
    }
}

private data class CachedCalibration(val calibration: TrackCalibration?)

private sealed interface CacheLookup {
    data class Cached(val calibration: TrackCalibration?) : CacheLookup
    data class AwaitExisting(val deferred: CompletableDeferred<TrackCalibration?>) : CacheLookup
    data class OwnLoad(val deferred: CompletableDeferred<TrackCalibration?>) : CacheLookup
}
