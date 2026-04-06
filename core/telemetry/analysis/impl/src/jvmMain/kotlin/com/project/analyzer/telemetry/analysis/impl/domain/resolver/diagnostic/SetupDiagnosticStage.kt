package com.project.analyzer.telemetry.analysis.impl.domain.resolver.diagnostic

internal interface SetupDiagnosticStage {

    suspend fun analyze(input: SetupDiagnosticInput): SetupDiagnosticStageResult
}
