package com.project.analyzer.ac.telemetry.impl.fallback.logfile.model

data class EvoFileInfo(
    val trackName: String? = null,
    val trackId: String? = null,
    val carModel: String? = null,
    val sessionEpoch: Long = 0L,
    val driverName: String? = null,
    val driverSteamId: String? = null,
    val hasPenalty: Boolean = false,
    val penaltyReason: String? = null,
    val penaltyTimestamp: String? = null
)
