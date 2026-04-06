package com.project.analyzer.telemetry.analysis.api.model.report.session

import com.project.analyzer.telemetry.analysis.api.model.header.SessionAnalysisHeader
import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap
import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.segment.SessionAnalysisSegment
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.api.model.vehicle.SessionAnalysisVehicleClass

public data class SessionAnalysisReport(
    val sessionId: Long,
    val header: SessionAnalysisHeader,
    val vehicleClass: SessionAnalysisVehicleClass,
    val tyreProfile: SessionAnalysisTyreProfile? = null,
    val trackMap: SessionAnalysisTrackMap? = null,
    val segments: List<SessionAnalysisSegment> = emptyList(),
    val laps: List<SessionAnalysisLap> = emptyList(),
    val samples: List<SessionAnalysisSample> = emptyList(),
    val highlights: List<SessionAnalysisHighlight> = emptyList(),
    val cornerZonesBySegmentId: Map<Long, List<SessionAnalysisCornerZone>> = emptyMap(),
    val bestLapNumber: Int? = null,
    val comprehensiveAnalysis: ComprehensiveSessionAnalysis? = null,
)
