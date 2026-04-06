package com.project.analyzer.telemetry.analysis.impl.domain.highlight

import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisConsistencyResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisResolver
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnosticResolver
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Emits highlights for repeatable corner mistakes so the feed surfaces actionable problem areas first.
 */
@Inject
internal class SessionAnalysisCornerDiagnosticsHighlightStage(
    private val cornerAnalysisResolver: SessionAnalysisCornerAnalysisResolver,
    private val setupDiagnosticResolver: SessionAnalysisSetupDiagnosticResolver,
    private val consistencyResolver: SessionAnalysisConsistencyResolver,
) : SessionAnalysisHighlightStage {

    override val stageKey: String = "corner-diagnostics"

    override fun isEnabled(options: SessionAnalysisHighlightPipelineOptions): Boolean = options.includeCornerDiagnostics

    override suspend fun execute(input: SessionAnalysisHighlightContext): SessionAnalysisHighlightContext =
        coroutineScope {
            val cornerReport = cornerAnalysisResolver.resolve(
                samples = input.samples,
                bestLapBySegmentId = input.bestLapBySegmentId,
                cornerZonesBySegmentId = input.cornerZonesBySegmentId,
            )
            val setupReportDeferred = async {
                setupDiagnosticResolver.resolve(
                    corners = cornerReport.corners,
                    samples = input.samples,
                    tyreProfile = input.tyreProfile,
                )
            }
            val consistencyReportDeferred = async {
                consistencyResolver.resolve(cornerReport.corners)
            }

            input.copy(
                cornerReport = cornerReport,
                setupReport = setupReportDeferred.await(),
                consistencyReport = consistencyReportDeferred.await(),
            )
        }
}
