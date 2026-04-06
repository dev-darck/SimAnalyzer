package com.project.analyzer.telemetry.analysis.impl.domain.resolver.trackzone

import kotlin.math.PI

internal const val detectorMinimumPointCount: Int = 20
internal const val detectorMinimumTrackLengthMeters: Float = 150f
internal const val detectorMinimumResampleCount: Int = 96
internal const val detectorTargetResamplePoints: Int = 420
internal const val detectorMinimumResampleStepMeters: Float = 2.5f
internal const val detectorMaximumResampleStepMeters: Float = 8f
internal const val detectorSmoothingRadiusMeters: Float = 10f
internal const val detectorHeadingWindowMeters: Float = 12f
internal const val detectorSupportWindowMeters: Float = 24f
internal const val detectorCandidateTurnAngleDeg: Float = 5.5f
internal const val detectorCandidateLocalTurnAngleDeg: Float = 0.45f
internal const val detectorBoundaryTurnAngleDeg: Float = 2.2f
internal const val detectorBoundaryLocalTurnAngleDeg: Float = 0.28f
internal const val detectorGroupingGapMeters: Float = 10f
internal const val detectorMergeGapMeters: Float = 14f
internal const val detectorTinyMergeGapMeters: Float = 5f
internal const val detectorMergeContinuationAngleDeg: Float = 1.2f
internal const val detectorSplitCompositeMinimumArcLengthMeters: Float = 90f
internal const val detectorSplitPeakMinimumSupportAngleDeg: Float = 20f
internal const val detectorSplitPeakMinimumSeparationMeters: Float = 40f
internal const val detectorSplitPeakMinimumLocalProminenceDeg: Float = 0.5f
internal const val detectorCornerMinimumArcLengthMeters: Float = 20f
internal const val detectorCornerMinimumHeadingDeltaDeg: Float = 18f
internal const val detectorCornerMinimumTurnDensityDegPerMeter: Float = 0.08f
internal const val detectorCornerStrongHeadingDeltaDeg: Float = 32f
internal const val detectorCornerMinimumPeakCurvature: Float = 0.002f
internal val degreesPerRadian: Float = 180f / PI.toFloat()
