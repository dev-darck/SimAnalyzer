package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone

/**
 * Bundles the report state each highlight stage needs so pipeline composition stays explicit and testable.
 */
internal data class SessionAnalysisHighlightPipelineInput(
    val samples: List<SessionAnalysisSample>,
    val bestLapBySegmentId: Map<Long, Int?>,
    val tyreProfile: SessionAnalysisTyreProfile?,
    val cornerZonesBySegmentId: Map<Long, List<TrackMapCornerZone>> = emptyMap(),
)
