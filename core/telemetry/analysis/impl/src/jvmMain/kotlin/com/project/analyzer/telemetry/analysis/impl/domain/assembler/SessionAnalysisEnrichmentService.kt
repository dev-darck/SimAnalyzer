package com.project.analyzer.telemetry.analysis.impl.domain.assembler

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisCornerZone
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.api.model.track.SessionAnalysisTrackMap
import com.project.analyzer.telemetry.analysis.impl.domain.extension.bestLapBySegmentId
import com.project.analyzer.telemetry.analysis.impl.domain.extension.cornerZonesBySegmentId
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.SessionAnalysisHighlightPipeline
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.SessionAnalysisHighlightPipelineInput
import com.project.analyzer.telemetry.analysis.impl.domain.highlight.mapper.finalizeHighlights
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics.ComprehensiveSessionAnalysisResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZone
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone.TrackMapCornerZoneDetector
import dev.zacsweers.metro.Inject

/**
 * Runs the expensive post-processing stages after the shell report is already available to the UI.
 */
@Inject
internal class SessionAnalysisEnrichmentService(
    private val trackMapCornerZoneDetector: TrackMapCornerZoneDetector,
    private val highlightPipeline: SessionAnalysisHighlightPipeline,
    private val comprehensiveSessionAnalysisResolver: ComprehensiveSessionAnalysisResolver,
) {

    suspend fun enrichReport(report: SessionAnalysisReport): SessionAnalysisReport {
        if (report.samples.isEmpty() || (report.highlights.isNotEmpty() && report.comprehensiveAnalysis != null)) {
            return report
        }

        val input = SessionAnalysisHighlightPipelineInput(
            samples = report.samples,
            bestLapBySegmentId = report.bestLapBySegmentId(),
            tyreProfile = report.tyreProfile,
            cornerZonesBySegmentId = report.cornerZonesBySegmentId(trackMapCornerZoneDetector),
        )
        val context = highlightPipeline.analyze(input)
        val highlights = context.drafts.finalizeHighlights()

        return report.copy(
            highlights = highlights,
            comprehensiveAnalysis = comprehensiveSessionAnalysisResolver.resolve(
                report = report,
                cornerReport = context.cornerReport,
                setupReport = context.setupReport,
                consistencyReport = context.consistencyReport,
                highlights = highlights,
            ),
        )
    }

    fun detectCornerZones(trackMap: SessionAnalysisTrackMap): List<SessionAnalysisCornerZone> =
        trackMapCornerZoneDetector.detect(trackMap).map(TrackMapCornerZone::toApiCornerZone)

    internal suspend fun buildHighlights(
        report: SessionAnalysisReport,
        cornerZonesBySegmentId: Map<Long, List<TrackMapCornerZone>> = report.cornerZonesBySegmentId(
            trackMapCornerZoneDetector,
        ),
    ): List<SessionAnalysisHighlight> = highlightPipeline.execute(
        input = SessionAnalysisHighlightPipelineInput(
            samples = report.samples,
            bestLapBySegmentId = report.bestLapBySegmentId(),
            tyreProfile = report.tyreProfile,
            cornerZonesBySegmentId = cornerZonesBySegmentId,
        ),
    )
}

private fun TrackMapCornerZone.toApiCornerZone(): SessionAnalysisCornerZone = SessionAnalysisCornerZone(
    cornerNumber = cornerNumber,
    startTrackPosition = startTrackPosition,
    endTrackPosition = endTrackPosition,
    apexTrackPosition = apexTrackPosition,
    peakCurvature = peakCurvature,
)
