package com.analyzer.session.data.model

public data class RecordedSessionDetail(
    val summary: RecordedSessionSummary,
    val laps: List<LapSummary>,
)
