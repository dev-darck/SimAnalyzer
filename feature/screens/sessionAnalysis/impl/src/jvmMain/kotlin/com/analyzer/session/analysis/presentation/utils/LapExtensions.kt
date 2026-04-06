package com.analyzer.session.analysis.presentation.utils

import com.project.analyzer.telemetry.analysis.api.model.lap.SessionAnalysisLap

internal fun List<SessionAnalysisLap>.fastestLap(): SessionAnalysisLap? = asSequence()
    .filter { lap -> lap.isValid && lap.isComplete && !lap.isPitLap }
    .minByOrNull { lap -> lap.durationMs ?: Int.MAX_VALUE }
