package com.project.analyzer.telemetry.ac.api.model.car.damage

public data class AeroDamageFrame(
    val frontWing: Float? = null, // 0..1
    val rearWing: Float? = null, // 0..1
    val diffuser: Float? = null, // 0..1
)
