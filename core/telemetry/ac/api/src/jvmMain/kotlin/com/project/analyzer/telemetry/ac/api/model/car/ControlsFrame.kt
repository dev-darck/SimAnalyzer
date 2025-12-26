package com.project.analyzer.telemetry.ac.api.model.car

public data class ControlsFrame(
    val throttle: Float? = null, // 0..1
    val brake: Float? = null, // 0..1
    val clutch: Float? = null, // 0..1
    val steerAngle: Float? = null, // radians
    val brakeBias: Float = 0f,
    val brakePressureFL: Float? = null,
    val brakePressureFR: Float? = null,
    val brakePressureRL: Float? = null,
    val brakePressureRR: Float? = null
)
