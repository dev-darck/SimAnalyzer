package com.analyzer.session.data.repository

import com.analyzer.session.data.analysis.IndexAnalysis
import com.analyzer.session.data.model.RecordedSessionMetadata
import com.analyzer.session.data.model.RecordedSessionSummary
import com.analyzer.session.data.model.SessionBundleLocation
import com.analyzer.session.data.model.SessionLocation
import com.analyzer.session.data.repository.cache.BundleIndexCache
import com.analyzer.session.data.repository.cache.RootFingerprint
import dev.zacsweers.metro.Inject
import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.utils.logger.logger
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@Inject
internal class RecordedSessionBundleStore(
    private val settings: TelemetryAcquisitionSettings,
    private val json: Json,
    private val bundleAssembler: RecordedSessionBundleAssembler,
    private val indexReader: RecordedSessionIndexReader,
) {

    private val logger = logger()

    suspend fun loadSummaries(forceRefresh: Boolean = false): List<RecordedSessionSummary> = resolveRoot()
        ?.let { root ->
            loadIndex(
                root = root,
                forceRefresh = forceRefresh,
            ).summaries
        }
        .orEmpty()

    suspend fun findBundle(sessionId: Long, forceRefresh: Boolean = false): SessionBundleLocation? {
        val root = resolveRoot() ?: return null
        return loadIndex(
            root = root,
            forceRefresh = forceRefresh,
        ).sessionIndex[sessionId]
    }

    suspend fun saveSession(sessionId: Long): Boolean {
        val bundle = findBundle(sessionId) ?: return false
        if (bundle.summary.isSaved) return true

        val saved = bundle.locations.all { location ->
            if (location.metadata.isSaved) {
                true
            } else {
                val updatedMetadata = location.metadata.copy(isSaved = true)
                writeMetadata(File(location.dir, META_FILE_NAME), updatedMetadata)
            }
        }
        if (saved) {
            refreshIndex()
        }
        return saved
    }

    suspend fun deleteSession(sessionId: Long): Boolean {
        val bundle = findBundle(sessionId) ?: return false
        val deleted = bundle.locations.all { location ->
            runCatching {
                location.dir.deleteRecursively()
            }.getOrElse { error ->
                logger.error(error) { "failed to delete session dir: ${location.dir.absolutePath}" }
                false
            }
        }

        if (deleted) {
            logger.info { "deleted session bundle $sessionId (${bundle.locations.size} dirs)" }
            refreshIndex()
        }
        return deleted
    }

    fun resolveAnalysis(location: SessionLocation): IndexAnalysis? = location.analysis
        ?: indexReader.readAnalysis(location.metadata, location.dir)

    private suspend fun refreshIndex() {
        val root = resolveRoot()
        if (root == null) {
            clearSharedCache()
            return
        }
        loadIndex(
            root = root,
            forceRefresh = true,
        )
    }

    private fun loadIndex(root: File, forceRefresh: Boolean): BundleIndexCache {
        val fingerprint = buildRootFingerprint(root)
        if (!forceRefresh) {
            peekSharedCache(
                rootPath = root.absolutePath,
                fingerprint = fingerprint,
            )?.let { return it }
        }

        val rebuilt = rebuildIndex(
            root = root,
            fingerprint = fingerprint,
        )
        storeSharedCache(rebuilt)
        return rebuilt
    }

    private fun rebuildIndex(root: File, fingerprint: RootFingerprint): BundleIndexCache {
        val locations = loadLocations(root)
        val bundles = bundleAssembler.build(locations)
        val summaries = bundles.map(SessionBundleLocation::summary)
        val sessionIndex = linkedMapOf<Long, SessionBundleLocation>()
        bundles.forEach { bundle ->
            val previous = sessionIndex.put(bundle.summary.sessionId, bundle)
            if (previous != null) {
                logger.warn {
                    "stable session bundle id collision for ${bundle.summary.sessionId}; replacing previous bundle"
                }
            }
        }
        return BundleIndexCache(
            rootPath = root.absolutePath,
            fingerprint = fingerprint,
            summaries = summaries,
            bundles = bundles,
            sessionIndex = sessionIndex,
        )
    }

    private fun loadLocations(root: File): List<SessionLocation> = root.listFiles()
        ?.asSequence()
        ?.filter(File::isDirectory)
        ?.mapNotNull(::readSessionLocation)
        ?.toList()
        .orEmpty()

    private fun readSessionLocation(dir: File): SessionLocation? {
        val metaFile = File(dir, META_FILE_NAME)
        if (!metaFile.exists()) return null

        val metadata = readMetadata(metaFile) ?: return null
        val analysis = indexReader.readAnalysis(metadata, dir)
        val summary = buildSummary(
            metadata = metadata,
            analysis = analysis,
            persistedSessionId = stablePersistedSessionId(metadata, dir),
        )

        return SessionLocation(
            summary = summary,
            dir = dir,
            metadata = metadata,
            analysis = analysis,
        )
    }

    private fun readMetadata(metaFile: File): RecordedSessionMetadata? = runCatching {
        json.decodeFromString(RecordedSessionMetadata.serializer(), metaFile.readText())
    }.onFailure { error ->
        logger.warn(error) { "failed to decode metadata: ${metaFile.absolutePath}" }
    }.getOrNull()

    private fun writeMetadata(metaFile: File, metadata: RecordedSessionMetadata): Boolean = runCatching {
        val encoded = json.encodeToString(RecordedSessionMetadata.serializer(), metadata)
        val tmp = File(metaFile.parentFile, metaFile.name + ".tmp")
        tmp.writeText(encoded)
        if (!tmp.renameTo(metaFile)) {
            metaFile.writeText(encoded)
            tmp.delete()
        }
        true
    }.getOrElse { error ->
        logger.error(error) { "failed to write metadata: ${metaFile.absolutePath}" }
        false
    }

    private fun buildSummary(
        metadata: RecordedSessionMetadata,
        analysis: IndexAnalysis?,
        persistedSessionId: Long,
    ): RecordedSessionSummary {
        val laps = analysis?.laps.orEmpty()
        val completedLaps = laps.count { lap -> lap.complete }
        val bestLapMs = laps
            .asSequence()
            .filter { lap -> lap.complete && !lap.invalid && !lap.inPit }
            .mapNotNull { lap -> lap.totalTimeMs }
            .minOrNull()
        val incidents = laps.count { lap -> lap.invalid }

        return RecordedSessionSummary(
            sessionId = persistedSessionId,
            startedAtMs = metadata.startedAtMs,
            endedAtMs = metadata.endedAtMs,
            gameId = metadata.gameId,
            sessionType = metadata.sessionType,
            carModel = metadata.carModel,
            carName = metadata.carName,
            carId = metadata.carId,
            trackId = metadata.trackId,
            trackName = metadata.trackName,
            layoutId = metadata.layoutId,
            lapCount = completedLaps,
            bestLapTimeMs = bestLapMs,
            totalIncidents = incidents,
            distanceKm = analysis?.distanceKm ?: 0.0,
            isSaved = metadata.isSaved,
            airTempC = metadata.airTempC,
            trackTempC = metadata.trackTempC,
        )
    }

    private suspend fun resolveRoot(): File? {
        val path = runCatching { settings.currentConfig().storageLocation }
            .getOrNull()
            .orEmpty()
            .trim()
        if (path.isBlank()) return null

        val dir = File(path)
        return dir.takeIf { it.exists() && it.isDirectory }
    }

    private fun stablePersistedSessionId(metadata: RecordedSessionMetadata, dir: File): Long {
        val source = buildString(96) {
            append(normalizeGameId(metadata.gameId))
            append('|')
            append(metadata.startedAtMs)
            append('|')
            append(metadata.sessionId)
            append('|')
            append(dir.absolutePath)
        }
        val hash = fnv1a64(source)
        return (hash and Long.MAX_VALUE).let { if (it == 0L) 1L else it }
    }

    private fun normalizeGameId(gameId: String?): String = gameId.orEmpty().trim().lowercase()

    private fun buildRootFingerprint(root: File): RootFingerprint {
        var directoryCount = 0
        var directoryHash = FNV1A_64_OFFSET
        root.listFiles()
            ?.asSequence()
            ?.filter(File::isDirectory)
            ?.sortedBy { dir -> dir.name }
            ?.forEach { dir ->
                directoryCount += 1
                directoryHash = directoryHash.fnv1aAppend(dir.name)
                directoryHash = directoryHash.fnv1aAppend(dir.lastModified())
            }
        return RootFingerprint(
            rootLastModified = root.lastModified(),
            directoryCount = directoryCount,
            directoryHash = directoryHash,
        )
    }

    private fun Long.fnv1aAppend(value: String): Long {
        var hash = this
        value.forEach { ch ->
            hash = hash xor ch.code.toLong()
            hash *= FNV1A_64_PRIME
        }
        return hash
    }

    private fun Long.fnv1aAppend(value: Long): Long {
        var hash = this
        repeat(Long.SIZE_BYTES) { byteIndex ->
            val shift = byteIndex * Byte.SIZE_BITS
            hash = hash xor ((value ushr shift) and 0xFFL)
            hash *= FNV1A_64_PRIME
        }
        return hash
    }

    private fun clearSharedCache() {
        sharedCache.set(null)
    }

    private fun peekSharedCache(rootPath: String, fingerprint: RootFingerprint): BundleIndexCache? = sharedCache.get()
        ?.takeIf { cache ->
            cache.rootPath == rootPath && cache.fingerprint == fingerprint
        }

    private fun storeSharedCache(cache: BundleIndexCache) {
        sharedCache.set(cache)
    }

    private fun fnv1a64(value: String): Long {
        var hash = FNV1A_64_OFFSET
        value.forEach { ch ->
            hash = hash xor ch.code.toLong()
            hash *= FNV1A_64_PRIME
        }
        return hash
    }

    private companion object {

        private const val FNV1A_64_OFFSET = -0x340d631b7bdddcdbL
        private const val FNV1A_64_PRIME = 0x100000001b3L

        private val sharedCache = AtomicReference<BundleIndexCache?>()
    }
}
