package com.project.analyzer.telemetry.analysis.api.model.report.session

import com.project.analyzer.telemetry.analysis.api.model.highlight.SessionAnalysisHighlight
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.AccelerationAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.BrakingAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.ConsistencyAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.FuelAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.SteeringAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.analysis.TyreAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.comparison.ComparativeAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.context.SessionContext
import com.project.analyzer.telemetry.analysis.api.model.report.corner.EnhancedCornerAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.segment.SegmentAnalysis
import com.project.analyzer.telemetry.analysis.api.model.report.setup.SetupRecommendation

public data class ComprehensiveSessionAnalysis(
    val context: SessionContext = SessionContext(),
    val sessionSummary: SessionSummary = SessionSummary(),
    val segmentAnalyses: List<SegmentAnalysis> = emptyList(),
    val cornerAnalyses: List<EnhancedCornerAnalysis> = emptyList(),
    val brakingAnalyses: List<BrakingAnalysis> = emptyList(),
    val accelerationAnalyses: List<AccelerationAnalysis> = emptyList(),
    val steeringAnalyses: List<SteeringAnalysis> = emptyList(),
    val tyreAnalyses: List<TyreAnalysis> = emptyList(),
    val fuelAnalysis: FuelAnalysis = FuelAnalysis(),
    val consistencyAnalysis: ConsistencyAnalysis = ConsistencyAnalysis(),
    val comparativeAnalysis: ComparativeAnalysis? = null,
    val setupRecommendations: List<SetupRecommendation> = emptyList(),
    val highlights: List<SessionAnalysisHighlight> = emptyList(),
    val narrative: String = "",
)
