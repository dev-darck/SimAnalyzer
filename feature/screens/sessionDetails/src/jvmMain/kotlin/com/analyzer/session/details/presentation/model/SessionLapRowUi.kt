package com.analyzer.session.details.presentation.model

data class SessionLapRowUi(
    val lapNumber: Int,
    val lapLabel: String,
    val sessionTypeLabel: String,
    val totalTimeMs: Int?,
    val totalTime: String,
    val s1: String,
    val s2: String,
    val s3: String,
    val incidents: String,
    val delta: String,
    val deltaIsPositive: Boolean,
    val status: LapStatus,
)
