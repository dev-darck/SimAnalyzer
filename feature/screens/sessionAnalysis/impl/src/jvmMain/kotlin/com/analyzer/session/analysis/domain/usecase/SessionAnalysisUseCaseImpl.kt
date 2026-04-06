package com.analyzer.session.analysis.domain.usecase

import com.analyzer.session.analysis.domain.model.SessionAnalysisWorkspaceData
import com.analyzer.session.analysis.domain.repository.SessionAnalysisRepository
import com.analyzer.session.analysis.domain.trackmap.SessionAnalysisTrackMapMerger
import com.analyzer.session.analysis.domain.trackmap.TrackMapSessionAnalysisMapper
import com.project.analyzer.api.di.IO
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import com.project.analyzer.utils.logger.logger
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Loads the lightweight workspace first and keeps authored assets alongside telemetry so the UI can
 * render immediately and later switch to enriched analysis without rebuilding its track context.
 */
@Inject
@SingleIn(ScreenScope::class)
internal class SessionAnalysisUseCaseImpl(
    private val repository: SessionAnalysisRepository,
    private val trackMapMapper: TrackMapSessionAnalysisMapper,
    private val trackMapMerger: SessionAnalysisTrackMapMerger,
    @param:IO
    private val ioDispatcher: CoroutineDispatcher,
) : SessionAnalysisUseCase {

    private val logger = logger()

    /**
     * Fetches the shell report, metadata, calibration, and imported track assets in parallel, then
     * builds a workspace that preserves both telemetry geometry and presentation geometry.
     */
    override suspend fun loadWorkspaceShell(sessionId: Long, forceRefresh: Boolean): SessionAnalysisWorkspaceData? =
        withContext(ioDispatcher) {
            coroutineScope {
                val reportDeferred = async {
                    repository.loadSessionShellReport(
                        sessionId = sessionId,
                        forceRefresh = forceRefresh,
                    )
                }
                val metadataDeferred = async {
                    repository.loadSessionMetadata(
                        sessionId = sessionId,
                        forceRefresh = forceRefresh,
                    )
                }

                val metadata = metadataDeferred.await()
                val trackMapDeferred = async {
                    runCatching { loadTrackMap(metadata) }
                        .onFailure { error ->
                            logger.warn(error) { "Failed to load track map for session $sessionId" }
                        }
                        .getOrNull()
                }
                val calibrationDeferred = async {
                    runCatching { loadCalibration(metadata) }
                        .onFailure { error ->
                            logger.warn(error) { "Failed to load calibration for session $sessionId" }
                        }
                        .getOrNull()
                }
                val importedTrackMap = trackMapDeferred.await()
                val importedCornerZonesDeferred = async {
                    importedTrackMap
                        ?.let { trackMap ->
                            runCatching {
                                repository.detectCornerZones(
                                    trackMap,
                                )
                            }.getOrDefault(emptyList())
                        }
                        .orEmpty()
                }
                val report = reportDeferred.await() ?: return@coroutineScope null
                val calibration = calibrationDeferred.await()
                val reportTrackMap = report.trackMap
                val resolvedReport = report.withPreferredCornerZones(
                    preferredCornerZones = importedCornerZonesDeferred.await(),
                )

                SessionAnalysisWorkspaceData(
                    report = resolvedReport,
                    calibration = calibration,
                    authoredTrackMap = importedTrackMap,
                    sourceTrackMap = reportTrackMap ?: importedTrackMap,
                    displayTrackMap = resolveDisplayTrackMap(
                        reportTrackMap = reportTrackMap,
                        importedTrackMap = importedTrackMap,
                    ),
                )
            }
        }

    override suspend fun enrichWorkspace(workspaceData: SessionAnalysisWorkspaceData): SessionAnalysisWorkspaceData =
        workspaceData.copy(
            report = repository.enrichSessionReport(workspaceData.report),
        )

    /**
     * Builds the canvas track map from authored geometry when available while keeping raw telemetry
     * geometry untouched for analytics and sample-to-track lookups.
     */
    private fun resolveDisplayTrackMap(
        reportTrackMap: SessionAnalysisTrackMap?,
        importedTrackMap: SessionAnalysisTrackMap?,
    ): SessionAnalysisTrackMap? = when {
        importedTrackMap == null -> reportTrackMap

        reportTrackMap == null -> importedTrackMap

        else -> trackMapMerger.merge(
            authoredTrackMap = importedTrackMap,
            telemetryTrackMap = reportTrackMap,
        ) ?: importedTrackMap
    }

    private suspend fun loadTrackMap(metadata: RecordedTelemetrySessionMetadata?): SessionAnalysisTrackMap? {
        metadata ?: return null
        val trackId = metadata.trackId ?: return null
        val trackMap = repository.loadTrackMap(
            gameId = metadata.gameId,
            trackId = trackId,
            layoutId = metadata.layoutId,
        ) ?: return null
        return trackMapMapper.map(trackMap)
    }

    private suspend fun loadCalibration(metadata: RecordedTelemetrySessionMetadata?): TrackCalibration? {
        metadata ?: return null
        val trackId = metadata.trackId ?: return null
        return repository.loadGameCalibration(
            trackId = trackId,
            layoutId = metadata.layoutId,
        ) ?: repository.loadCalibration(
            trackId = trackId,
            layoutId = metadata.layoutId,
        )
    }
}
