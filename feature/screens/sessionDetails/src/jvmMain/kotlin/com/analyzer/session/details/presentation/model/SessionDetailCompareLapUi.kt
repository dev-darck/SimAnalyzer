package com.analyzer.session.details.presentation.model

data class SessionDetailCompareLapUi(
    val segmentId: Long,
    val lapNumber: Int,
    val lapLabel: String,
    val sessionTypeLabel: String,
    val totalTimeMs: Int?,
)
