package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.AcEvoImportCandidate
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.AcEvoImportedAsset
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.AcEvoImportedAssetKind
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.AcEvoImportedContentManifest
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.AcEvoImportedContentSnapshot
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.AcEvoImportedTrackMapCacheKey
import com.project.analyzer.api.di.IO
import com.project.analyzer.utils.AppDirectories
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

@SingleIn(AppScope::class)
@Inject
internal class AcEvoTrackAssetImporter internal constructor(
    private val appDirectories: AppDirectories,
    private val locator: AcEvoContentPackageLocator,
    private val kspkgReader: AcEvoKspkgReader,
    private val json: Json,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) {

    private val logger = logger()
    private val mutex = Mutex()

    @Volatile
    private var cachedKey: AcEvoImportedTrackMapCacheKey? = null

    @Volatile
    private var cachedSnapshot: AcEvoImportedContentSnapshot? = null

    suspend fun ensureImported(): AcEvoImportedContentSnapshot? = withContext(ioDispatcher) {
        mutex.withLock {
            val packagePath = locator.locate() ?: return@withLock null
            val packageSizeBytes = runCatching { Files.size(packagePath) }.getOrDefault(0L)
            val packageLastModifiedEpochMs = runCatching {
                Files.getLastModifiedTime(packagePath).toMillis()
            }.getOrDefault(0L)
            val packageCacheKey = AcEvoImportedTrackMapCacheKey(
                packagePath = packagePath.toAbsolutePath().normalize().toString(),
                packageSizeBytes = packageSizeBytes,
                packageLastModifiedEpochMs = packageLastModifiedEpochMs,
                assetCount = -1,
            )

            cachedSnapshot
                ?.takeIf { snapshot ->
                    cachedKey?.isSamePackage(packageCacheKey) == true && Files.isDirectory(snapshot.assetsRoot)
                }
                ?.let { snapshot ->
                    return@withLock snapshot
                }

            val importRoot = appDirectories.userDataDir.toPath().resolve(IMPORT_ROOT_DIR_NAME)
            val assetsRoot = importRoot.resolve(ASSETS_DIR_NAME)
            val manifestPath = importRoot.resolve(MANIFEST_FILE_NAME)

            readManifest(manifestPath)?.takeIf { manifest ->
                manifest.isCurrentFor(
                    packagePath = packagePath,
                    packageSizeBytes = packageSizeBytes,
                    packageLastModifiedEpochMs = packageLastModifiedEpochMs,
                    assetsRoot = assetsRoot,
                )
            }?.let { manifest ->
                val snapshot = AcEvoImportedContentSnapshot(
                    assetsRoot = assetsRoot,
                    manifest = manifest,
                )
                cachedKey = manifest.toCacheKey()
                cachedSnapshot = snapshot
                return@withLock snapshot
            }

            val archive = kspkgReader.open(packagePath)
            val importCandidates = archive.entries.mapNotNull(::classifyImportCandidate)
            if (importCandidates.isEmpty()) return@withLock null

            deleteRecursively(importRoot)
            Files.createDirectories(assetsRoot)

            importCandidates.forEach { candidate ->
                val targetPath = resolveRelativePath(assetsRoot, candidate.asset.relativePath)
                Files.createDirectories(targetPath.parent)
                Files.write(targetPath, archive.extract(candidate.entry))
            }

            val manifest = AcEvoImportedContentManifest(
                importerVersion = IMPORTER_VERSION,
                packagePath = packagePath.toAbsolutePath().normalize().toString(),
                packageSizeBytes = packageSizeBytes,
                packageLastModifiedEpochMs = packageLastModifiedEpochMs,
                importedAtEpochMs = System.currentTimeMillis(),
                assets = importCandidates.map { it.asset }.sortedBy { it.relativePath },
            )

            Files.createDirectories(importRoot)
            Files.writeString(
                manifestPath,
                json.encodeToString(AcEvoImportedContentManifest.serializer(), manifest),
            )

            logger.info {
                "Imported ${manifest.assets.size} AC EVO assets from " +
                    "${packagePath.toAbsolutePath()} to ${assetsRoot.toAbsolutePath()}"
            }

            AcEvoImportedContentSnapshot(
                assetsRoot = assetsRoot,
                manifest = manifest,
            ).also { snapshot ->
                cachedKey = manifest.toCacheKey()
                cachedSnapshot = snapshot
            }
        }
    }

    private fun readManifest(manifestPath: Path): AcEvoImportedContentManifest? {
        if (!Files.isRegularFile(manifestPath)) return null

        return runCatching {
            json.decodeFromString(
                AcEvoImportedContentManifest.serializer(),
                Files.readString(manifestPath),
            )
        }.getOrElse {
            logger.warn(it) { "Failed to read AC EVO content import manifest: ${manifestPath.toAbsolutePath()}" }
            null
        }
    }

    private fun classifyImportCandidate(entry: AcEvoKspkgEntry): AcEvoImportCandidate? {
        val normalizedPath = entry.relativePath.replace('\\', '/').trim()
        val asset = classifyTrackAsset(normalizedPath) ?: classifySvgAsset(normalizedPath) ?: return null
        return AcEvoImportCandidate(entry = entry.copy(relativePath = normalizedPath), asset = asset)
    }

    private fun classifyTrackAsset(relativePath: String): AcEvoImportedAsset? {
        val segments = relativePath.split('/').filter(String::isNotBlank)
        if (segments.size < 5) return null
        if (!segments[0].equals("content", ignoreCase = true) || !segments[1].equals("tracks", ignoreCase = true)) {
            return null
        }

        val trackFolder = segments[2].trim().takeIf { it.isNotBlank() } ?: return null
        val fileName = segments.last()
        val assetKindAndLayout = when {
            fileName.endsWith(SPLINEDATA_SUFFIX, ignoreCase = true) -> {
                AcEvoImportedAssetKind.SPLINEDATA_JSON to fileName.removeSuffixIgnoreCase(SPLINEDATA_SUFFIX)
            }

            fileName.endsWith(IDEAL_LINE_SUFFIX, ignoreCase = true) -> {
                AcEvoImportedAssetKind.IDEAL_LINE_AI to fileName.removeSuffixIgnoreCase(IDEAL_LINE_SUFFIX)
            }

            fileName.endsWith(PITLANE_SUFFIX, ignoreCase = true) -> {
                AcEvoImportedAssetKind.PITLANE_AI to fileName.removeSuffixIgnoreCase(PITLANE_SUFFIX)
            }

            fileName.endsWith(TRACK_CONTROL_POINTS_SUFFIX, ignoreCase = true) -> {
                AcEvoImportedAssetKind.TRACK_CONTROL_POINTS to fileName.removeSuffixIgnoreCase(
                    TRACK_CONTROL_POINTS_SUFFIX,
                )
            }

            else -> null
        } ?: return null

        val layoutId = normalizeRuntimeLayoutId(assetKindAndLayout.second)
        val trackId = TrackIdNormalizer.normalize(trackFolder, layoutId).takeIf { it.isNotBlank() } ?: return null

        return AcEvoImportedAsset(
            relativePath = relativePath,
            kind = assetKindAndLayout.first,
            trackFolder = trackFolder,
            layoutId = layoutId,
            trackId = trackId,
        )
    }

    private fun classifySvgAsset(relativePath: String): AcEvoImportedAsset? {
        if (!relativePath.startsWith(SVG_DIR_PREFIX, ignoreCase = true) ||
            !relativePath.endsWith(SVG_SUFFIX, ignoreCase = true)
        ) {
            return null
        }

        val fileName = relativePath.substringAfterLast('/')
        val stem = fileName.removeSuffixIgnoreCase(SVG_SUFFIX)
        val hyphenIndex = stem.lastIndexOf('-')
        val trackFolder = stem.substringBeforeLast('-', missingDelimiterValue = stem)
            .trim()
            .takeIf { it.isNotBlank() }

        val layoutId = if (hyphenIndex > 0 && hyphenIndex < stem.lastIndex) {
            normalizeRuntimeLayoutId(stem.substring(hyphenIndex + 1))
        } else {
            null
        }

        val trackId = trackFolder?.let { TrackIdNormalizer.normalize(it, layoutId).takeIf(String::isNotBlank) }

        return AcEvoImportedAsset(
            relativePath = relativePath,
            kind = AcEvoImportedAssetKind.TRACKMAP_SVG,
            trackFolder = trackFolder,
            layoutId = layoutId,
            trackId = trackId,
        )
    }

    private fun normalizeRuntimeLayoutId(raw: String?): String? {
        val normalized = raw
            ?.trim()
            ?.lowercase(Locale.US)
            ?.substringAfterLast('/')
            ?.substringAfterLast('\\')
            ?.removePrefix("layout_")
            ?.trim('_')
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return TrackIdNormalizer.normalizeLayoutId(normalized) ?: normalized
    }

    private fun resolveRelativePath(root: Path, relativePath: String): Path {
        var current = root
        relativePath.split('/').filter(String::isNotBlank).forEach { segment ->
            current = current.resolve(segment)
        }
        val normalized = current.normalize()
        check(normalized.startsWith(root.normalize())) {
            "Refusing to resolve path outside import root: $relativePath"
        }
        return normalized
    }

    private fun deleteRecursively(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { path ->
                Files.deleteIfExists(path)
            }
        }
    }

    private fun String.removeSuffixIgnoreCase(suffix: String): String = if (endsWith(suffix, ignoreCase = true)) {
        dropLast(suffix.length)
    } else {
        this
    }

    internal companion object {

        const val IMPORTER_VERSION: Int = 4
        const val IMPORT_ROOT_DIR_NAME = "ac-evo-content"
        const val ASSETS_DIR_NAME = "assets"
        const val MANIFEST_FILE_NAME = "manifest.json"

        const val SPLINEDATA_SUFFIX = ".splinedata.json"
        const val IDEAL_LINE_SUFFIX = ".ideal_line.aisplinedata"
        const val PITLANE_SUFFIX = ".pitlane.aisplinedata"
        const val TRACK_CONTROL_POINTS_SUFFIX = ".trackcontrolpoints"
        const val SVG_SUFFIX = ".svg"
        const val SVG_DIR_PREFIX = "uiresources/images/trackmaps/"
    }
}

private fun AcEvoImportedTrackMapCacheKey.isSamePackage(other: AcEvoImportedTrackMapCacheKey): Boolean =
    packagePath == other.packagePath &&
        packageSizeBytes == other.packageSizeBytes &&
        packageLastModifiedEpochMs == other.packageLastModifiedEpochMs

private fun AcEvoImportedContentManifest.toCacheKey(): AcEvoImportedTrackMapCacheKey = AcEvoImportedTrackMapCacheKey(
    packagePath = packagePath,
    packageSizeBytes = packageSizeBytes,
    packageLastModifiedEpochMs = packageLastModifiedEpochMs,
    assetCount = assets.size,
)
