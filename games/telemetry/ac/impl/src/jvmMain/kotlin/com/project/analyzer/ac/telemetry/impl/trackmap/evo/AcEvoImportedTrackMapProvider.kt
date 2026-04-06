package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.ac.telemetry.impl.trackmap.TrackMapGameProvider
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.AcEvoImportedAssetKind
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.AcEvoImportedContentSnapshot
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.CacheKey
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.TrackAssetBundle
import com.project.analyzer.ac.telemetry.impl.trackmap.evo.model.TrackAssetBundleBuilder
import com.project.analyzer.game.api.AC_KEY
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TrackMapGameProvider>())
@Suppress("unused")
class AcEvoImportedTrackMapProvider internal constructor(
    private val importer: AcEvoTrackAssetImporter,
    private val parsers: AcEvoTrackMapAssetParsers,
    private val centerLineResolver: AcEvoTrackCenterLineResolver,
) : TrackMapGameProvider {

    private val mutex = Mutex()

    @Volatile
    private var cachedKey: CacheKey? = null

    @Volatile
    private var cachedMaps: List<TrackMap> = emptyList()

    @Volatile
    private var cachedMapsByBundleKey: Map<String, TrackMap> = emptyMap()

    override suspend fun load(gameId: String, trackId: String, layoutId: String?): TrackMap? {
        if (!isAceGame(gameId)) return null
        val normalizedTrackId = trackId.trim().lowercase(Locale.US).takeIf { it.isNotBlank() } ?: return null
        val normalizedLayoutId = layoutId.normalizedLayoutId()

        return mutex.withLock {
            val snapshot = importer.ensureImported() ?: return@withLock null

            invalidateCacheIfNeeded(snapshot.toCacheKey())

            val bundles = buildBundles(snapshot = snapshot, trackIdFilter = normalizedTrackId)
            val candidateBundles = selectCandidateBundles(
                bundles = bundles.values.map(TrackAssetBundleBuilder::toImmutable),
                normalizedLayoutId = normalizedLayoutId,
            )

            if (candidateBundles.isEmpty()) return@withLock null

            candidateBundles.firstNotNullOfOrNull { bundle ->
                val bundleKey = trackBundleKey(trackId = bundle.trackId, layoutId = bundle.layoutId)

                cachedMapsByBundleKey[bundleKey]
                    ?: buildTrackMap(
                        bundle = bundle,
                        createdAtEpochMs = snapshot.manifest.packageLastModifiedEpochMs,
                    )?.also { builtMap ->
                        cachedMapsByBundleKey = cachedMapsByBundleKey + (bundleKey to builtMap)
                    }
            }
        }
    }

    override suspend fun loadAll(gameId: String?): List<TrackMap> {
        if (gameId != null && !isAceGame(gameId)) return emptyList()
        return ensureMaps()
    }

    private suspend fun ensureMaps(): List<TrackMap> = mutex.withLock {
        val snapshot = importer.ensureImported() ?: return emptyList()
        invalidateCacheIfNeeded(snapshot.toCacheKey())
        if (cachedMaps.isNotEmpty()) {
            return cachedMaps
        }

        val bundles = buildBundles(snapshot = snapshot)

        val builtMaps = bundles.values.mapNotNull { bundle ->
            val immutableBundle = bundle.toImmutable()
            val bundleKey = trackBundleKey(trackId = immutableBundle.trackId, layoutId = immutableBundle.layoutId)
            cachedMapsByBundleKey[bundleKey]
                ?: buildTrackMap(
                    bundle = immutableBundle,
                    createdAtEpochMs = snapshot.manifest.packageLastModifiedEpochMs,
                )?.also { builtMap ->
                    cachedMapsByBundleKey = cachedMapsByBundleKey + (bundleKey to builtMap)
                }
        }
            .sortedBy { map -> trackBundleKey(trackId = map.trackId, layoutId = map.layoutId) }

        cachedMaps = builtMaps
        builtMaps
    }

    private fun buildBundles(
        snapshot: AcEvoImportedContentSnapshot,
        trackIdFilter: String? = null,
    ): LinkedHashMap<String, TrackAssetBundleBuilder> {
        val bundles = linkedMapOf<String, TrackAssetBundleBuilder>()
        snapshot.manifest.assets.forEach { asset ->
            val trackId = asset.trackId?.trim()?.takeIf { it.isNotBlank() } ?: return@forEach
            if (trackIdFilter != null && trackId != trackIdFilter) return@forEach
            val bundleKey = trackBundleKey(trackId = trackId, layoutId = asset.layoutId)
            val builder = bundles.getOrPut(bundleKey) {
                TrackAssetBundleBuilder(
                    trackId = trackId,
                    trackFolder = asset.trackFolder.orEmpty(),
                    layoutId = asset.layoutId?.takeIf { it.isNotBlank() },
                )
            }
            val assetPath = resolveRelativePath(snapshot.assetsRoot, asset.relativePath)
            when (asset.kind) {
                AcEvoImportedAssetKind.SPLINEDATA_JSON -> builder.splineJsonPath = assetPath
                AcEvoImportedAssetKind.IDEAL_LINE_AI -> builder.idealLinePath = assetPath
                AcEvoImportedAssetKind.PITLANE_AI -> builder.pitlanePath = assetPath
                AcEvoImportedAssetKind.TRACK_CONTROL_POINTS -> builder.controlPointsPath = assetPath
                AcEvoImportedAssetKind.TRACK_LAYOUT -> builder.trackLayoutPath = assetPath
                AcEvoImportedAssetKind.TRACKMAP_SVG -> builder.trackMapSvgPath = assetPath
            }
        }
        return bundles
    }

    private fun selectCandidateBundles(
        bundles: List<TrackAssetBundle>,
        normalizedLayoutId: String,
    ): List<TrackAssetBundle> {
        if (bundles.isEmpty()) return emptyList()
        val sortedBundles = bundles.sortedBy { bundle ->
            trackBundleKey(trackId = bundle.trackId, layoutId = bundle.layoutId)
        }
        if (normalizedLayoutId.isNotEmpty()) {
            val exactBundle = sortedBundles.firstOrNull { it.layoutId.normalizedLayoutId() == normalizedLayoutId }
            if (exactBundle != null) return listOf(exactBundle)
            return listOfNotNull(
                sortedBundles.firstOrNull { it.layoutId.normalizedLayoutId().isEmpty() },
            )
        }
        return listOfNotNull(
            sortedBundles.firstOrNull { it.layoutId.normalizedLayoutId().isEmpty() }
                ?: sortedBundles.firstOrNull(),
        )
    }

    private fun invalidateCacheIfNeeded(key: CacheKey) {
        if (cachedKey == key) return
        cachedKey = key
        cachedMaps = emptyList()
        cachedMapsByBundleKey = emptyMap()
    }

    private fun buildTrackMap(bundle: TrackAssetBundle, createdAtEpochMs: Long): TrackMap? {
        val controlPoints = bundle.controlPointsPath?.let(parsers::parseTrackControlPoints).orEmpty()
        val idealLine = bundle.idealLinePath?.let(parsers::parseAiSpline).orEmpty()
        val splineCenterLine = bundle.splineJsonPath?.let(parsers::parseSplineJson).orEmpty()
        val centerline = centerLineResolver.resolve(
            splineSamples = splineCenterLine,
            controlPoints = controlPoints,
            idealLine = idealLine,
        )
        if (centerline.size < 2) return null

        val pitPoints = bundle.pitlanePath?.let(parsers::parseAiSpline).orEmpty()
        val resolvedPoints = if (controlPoints.isEmpty()) {
            centerline.map { point ->
                TrackMapPoint(
                    x = point.x,
                    y = point.y,
                    leftWidthMeters = point.leftWidthMeters,
                    rightWidthMeters = point.rightWidthMeters,
                )
            }
        } else {
            centerline.map { point ->
                val nearestControlPoint = nearestControlPoint(point = point, controlPoints = controlPoints)
                TrackMapPoint(
                    x = point.x,
                    y = point.y,
                    leftWidthMeters = nearestControlPoint?.leftWidthMeters ?: point.leftWidthMeters,
                    rightWidthMeters = nearestControlPoint?.rightWidthMeters ?: point.rightWidthMeters,
                )
            }
        }
        val resolvedPitPoints = pitPoints.map { point ->
            TrackMapPoint(
                x = point.x,
                y = point.y,
            )
        }
        val resolvedIdealPoints = idealLine.map { point ->
            TrackMapPoint(
                x = point.x,
                y = point.y,
                leftWidthMeters = point.leftWidthMeters,
                rightWidthMeters = point.rightWidthMeters,
            )
        }
        val bounds = computeBounds(resolvedPoints + resolvedPitPoints + resolvedIdealPoints) ?: return null

        return TrackMap(
            gameId = AC_KEY,
            trackId = bundle.trackId,
            trackName = buildTrackDisplayName(trackFolder = bundle.trackFolder, layoutId = bundle.layoutId),
            layoutId = bundle.layoutId,
            createdAtEpochMs = createdAtEpochMs,
            referencePoint = ReferencePoint.FRONT_AXLE,
            points = resolvedPoints,
            pitPoints = resolvedPitPoints,
            idealLinePoints = resolvedIdealPoints,
            bounds = bounds,
            svgPath = bundle.trackMapSvgPath
                ?.takeIf(Files::isRegularFile)
                ?.toAbsolutePath()
                ?.normalize()
                ?.toString(),
        )
    }

    private fun nearestControlPoint(point: AcEvoTrackSample, controlPoints: List<AcEvoTrackSample>): AcEvoTrackSample? {
        var bestPoint: AcEvoTrackSample? = null
        var bestDistanceSq = Float.POSITIVE_INFINITY

        controlPoints.forEach { candidate ->
            val dx = candidate.x - point.x
            val dy = candidate.y - point.y
            val distanceSq = dx * dx + dy * dy
            if (distanceSq < bestDistanceSq) {
                bestDistanceSq = distanceSq
                bestPoint = candidate
            }
        }

        return bestPoint
    }

    private fun computeBounds(points: List<TrackMapPoint>): TrackMapBounds? {
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

        if (maxX <= minX || maxY <= minY) return null

        return TrackMapBounds(
            minX = minX,
            minY = minY,
            maxX = maxX,
            maxY = maxY,
        )
    }

    private fun buildTrackDisplayName(trackFolder: String, layoutId: String?): String {
        val base = humanizeToken(trackFolder)

        val layout = layoutId
            ?.takeIf { it.isNotBlank() && !it.equals("track_layout", ignoreCase = true) }
            ?.let(::humanizeToken)

        return if (layout == null || layout.equals(base, ignoreCase = true)) base else "$base $layout"
    }

    private fun humanizeToken(raw: String): String = raw.split('_')
        .filter(String::isNotBlank)
        .joinToString(" ") { token -> token.replaceFirstChar { ch -> ch.titlecase(Locale.US) } }
        .ifBlank { raw }

    private fun resolveRelativePath(root: Path, relativePath: String): Path {
        var current = root

        relativePath.split('/').filter(String::isNotBlank).forEach { segment ->
            current = current.resolve(segment)
        }

        return current.normalize()
    }

    private fun trackBundleKey(trackId: String, layoutId: String?): String = buildString {
        append(trackId)
        append('|')
        append(layoutId.normalizedLayoutId())
    }

    private fun String?.normalizedLayoutId(): String = TrackIdNormalizer.normalizeLayoutId(this).orEmpty()

    private fun isAceGame(gameId: String): Boolean = gameId.trim().lowercase(Locale.US) == AC_KEY
}

private fun AcEvoImportedContentSnapshot.toCacheKey(): CacheKey = CacheKey(
    packagePath = manifest.packagePath,
    packageSizeBytes = manifest.packageSizeBytes,
    packageLastModifiedEpochMs = manifest.packageLastModifiedEpochMs,
    assetCount = manifest.assets.size,
)
