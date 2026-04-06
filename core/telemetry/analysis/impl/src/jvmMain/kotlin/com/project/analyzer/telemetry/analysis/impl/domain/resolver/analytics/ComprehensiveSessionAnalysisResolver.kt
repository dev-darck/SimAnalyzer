package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.report.session.ComprehensiveSessionAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.session.SessionAnalysisReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.consistency.SessionAnalysisConsistencyReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysisReport
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic.SessionAnalysisSetupDiagnosticReport
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Orchestrates the high-level analytics branches and merges them into the comprehensive session
 * report consumed by the richer coaching UI.
 */
@Inject
internal class ComprehensiveSessionAnalysisResolver(
    private val consistencyBuilder: ComprehensiveSessionAnalysisConsistencyBuilder,
    private val contextResolver: ComprehensiveSessionAnalysisContextResolver,
    private val cornerBuilder: ComprehensiveSessionAnalysisCornerBuilder,
    private val segmentBuilder: ComprehensiveSessionAnalysisSegmentBuilder,
    private val summaryBuilder: ComprehensiveSessionAnalysisSummaryBuilder,
    private val tyreFuelBuilder: ComprehensiveSessionAnalysisTyreFuelBuilder,
) {

    /**
     * Resolves the independent analysis branches in parallel and stitches them into one coherent
     * session model.
     */
    suspend fun resolve(
        report: SessionAnalysisReport,
        cornerReport: SessionAnalysisCornerAnalysisReport?,
        setupReport: SessionAnalysisSetupDiagnosticReport?,
        consistencyReport: SessionAnalysisConsistencyReport?,
        highlights: List<SessionAnalysisHighlight>,
    ): ComprehensiveSessionAnalysis = coroutineScope {
        val context = contextResolver.resolve(report)
        val config = contextResolver.configFor(report.vehicleClass, report.tyreProfile)

        val cornersDeferred = async {
            cornerBuilder.build(
                report = report,
                cornerReport = cornerReport,
                setupReport = setupReport,
                consistencyReport = consistencyReport,
                config = config,
            )
        }
        val tyreDeferred = async { tyreFuelBuilder.buildTyreAnalyses(report, report.tyreProfile) }
        val fuelDeferred = async { tyreFuelBuilder.buildFuelAnalysis(report, context) }
        val consistencyDeferred = async { consistencyBuilder.build(report, consistencyReport) }
        val setupDeferred = async { summaryBuilder.buildSetupRecommendations(setupReport) }

        val cornerAnalyses = cornersDeferred.await()
        val segmentAnalyses = segmentBuilder.build(
            report = report,
            cornerAnalyses = cornerAnalyses,
            cornerReport = cornerReport,
        )
        val tyreAnalyses = tyreDeferred.await()
        val fuelAnalysis = fuelDeferred.await()
        val consistencyAnalysis = consistencyDeferred.await()
        val setupRecommendations = setupDeferred.await()
        val brakingAnalyses = cornerAnalyses.map(::cornerToBraking)
        val accelerationAnalyses = cornerAnalyses.map(::cornerToAcceleration)
        val steeringAnalyses = cornerAnalyses.map(::cornerToSteering)
        val comparativeAnalysis = summaryBuilder.buildComparativeAnalysis(report, segmentAnalyses, cornerAnalyses)
        val summary = summaryBuilder.buildSessionSummary(
            report = report,
            context = context,
            segmentAnalyses = segmentAnalyses,
            cornerAnalyses = cornerAnalyses,
            tyreAnalyses = tyreAnalyses,
            fuelAnalysis = fuelAnalysis,
            consistencyAnalysis = consistencyAnalysis,
            comparativeAnalysis = comparativeAnalysis,
            highlights = highlights,
        )
        val narrative = summaryBuilder.buildNarrative(context, summary, setupRecommendations, comparativeAnalysis)

        ComprehensiveSessionAnalysis(
            context = context,
            sessionSummary = summary.copy(sessionNarrative = narrative),
            segmentAnalyses = segmentAnalyses,
            cornerAnalyses = cornerAnalyses,
            brakingAnalyses = brakingAnalyses,
            accelerationAnalyses = accelerationAnalyses,
            steeringAnalyses = steeringAnalyses,
            tyreAnalyses = tyreAnalyses,
            fuelAnalysis = fuelAnalysis,
            consistencyAnalysis = consistencyAnalysis,
            comparativeAnalysis = comparativeAnalysis,
            setupRecommendations = setupRecommendations,
            highlights = highlights,
            narrative = narrative,
        )
    }
}
