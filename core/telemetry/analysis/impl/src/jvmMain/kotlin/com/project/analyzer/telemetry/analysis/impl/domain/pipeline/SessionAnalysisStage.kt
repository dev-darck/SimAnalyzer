package com.project.analyzer.telemetry.analysis.impl.domain.pipeline

/**
 * Small pipeline contract used by staged analysis flows to keep orchestration and work units separate.
 */
internal fun interface SessionAnalysisStage<I, O> {

    suspend fun execute(input: I): O
}
