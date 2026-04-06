package com.project.analyzer.telemetry.analysis.api.model.report.comparison

public data class ComparativeAnalysis(
    val baseLap: LapReference = LapReference(),
    val comparisonLaps: List<LapReference> = emptyList(),
    val segmentComparisons: List<SegmentComparison> = emptyList(),
    val cornerComparisons: List<CornerComparison> = emptyList(),
    val biggestGains: List<ImprovementOpportunity> = emptyList(),
    val biggestLosses: List<PerformanceLoss> = emptyList(),
    val overallDelta: Long = 0L,
    val potentialGain: Long = 0L,
    val patternDetected: PatternType? = null,
    val recommendations: List<String> = emptyList(),
)
