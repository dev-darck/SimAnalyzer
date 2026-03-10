package com.project.analyzer.ac.telemetry.impl.trackmap.evo

import com.project.analyzer.ac.telemetry.impl.internal.TrackIdNormalizer
import com.project.analyzer.ac.telemetry.impl.trackmap.TrackMapGameProvider
import com.project.analyzer.game.api.AC_KEY
import com.project.analyzer.telemetry.ac.api.model.calibration.ReferencePoint
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapBounds
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMapPoint
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.nio.file.Path
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.hypot

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TrackMapGameProvider>())
@Inject
@Suppress("unused")
class AcEvoImportedTrackMapProvider internal constructor(
    private val importer: AcEvoTrackAssetImporter,
    private val parsers: AcEvoTrackMapAssetParsers,
) : TrackMapGameProvider {

    private val logger = logger()
    private val mutex = Mutex()

    @Volatile
    private var cachedKey: CacheKey? = null

    @Volatile
    private var cachedMaps: List<TrackMap> = emptyList()

    override suspend fun load(gameId: String, trackId: String, layoutId: String?): TrackMap? {
        if (!isAceGame(gameId)) return null
        val normalizedTrackId = trackId.trim().lowercase(Locale.US).takeIf { it.isNotBlank() } ?: return null
        val normalizedLayoutId = layoutId.normalizedLayoutId()
        val maps = ensureMaps()
        val candidates = maps.filter { it.trackId == normalizedTrackId }
        if (candidates.isEmpty()) return null
        if (normalizedLayoutId.isNotEmpty()) {
            return candidates.firstOrNull { it.layoutId.normalizedLayoutId() == normalizedLayoutId }
                ?: candidates.firstOrNull { it.layoutId.normalizedLayoutId().isEmpty() }
        }
        return candidates.firstOrNull { it.layoutId.normalizedLayoutId().isEmpty() }
            ?: candidates.maxByOrNull { it.createdAtEpochMs }
    }

    override suspend fun loadAll(gameId: String?): List<TrackMap> {
        if (gameId != null && !isAceGame(gameId)) return emptyList()
        return ensureMaps()
    }

    private suspend fun ensureMaps(): List<TrackMap> = mutex.withLock {
        logger.info { "AcEvoImportedTrackMapProvider.ensureMaps start" }
        val snapshot = importer.ensureImported() ?: return emptyList()
        val key = CacheKey(
            packagePath = snapshot.manifest.packagePath,
            packageSizeBytes = snapshot.manifest.packageSizeBytes,
            packageLastModifiedEpochMs = snapshot.manifest.packageLastModifiedEpochMs,
            assetCount = snapshot.manifest.assets.size,
        )
        if (cachedKey == key) {
            logger.info { "AcEvoImportedTrackMapProvider.ensureMaps cache hit maps=${cachedMaps.size}" }
            return cachedMaps
        }

        val bundles = linkedMapOf<String, TrackAssetBundleBuilder>()
        snapshot.manifest.assets.forEach { asset ->
            val trackId = asset.trackId?.trim()?.takeIf { it.isNotBlank() } ?: return@forEach
            if (asset.kind == AcEvoImportedAssetKind.TRACKMAP_SVG) return@forEach
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
            }
        }

        val builtMaps = bundles.values.mapNotNull { bundle ->
            buildTrackMap(
                bundle = bundle.toImmutable(),
                createdAtEpochMs = snapshot.manifest.packageLastModifiedEpochMs,
            )
        }
            .sortedBy { map -> trackBundleKey(trackId = map.trackId, layoutId = map.layoutId) }

        cachedKey = key
        cachedMaps = builtMaps
        logger.info { "AcEvoImportedTrackMapProvider.ensureMaps built maps=${builtMaps.size}" }
        builtMaps
    }

    private fun buildTrackMap(bundle: TrackAssetBundle, createdAtEpochMs: Long): TrackMap? {
        val controlPoints = bundle.controlPointsPath?.let(parsers::parseTrackControlPoints).orEmpty()
        val centerline = bundle.splineJsonPath?.let(parsers::parseSplineJson).orEmpty()
            .ifEmpty { bundle.idealLinePath?.let(parsers::parseAiSpline).orEmpty() }
            .ifEmpty { densifyControlPoints(controlPoints) }
        if (centerline.size < 2) {
            logger.debug { "Skipping AC EVO imported map without centerline: ${bundle.trackId}" }
            return null
        }

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
        val bounds = computeBounds(resolvedPoints + resolvedPitPoints) ?: return null

        return TrackMap(
            gameId = AC_KEY,
            trackId = bundle.trackId,
            trackName = buildTrackDisplayName(trackFolder = bundle.trackFolder, layoutId = bundle.layoutId),
            layoutId = bundle.layoutId,
            createdAtEpochMs = createdAtEpochMs,
            referencePoint = ReferencePoint.FRONT_AXLE,
            points = resolvedPoints,
            pitPoints = resolvedPitPoints,
            bounds = bounds,
        )
    }

    private fun densifyControlPoints(points: List<AcEvoTrackSample>): List<AcEvoTrackSample> {
        if (points.size < 2) return points
        val densified = mutableListOf<AcEvoTrackSample>()
        for (index in 0 until points.lastIndex) {
            val start = points[index]
            val end = points[index + 1]
            val distance = hypot((end.x - start.x).toDouble(), (end.y - start.y).toDouble())
            val segments = maxOf(1, ceil(distance / CONTROL_POINT_RESAMPLE_STEP_METERS).toInt())
            repeat(segments) { segmentIndex ->
                val t = segmentIndex.toFloat() / segments.toFloat()
                densified += lerp(start, end, t)
            }
        }
        densified += points.last()
        return densified
    }

    private fun lerp(start: AcEvoTrackSample, end: AcEvoTrackSample, t: Float): AcEvoTrackSample = AcEvoTrackSample(
        x = start.x + (end.x - start.x) * t,
        y = start.y + (end.y - start.y) * t,
        leftWidthMeters = start.leftWidthMeters + (end.leftWidthMeters - start.leftWidthMeters) * t,
        rightWidthMeters = start.rightWidthMeters + (end.rightWidthMeters - start.rightWidthMeters) * t,
    )

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

    private fun humanizeToken(raw: String): String = raw
        .split('_')
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

    private companion object {

        const val CONTROL_POINT_RESAMPLE_STEP_METERS = 5.0
    }
}

private data class TrackAssetBundle(
    val trackId: String,
    val trackFolder: String,
    val layoutId: String?,
    val splineJsonPath: Path?,
    val idealLinePath: Path?,
    val pitlanePath: Path?,
    val controlPointsPath: Path?,
    val trackLayoutPath: Path?,
)

private data class TrackAssetBundleBuilder(
    val trackId: String,
    val trackFolder: String,
    val layoutId: String?,
    var splineJsonPath: Path? = null,
    var idealLinePath: Path? = null,
    var pitlanePath: Path? = null,
    var controlPointsPath: Path? = null,
    var trackLayoutPath: Path? = null,
) {

    fun toImmutable(): TrackAssetBundle = TrackAssetBundle(
        trackId = trackId,
        trackFolder = trackFolder,
        layoutId = layoutId,
        splineJsonPath = splineJsonPath,
        idealLinePath = idealLinePath,
        pitlanePath = pitlanePath,
        controlPointsPath = controlPointsPath,
        trackLayoutPath = trackLayoutPath,
    )
}

private data class CacheKey(
    val packagePath: String,
    val packageSizeBytes: Long,
    val packageLastModifiedEpochMs: Long,
    val assetCount: Int,
)
