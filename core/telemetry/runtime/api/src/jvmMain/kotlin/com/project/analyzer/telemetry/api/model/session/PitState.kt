package com.project.analyzer.telemetry.api.model.session

public data class PitState(
    val isInPit: Boolean? = null,
    val isInPitLane: Boolean? = null,
    val pitLimiterOn: Boolean? = null,

    val mandatoryPitDone: Boolean? = null,
    val missingMandatoryPits: Int? = null,

    // Pit window (for endurance races)
    val pitWindowStart: Int? = null,
    val pitWindowEnd: Int? = null,
    val serviceDamage: Int? = null,
    val serviceFuel: Int? = null,
    val serviceTyreFl: Int? = null,
    val serviceTyreFr: Int? = null,
    val serviceTyreRl: Int? = null,
    val serviceTyreRr: Int? = null,
)
