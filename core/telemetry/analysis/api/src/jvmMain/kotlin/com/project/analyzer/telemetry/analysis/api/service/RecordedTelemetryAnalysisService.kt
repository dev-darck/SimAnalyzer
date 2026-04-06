package com.project.analyzer.telemetry.analysis.api.service

import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap

public interface RecordedTelemetryAnalysisService {

    public suspend fun loadSessionReport(
        sessionId: Long,
        includeTrackMap: Boolean = true,
        forceRefresh: Boolean = false,
    ): SessionAnalysisReport? = loadSessionReportShell(
        sessionId = sessionId,
        includeTrackMap = includeTrackMap,
        forceRefresh = forceRefresh,
    )?.let { report -> enrichSessionReport(report) }

    public suspend fun loadSessionReportShell(
        sessionId: Long,
        includeTrackMap: Boolean = true,
        forceRefresh: Boolean = false,
    ): SessionAnalysisReport?

    public suspend fun enrichSessionReport(report: SessionAnalysisReport): SessionAnalysisReport

    public fun detectCornerZones(trackMap: SessionAnalysisTrackMap): List<SessionAnalysisCornerZone>
}
