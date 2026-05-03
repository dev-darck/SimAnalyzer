package com.analyzer.session.analysis.domain.model

import com.project.analyzer.telemetry.ac.api.model.calibration.TrackCalibration
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap

internal data class SessionAnalysisWorkspaceData(
    val report: SessionAnalysisReport,
    val referenceReport: SessionAnalysisReport? = null,
    val calibration: TrackCalibration? = null,
    val authoredTrackMap: SessionAnalysisTrackMap? = null,
    val sourceTrackMap: SessionAnalysisTrackMap? = null,
    val displayTrackMap: SessionAnalysisTrackMap? = null,
)
