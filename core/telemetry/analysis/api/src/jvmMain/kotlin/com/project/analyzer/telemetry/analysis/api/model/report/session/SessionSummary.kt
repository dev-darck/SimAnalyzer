package com.project.analyzer.telemetry.analysis.api.model.report.session

import com.project.analyzer.telemetry.analysis.api.model.report.common.Grade

public data class SessionSummary(
    val totalLaps: Int = 0,
    val validLaps: Int = 0,
    val bestLapTime: Long = 0L,
    val avgLapTime: Long = 0L,
    val consistency: Int = 0,
    val totalTimeLost: Long = 0L,
    val potentialBestLap: Long = 0L,
    val topStrengths: List<String> = emptyList(),
    val topWeaknesses: List<String> = emptyList(),
    val top3ImprovementAreas: List<ImprovementArea> = emptyList(),
    val estimatedLapTimePotential: Long = 0L,
    val drivingStyle: DrivingStyle = DrivingStyle.BALANCED,
    val tyreManagementGrade: Grade = Grade.C,
    val consistencyGrade: Grade = Grade.C,
    val overallGrade: Grade = Grade.C,
    val sessionNarrative: String = "",
)
