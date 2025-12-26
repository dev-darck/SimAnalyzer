package com.project.analyzer.telemetry.ac.api.model.car.wheels

public data class WheelsFrame(
    val fl: WheelFrame? = null,
    val fr: WheelFrame? = null, // Front Right
    val rl: WheelFrame? = null, // Rear Left
    val rr: WheelFrame? = null, // Rear Right

    // Tyre compound
    val tyreCompound: String? = null, // e.g., "dry_compound", "wet_compound"
    val currentTyreSet: Int? = null,
    val strategyTyreSet: Int? = null,
    val isRainTyres: Boolean? = null,

    // MFD pressures for pit stop
    val mfdPressureLF: Float? = null,
    val mfdPressureRF: Float? = null,
    val mfdPressureLR: Float? = null,
    val mfdPressureRR: Float? = null,
    val mfdTyreSet: Int? = null,

    // Brake compounds
    val frontBrakeCompound: Int? = null,
    val rearBrakeCompound: Int? = null,
)
