package com.analyzer.session.data.model

public data class LapSummary(
    val lap: Int,
    val sessionType: String? = null,
    val totalTimeMs: Int?,
    val sectorTimesMs: List<Int?>,
    val invalid: Boolean,
    val inPit: Boolean,
    val complete: Boolean,
    val segmentId: Long = 0L,
)
