package com.analyzer.session.analysis.domain.repository

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.ac.api.model.trackmap.TrackMap
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.recording.api.session.RecordedTelemetrySessionMetadata

internal interface SessionAnalysisRepository {

    suspend fun loadSessionShellReport(sessionId: Long, forceRefresh: Boolean = false): SessionAnalysisReport?

    suspend fun enrichSessionReport(report: SessionAnalysisReport): SessionAnalysisReport

    suspend fun detectCornerZones(trackMap: SessionAnalysisTrackMap): List<SessionAnalysisCornerZone>

    suspend fun loadSessionMetadata(sessionId: Long, forceRefresh: Boolean = false): RecordedTelemetrySessionMetadata?

    suspend fun loadTrackMap(gameId: String, trackId: String, layoutId: String?): TrackMap?

    suspend fun loadGameCalibration(trackId: String, layoutId: String?): TrackCalibration?

    suspend fun loadCalibration(trackId: String, layoutId: String?): TrackCalibration?
}
