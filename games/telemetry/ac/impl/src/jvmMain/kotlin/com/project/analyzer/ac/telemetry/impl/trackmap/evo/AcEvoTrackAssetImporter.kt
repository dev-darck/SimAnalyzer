package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
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
import kotlinx.serialization.Serializable
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

    suspend fun ensureImported(): AcEvoImportedContentSnapshot? = withContext(ioDispatcher) {
        mutex.withLock {
            val packagePath = locator.locate() ?: return@withLock null
            val packageSizeBytes = runCatching { Files.size(packagePath) }.getOrDefault(0L)
            val packageLastModifiedEpochMs = runCatching {
                Files.getLastModifiedTime(packagePath).toMillis()
            }.getOrDefault(0L)

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
                return@withLock AcEvoImportedContentSnapshot(
                    assetsRoot = assetsRoot,
                    manifest = manifest,
                )
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
            )
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

            fileName.endsWith(TRACK_LAYOUT_SUFFIX, ignoreCase = true) -> {
                AcEvoImportedAssetKind.TRACK_LAYOUT to fileName.removeSuffixIgnoreCase(TRACK_LAYOUT_SUFFIX)
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
        return when (normalized) {
            "gp_circuit" -> "gp"
            "gp_circuit_shortcut", "gp_circuit_short", "gp_shortcut", "short", "shortcut" -> "gp_short"
            "full_course", "full" -> "gp"
            "national_circuit" -> "national"
            "international_circuit" -> "international"
            "24_hr", "24_hour", "24hours", "24_hour_layout" -> "24h"
            else -> normalized
        }
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

        const val IMPORTER_VERSION: Int = 1
        const val IMPORT_ROOT_DIR_NAME = "ac-evo-content"
        const val ASSETS_DIR_NAME = "assets"
        const val MANIFEST_FILE_NAME = "manifest.json"

        const val SPLINEDATA_SUFFIX = ".splinedata.json"
        const val IDEAL_LINE_SUFFIX = ".ideal_line.aisplinedata"
        const val PITLANE_SUFFIX = ".pitlane.aisplinedata"
        const val TRACK_CONTROL_POINTS_SUFFIX = ".trackcontrolpoints"
        const val TRACK_LAYOUT_SUFFIX = ".track_layout"
        const val SVG_SUFFIX = ".svg"
        const val SVG_DIR_PREFIX = "uiresources/images/trackmaps/"
    }
}

internal data class AcEvoImportedContentSnapshot(val assetsRoot: Path, val manifest: AcEvoImportedContentManifest)

@Serializable
internal data class AcEvoImportedContentManifest(
    val importerVersion: Int,
    val packagePath: String,
    val packageSizeBytes: Long,
    val packageLastModifiedEpochMs: Long,
    val importedAtEpochMs: Long,
    val assets: List<AcEvoImportedAsset>,
) {

    fun isCurrentFor(
        packagePath: Path,
        packageSizeBytes: Long,
        packageLastModifiedEpochMs: Long,
        assetsRoot: Path,
    ): Boolean {
        if (importerVersion != AcEvoTrackAssetImporter.IMPORTER_VERSION) return false
        if (this.packagePath != packagePath.toAbsolutePath().normalize().toString()) return false
        if (this.packageSizeBytes != packageSizeBytes) return false
        if (this.packageLastModifiedEpochMs != packageLastModifiedEpochMs) return false
        if (!Files.isDirectory(assetsRoot)) return false
        return assets.all { asset -> Files.isRegularFile(resolveRelativePath(assetsRoot, asset.relativePath)) }
    }

    private fun resolveRelativePath(root: Path, relativePath: String): Path {
        var current = root
        relativePath.split('/').filter(String::isNotBlank).forEach { segment ->
            current = current.resolve(segment)
        }
        return current.normalize()
    }
}

@Serializable
internal data class AcEvoImportedAsset(
    val relativePath: String,
    val kind: AcEvoImportedAssetKind,
    val trackFolder: String? = null,
    val layoutId: String? = null,
    val trackId: String? = null,
)

@Serializable
internal enum class AcEvoImportedAssetKind {

    SPLINEDATA_JSON,
    IDEAL_LINE_AI,
    PITLANE_AI,
    TRACK_CONTROL_POINTS,
    TRACK_LAYOUT,
    TRACKMAP_SVG,
}

private data class AcEvoImportCandidate(val entry: AcEvoKspkgEntry, val asset: AcEvoImportedAsset)
