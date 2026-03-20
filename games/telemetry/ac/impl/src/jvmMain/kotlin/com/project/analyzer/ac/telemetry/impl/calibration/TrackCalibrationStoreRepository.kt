package com.project.analyzer.ac.telemetry.impl.calibration

import com.project.analyzer.api.di.IO
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import com.project.analyzer.utils.AppDirectories
import com.project.analyzer.utils.TrackIdentityAliasMatcher
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import dev.zacsweers.metro.binding
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class, binding = binding<TrackCalibrationRepository>())
class TrackCalibrationStoreRepository(
    private val json: Json,
    appDirectories: AppDirectories,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : TrackCalibrationRepository {

    private val calibrationsDir = appDirectories.userDataDir
        .resolve(TrackCalibrationFileNameResolver.DIRECTORY_NAME)
        .apply { mkdirs() }

    override suspend fun save(calibration: TrackCalibration, source: TrackCalibrationSource) {
        withContext(ioDispatcher) {
            val normalized = calibration.normalizeForStorage(source = source)
            val targetFile = calibrationsDir.resolve(
                TrackCalibrationFileNameResolver.resolveFileName(
                    trackId = normalized.trackId,
                    layoutId = normalized.layoutId,
                ),
            )
            targetFile.parentFile?.mkdirs()
            targetFile.writeText(
                text = json.encodeToString(TrackCalibration.serializer(), normalized),
                charset = StandardCharsets.UTF_8,
            )
        }
    }

    override suspend fun load(trackId: String, layoutId: String?): TrackCalibration? = withContext(ioDispatcher) {
        val normalizedLayoutId = normalizeCalibrationLayoutId(layoutId)
        loadDirectCalibration(trackId = trackId, layoutId = normalizedLayoutId)
            ?: findAliasedCalibration(
                trackId = trackId,
                layoutId = normalizedLayoutId,
            )
    }

    private fun loadDirectCalibration(trackId: String, layoutId: String?): TrackCalibration? =
        TrackCalibrationFileNameResolver.buildTrackIdCandidates(
            trackId = trackId,
            layoutId = layoutId,
        ).firstNotNullOfOrNull { candidateTrackId ->
            readCalibration(candidateTrackId)?.withResolvedLayout(layoutId)
        }

    private fun findAliasedCalibration(trackId: String, layoutId: String?): TrackCalibration? {
        val normalizedTrackId = trackId.trim()
        return calibrationFiles().asSequence()
            .mapNotNull { file ->
                readCalibration(file.nameWithoutExtension)
            }
            .map { calibration ->
                val resolved = calibration.withResolvedLayout(layoutId)
                scoreAliasedCalibrationMatch(
                    requestedTrackId = normalizedTrackId,
                    requestedLayoutId = layoutId,
                    calibration = resolved,
                ) to resolved
            }
            .filter { (score, _) -> score > 0 }
            .maxByOrNull { (score, calibration) ->
                score * 10_000 - calibration.trackId.length
            }
            ?.second
    }

    private fun scoreAliasedCalibrationMatch(
        requestedTrackId: String,
        requestedLayoutId: String?,
        calibration: TrackCalibration,
    ): Int {
        val requestedKeys = TrackIdentityAliasMatcher.buildAliasKeys(
            trackId = requestedTrackId,
            layoutId = requestedLayoutId,
            extraLayouts = listOf(calibration.layoutId),
        )
        if (requestedKeys.isEmpty()) return 0

        val calibrationKeys = TrackIdentityAliasMatcher.buildAliasKeys(
            trackId = calibration.trackId,
            layoutId = calibration.layoutId,
            extraLayouts = listOf(requestedLayoutId),
        )
        if (calibrationKeys.isEmpty()) return 0
        if (requestedKeys.none(calibrationKeys::contains)) return 0

        return when {
            calibration.trackId == requestedTrackId && calibration.layoutId == requestedLayoutId -> 4

            TrackIdentityAliasMatcher.areEquivalent(
                trackId = requestedTrackId,
                layoutId = requestedLayoutId,
                otherTrackId = calibration.trackId,
                otherLayoutId = calibration.layoutId,
            ) -> 3

            else -> 1
        }
    }

    override suspend fun loadBySource(
        trackId: String,
        source: TrackCalibrationSource,
        layoutId: String?,
    ): TrackCalibration? = load(trackId = trackId, layoutId = layoutId)
        ?.takeIf { it.source == source }

    override suspend fun loadAll(source: TrackCalibrationSource?): List<TrackCalibration> = withContext(ioDispatcher) {
        val calibrations = calibrationFiles()
            .mapNotNull { file -> readCalibration(file.nameWithoutExtension) }
            .groupBy { calibration -> calibration.trackId to calibration.layoutId.orEmpty() }
            .values
            .mapNotNull { duplicates ->
                duplicates.maxWithOrNull(
                    compareBy<TrackCalibration>(
                        { it.source == TrackCalibrationSource.USER },
                        TrackCalibration::createdAtEpochMs,
                    ),
                )
            }
            .sortedBy(TrackCalibration::trackId)
        if (source == null) {
            calibrations
        } else {
            calibrations.filter { it.source == source }
        }
    }

    private fun calibrationFiles() = calibrationsDir.listFiles()
        ?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
        ?: emptyList()

    private fun readCalibration(storageTrackId: String): TrackCalibration? {
        val fileName = TrackCalibrationFileNameResolver.resolveFileName(
            trackId = storageTrackId,
            layoutId = null,
        )
        val targetFile = calibrationsDir.resolve(fileName)
        if (!targetFile.isFile) return null
        return runCatching {
            json.decodeFromString(
                deserializer = TrackCalibration.serializer(),
                string = targetFile.readText(StandardCharsets.UTF_8),
            )
        }.getOrNull()?.normalizeForRead()
    }
}
