package com.project.analyzer.telemetry.ac.api.model.car.damage

public data class TyreDamageFrame(
    val fl: Float? = null,
    val fr: Float? = null,
    val rl: Float? = null,
    val rr: Float? = null,

    val punctureFl: Boolean? = null,
    val punctureFr: Boolean? = null,
    val punctureRl: Boolean? = null,
    val punctureRr: Boolean? = null,
)
