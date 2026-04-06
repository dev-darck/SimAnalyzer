package com.project.analyzer.telemetry.analysis.impl.domain.extension

import com.project.analyzer.telemetry.analysis.api.model.sample.SessionAnalysisSample

/**
 * Normalizes sample flags into explicit checks for invalid laps, pit phases, and caution periods.
 */
internal fun SessionAnalysisSample.hasFlag(flag: Int): Boolean = (flags and flag) != 0
