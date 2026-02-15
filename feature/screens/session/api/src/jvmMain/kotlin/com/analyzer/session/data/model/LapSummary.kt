package com.analyzer.session.data.model

public data class LapSummary(
    val lap: Int,
    val totalTimeMs: Int?,
    val sectorTimesMs: List<Int?>,
    val invalid: Boolean,
    val inPit: Boolean,
    val complete: Boolean,
)
