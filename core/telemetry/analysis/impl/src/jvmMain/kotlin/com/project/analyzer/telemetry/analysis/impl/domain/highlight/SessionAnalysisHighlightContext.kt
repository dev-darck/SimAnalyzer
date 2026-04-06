package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisConsistencyReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnosticReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone

internal data class SessionAnalysisHighlightContext(
    val samples: List<SessionAnalysisSample>,
    val bestLapBySegmentId: Map<Long, Int?>,
    val tyreProfile: SessionAnalysisTyreProfile?,
    val cornerZonesBySegmentId: Map<Long, List<TrackMapCornerZone>> = emptyMap(),
    val drafts: List<SessionAnalysisHighlightDraft> = emptyList(),
    val cornerReport: SessionAnalysisCornerAnalysisReport? = null,
    val setupReport: SessionAnalysisSetupDiagnosticReport? = null,
    val consistencyReport: SessionAnalysisConsistencyReport? = null,
) {

    fun appendDrafts(additionalDrafts: Iterable<SessionAnalysisHighlightDraft>): SessionAnalysisHighlightContext =
        copy(drafts = drafts + additionalDrafts)
}
