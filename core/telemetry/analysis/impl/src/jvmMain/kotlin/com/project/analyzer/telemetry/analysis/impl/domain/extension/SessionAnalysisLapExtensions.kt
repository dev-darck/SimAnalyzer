package com.project.analyzer.telemetry.analysis.impl.domain.extension

import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap

/**
 * Lap helpers centralize validity and timing checks so every resolver uses the same lap semantics.
 */
internal fun List<SessionAnalysisLap>.bestLapBySegmentId(): Map<Long, Int?> = filter { lap ->
    lap.isValid && lap.isComplete && !lap.isPitLap
}.groupBy(SessionAnalysisLap::segmentId)
    .mapValues { (_, segmentLaps) ->
        segmentLaps.minByOrNull { lap -> lap.durationMs ?: Int.MAX_VALUE }?.lapNumber
    }
