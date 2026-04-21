package com.project.analyzer.telemetry.recording.impl.reader.storage

import com.project.analyzer.telemetry.recording.api.acquisition.TelemetryAcquisitionSettings
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_COMPRESSION_GZIP
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_FRAMES_FILE_NAME
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_INDEX_FILE_NAME
import com.project.analyzer.telemetry.recording.api.file.TELEMETRY_SESSION_META_FILE_NAME
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionBundle
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionLocation
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import com.project.analyzer.telemetry.recording.impl.reader.assembler.RecordedTelemetrySessionBundleAssembler
import com.project.analyzer.telemetry.recording.impl.reader.model.RecordedTelemetryBundleIndex
import com.project.analyzer.telemetry.recording.impl.reader.model.RecordedTelemetryRootFingerprint
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.serialization.json.Json
import java.io.File
import java.util.concurrent.atomic.AtomicReference

@Inject
@SingleIn(AppScope::class)
internal class RecordedTelemetrySessionStorageImpl(
    private val settings: TelemetryAcquisitionSettings,
    private val json: Json,
    private val bundleAssembler: RecordedTelemetrySessionBundleAssembler,
) : RecordedTelemetrySessionStorage {

    private val logger = logger()

    override suspend fun loadBundles(forceRefresh: Boolean): List<RecordedTelemetrySessionBundle> {
        val root = resolveRoot() ?: return emptyList()
        return loadIndex(
            root = root,
            forceRefresh = forceRefresh,
        ).bundles
    }

    override suspend fun findBundle(sessionId: Long, forceRefresh: Boolean): RecordedTelemetrySessionBundle? {
        val root = resolveRoot() ?: return null
        return loadIndex(
            root = root,
            forceRefresh = forceRefresh,
        ).sessionIndex[sessionId]
    }

    override suspend fun saveSession(sessionId: Long): Boolean {
        val bundle = findBundle(sessionId) ?: return false
        if (bundle.locations.all { it.metadata.isSaved }) return true

        val saved = bundle.locations.all { location ->
            if (location.metadata.isSaved) {
                true
            } else {
                writeMetadata(
                    metaFile = File(location.dir, TELEMETRY_SESSION_META_FILE_NAME),
                    metadata = location.metadata.copy(isSaved = true),
                )
            }
        }
        if (saved) {
            refreshIndex()
        }
        return saved
    }

    override suspend fun deleteSession(sessionId: Long): Boolean {
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
            refreshIndex()
        }
        return deleted
    }

    override fun resolveFramesFile(location: RecordedTelemetrySessionLocation): File? = candidateFiles(
        preferredName = location.metadata.framesFile,
        fallbackName = TELEMETRY_SESSION_FRAMES_FILE_NAME,
        location = location,
    ).firstOrNull(File::exists)

    override fun resolveIndexFile(location: RecordedTelemetrySessionLocation): File? = candidateFiles(
        preferredName = location.metadata.indexFile,
        fallbackName = TELEMETRY_SESSION_INDEX_FILE_NAME,
        location = location,
    ).firstOrNull(File::exists)

    private suspend fun refreshIndex() {
        val root = resolveRoot()
        if (root == null) {
            sharedCache.set(null)
            return
        }
        loadIndex(root = root, forceRefresh = true)
    }

    private fun loadIndex(root: File, forceRefresh: Boolean): RecordedTelemetryBundleIndex {
        val fingerprint = buildRootFingerprint(root)
        if (!forceRefresh) {
            sharedCache.get()
                ?.takeIf { cache -> cache.rootPath == root.absolutePath && cache.fingerprint == fingerprint }
                ?.let { return it }
        }

        val locations = loadLocations(root)
        val bundles = bundleAssembler.build(locations)
        val rebuilt = RecordedTelemetryBundleIndex(
            rootPath = root.absolutePath,
            fingerprint = fingerprint,
            bundles = bundles,
            sessionIndex = bundles.associateBy(RecordedTelemetrySessionBundle::sessionId),
        )
        sharedCache.set(rebuilt)
        return rebuilt
    }

    private fun loadLocations(root: File): List<RecordedTelemetrySessionLocation> = root.listFiles()
        ?.asSequence()
        ?.filter(File::isDirectory)
        ?.mapNotNull(::readLocation)
        ?.toList()
        .orEmpty()

    private fun readLocation(dir: File): RecordedTelemetrySessionLocation? {
        val metaFile = File(dir, TELEMETRY_SESSION_META_FILE_NAME)
        if (!metaFile.exists()) return null

        val metadata = runCatching {
            json.decodeFromString(RecordedTelemetrySessionMetadata.serializer(), metaFile.readText())
        }.onFailure { error ->
            logger.warn(error) { "failed to decode metadata: ${metaFile.absolutePath}" }
        }.getOrNull() ?: return null

        return RecordedTelemetrySessionLocation(
            persistedSessionId = stablePersistedSessionId(metadata, dir),
            dir = dir,
            metadata = metadata,
        )
    }

    private fun writeMetadata(metaFile: File, metadata: RecordedTelemetrySessionMetadata): Boolean = runCatching {
        val encoded = json.encodeToString(RecordedTelemetrySessionMetadata.serializer(), metadata)
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

    private suspend fun resolveRoot(): File? {
        val path = runCatching { settings.currentConfig().storageLocation }
            .getOrNull()
            .orEmpty()
            .trim()
        if (path.isBlank()) return null

        val root = File(path)
        return root.takeIf { it.exists() && it.isDirectory }
    }

    private fun buildRootFingerprint(root: File): RecordedTelemetryRootFingerprint {
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
        return RecordedTelemetryRootFingerprint(
            rootLastModified = root.lastModified(),
            directoryCount = directoryCount,
            directoryHash = directoryHash,
        )
    }

    private fun stablePersistedSessionId(metadata: RecordedTelemetrySessionMetadata, dir: File): Long {
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

    private fun candidateFiles(
        preferredName: String,
        fallbackName: String,
        location: RecordedTelemetrySessionLocation,
    ): List<File> {
        val names = linkedSetOf(preferredName, fallbackName)
        if (location.metadata.compression.equals(TELEMETRY_SESSION_COMPRESSION_GZIP, ignoreCase = true)) {
            if (!preferredName.endsWith(".gz", ignoreCase = true)) names += "$preferredName.gz"
            if (!fallbackName.endsWith(".gz", ignoreCase = true)) names += "$fallbackName.gz"
        }
        return names.map { name -> File(location.dir, name) }
    }

    private fun normalizeGameId(gameId: String?): String = gameId.orEmpty().trim().lowercase()

    private fun fnv1a64(value: String): Long {
        var hash = FNV1A_64_OFFSET
        value.forEach { ch ->
            hash = hash xor ch.code.toLong()
            hash *= FNV1A_64_PRIME
        }
        return hash
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

    private companion object {

        private const val FNV1A_64_OFFSET = -0x340d631b7bdddcdbL
        private const val FNV1A_64_PRIME = 0x100000001b3L

        private val sharedCache = AtomicReference<RecordedTelemetryBundleIndex?>()
    }
}
