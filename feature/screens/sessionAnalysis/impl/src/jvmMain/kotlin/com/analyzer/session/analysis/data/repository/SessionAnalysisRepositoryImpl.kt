package com.analyzer.session.analysis.data.repository

import com.analyzer.session.analysis.domain.repository.SessionAnalysisRepository
import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.ac.api.calibration.TrackCalibrationRepository
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibrationSource
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.ac.api.trackmap.TrackMapRepository
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.service.RecordedTelemetryAnalysisService
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionStorage
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Adapts the telemetry-analysis service into the screen-facing repository contract.
 */
@Inject
@SingleIn(ScreenScope::class)
internal class SessionAnalysisRepositoryImpl(
    private val analysisService: RecordedTelemetryAnalysisService,
    private val sessionStorage: RecordedTelemetrySessionStorage,
    private val trackMapRepository: TrackMapRepository,
    private val trackCalibrationRepository: TrackCalibrationRepository,
) : SessionAnalysisRepository {

    override suspend fun loadSessionShellReport(sessionId: Long, forceRefresh: Boolean): SessionAnalysisReport? =
        analysisService.loadSessionReportShell(
            sessionId = sessionId,
            includeTrackMap = true,
            forceRefresh = forceRefresh,
        )

    override suspend fun enrichSessionReport(report: SessionAnalysisReport): SessionAnalysisReport =
        analysisService.enrichSessionReport(report)

    override suspend fun detectCornerZones(trackMap: SessionAnalysisTrackMap): List<SessionAnalysisCornerZone> =
        analysisService.detectCornerZones(trackMap)

    override suspend fun loadSessionMetadata(
        sessionId: Long,
        forceRefresh: Boolean,
    ): RecordedTelemetrySessionMetadata? = sessionStorage.findBundle(
        sessionId = sessionId,
        forceRefresh = forceRefresh,
    )?.metadata

    override suspend fun loadTrackMap(gameId: String, trackId: String, layoutId: String?): TrackMap? =
        trackMapRepository.load(
            gameId = gameId,
            trackId = trackId,
            layoutId = layoutId,
        )

    override suspend fun loadGameCalibration(trackId: String, layoutId: String?): TrackCalibration? =
        trackCalibrationRepository.loadBySource(
            trackId = trackId,
            source = TrackCalibrationSource.GAME,
            layoutId = layoutId,
        )

    override suspend fun loadCalibration(trackId: String, layoutId: String?): TrackCalibration? =
        trackCalibrationRepository.load(
            trackId = trackId,
            layoutId = layoutId,
        )
}
