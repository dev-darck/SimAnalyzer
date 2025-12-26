package com.project.analyzer.telemetry.ac.api.model.session

public data class DriverInfo(
    val firstName: String? = null,
    val lastName: String? = null,
    val nickname: String? = null,

    // Stint info (for endurance)
    val stintTotalTimeLeftMs: Int? = null,
    val stintTimeLeftMs: Int? = null,
)
