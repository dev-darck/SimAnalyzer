package com.project.analyzer.telemetry.analysis.api.model.report.context

public data class TypicalSpeeds(
    val highSpeedCornerMin: Float = 0f,
    val mediumSpeedCornerMin: Float = 0f,
    val lowSpeedCornerMax: Float = 0f,
    val straightBaselineKmh: Float = 0f,
)
