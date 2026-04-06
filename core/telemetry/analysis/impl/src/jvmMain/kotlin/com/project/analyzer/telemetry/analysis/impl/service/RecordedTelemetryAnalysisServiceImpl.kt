package com.project.analyzer.telemetry.analysis.impl.service

import com.project.analyzer.api.di.ScreenScope
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.service.RecordedTelemetryAnalysisService
import com.project.analyzer.telemetry.analysis.impl.domain.assembler.SessionAnalysisEnrichmentService
import com.project.analyzer.telemetry.analysis.impl.domain.assembler.SessionAnalysisShellFactory
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionReader
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn

/**
 * Orchestrates the full recorded-session workflow from bundle loading through report enrichment and highlights.
 */
@Inject
@SingleIn(ScreenScope::class)
internal class RecordedTelemetryAnalysisServiceImpl(
    private val sessionReader: RecordedTelemetrySessionReader,
    private val shellFactory: SessionAnalysisShellFactory,
    private val enrichmentService: SessionAnalysisEnrichmentService,
) : RecordedTelemetryAnalysisService {

    override suspend fun loadSessionReportShell(sessionId: Long, includeTrackMap: Boolean, forceRefresh: Boolean) =
        sessionReader.readSession(
            sessionId = sessionId,
            forceRefresh = forceRefresh,
        )?.let { decoded ->
            shellFactory.createShell(
                data = decoded,
                includeTrackMap = includeTrackMap,
            )
        }

    override suspend fun enrichSessionReport(report: SessionAnalysisReport): SessionAnalysisReport =
        enrichmentService.enrichReport(report)

    override fun detectCornerZones(trackMap: SessionAnalysisTrackMap): List<SessionAnalysisCornerZone> =
        enrichmentService.detectCornerZones(trackMap)
}
