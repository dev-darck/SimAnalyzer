package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample
import com.project.analyzer.telemetry.analysis.api.model.tyre.SessionAnalysisTyreProfile
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerAnalysis
import com.project.analyzer.telemetry.analysis.impl.domain.resolver.corner.SessionAnalysisCornerKey
import dev.zacsweers.metro.Inject

/**
 * Coordinates setup-specific diagnostic stages while keeping the composition order explicit.
 */
@Inject
internal class SessionAnalysisSetupDiagnosticResolver(
    private val balanceDiagnosticResolver: BalanceDiagnosticResolver,
    private val tyreTemperatureDiagnosticResolver: TyreTemperatureDiagnosticResolver,
    private val tyrePressureDiagnosticResolver: TyrePressureDiagnosticResolver,
    private val brakeBiasDiagnosticResolver: BrakeBiasDiagnosticResolver,
    private val aeroBalanceDiagnosticResolver: AeroBalanceDiagnosticResolver,
    private val damperDiagnosticResolver: DamperDiagnosticResolver,
) {

    private val stages: List<SetupDiagnosticStage> = listOf(
        balanceDiagnosticResolver,
        tyreTemperatureDiagnosticResolver,
        tyrePressureDiagnosticResolver,
        brakeBiasDiagnosticResolver,
        aeroBalanceDiagnosticResolver,
        damperDiagnosticResolver,
    )

    internal suspend fun resolve(
        corners: List<SessionAnalysisCornerAnalysis>,
        samples: List<SessionAnalysisSample>,
        tyreProfile: SessionAnalysisTyreProfile?,
    ): SessionAnalysisSetupDiagnosticReport {
        if (corners.isEmpty() && samples.isEmpty()) return SessionAnalysisSetupDiagnosticReport()

        val input = SetupDiagnosticInput(
            corners = corners,
            samples = samples,
            tyreProfile = tyreProfile,
        )
        val diagnostics = mutableListOf<SessionAnalysisSetupDiagnostic>()
        val cornerInsights = linkedMapOf<SessionAnalysisCornerKey, SessionAnalysisCornerSetupInsight>()

        stages.forEach { stage ->
            val result = stage.analyze(input)
            diagnostics += result.diagnostics
            cornerInsights += result.cornerInsights
        }

        return SessionAnalysisSetupDiagnosticReport(
            diagnostics = diagnostics,
            cornerInsights = cornerInsights,
        )
    }
}
