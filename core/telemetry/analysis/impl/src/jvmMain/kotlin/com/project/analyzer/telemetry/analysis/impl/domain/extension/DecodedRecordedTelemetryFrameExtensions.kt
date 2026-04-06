package com.project.analyzer.telemetry.analysis.impl.domain.extension

import com.project.analyzer.telemetry.recording.api.session.DecodedRecordedTelemetryFrame

/**
 * Hides decoder differences behind a shared set of frame accessors used by the analysis mappers.
 */
internal fun DecodedRecordedTelemetryFrame.hasUsableLapNumber(): Boolean = lapNumber > 0
