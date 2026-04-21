package com.project.analyzer.telemetry.api.model.car.wheels

import com.project.analyzer.math.Vec3

public data class WheelFrame(
    // Tyre state
    val pressurePsi: Float? = null,
    val wear: Float? = null, // 0..1
    val dirtyLevel: Float? = null, // 0..1

    // Temperatures
    val coreTempC: Float? = null,
    val innerTempC: Float? = null,
    val middleTempC: Float? = null,
    val outerTempC: Float? = null,
    val avgTempC: Float? = null, // Average tyre temp

    // Brake
    val brakeTempC: Float? = null, // Disc temperature
    val brakePressure: Float? = null,
    val padLife: Float? = null, // 0..1 or mm
    val discLife: Float? = null, // 0..1 or mm

    // Dynamics
    val slip: Float? = null, // Wheel slip
    val slipRatio: Float? = null,
    val slipAngle: Float? = null, // radians
    val load: Float? = null, // N (not used in ACC)
    val angularSpeed: Float? = null, // rad/s

    // Forces
    val longitudinalForce: Float? = null, // Fx - acceleration/braking force
    val lateralForce: Float? = null, // Fy - cornering force
    val selfAligningTorque: Float? = null, // Mz

    // Suspension
    val suspensionTravel: Float? = null, // meters
    val camberRad: Float? = null, // radians

    // Contact
    val contactPoint: Vec3? = null, // World position of a contact patch
    val contactNormal: Vec3? = null, // Surface normal at contact
    val contactHeading: Vec3? = null, // Surface heading at contact

    // Tyre radius
    val tyreRadius: Float? = null, // meters
    val normalizedPressure: Float? = null,
    val normalizedTempLeft: Float? = null,
    val normalizedTempMiddle: Float? = null,
    val normalizedTempRight: Float? = null,
    val normalizedBrakeTemp: Float? = null,
    val normalizedCoreTemp: Float? = null,
    val compoundFront: String? = null,
    val compoundRear: String? = null,
)
