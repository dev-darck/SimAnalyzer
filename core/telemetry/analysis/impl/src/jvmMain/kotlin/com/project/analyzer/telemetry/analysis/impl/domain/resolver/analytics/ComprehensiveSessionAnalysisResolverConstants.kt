package com.project.analyzer.telemetry.analysis.impl.domain.resolver.analytics

/**
 * Shared thresholds keep the split analytics helpers scoring segments and issues on the same scale.
 */
internal object ComprehensiveSessionAnalysisResolverConstants {

    internal const val DefaultTrackLengthMeters: Float = 5_000f
    internal const val MinStraightWidth: Float = 0.035f
    internal const val WarningDeltaMs: Long = 45L
    internal const val CriticalDeltaMs: Long = 120L
}
