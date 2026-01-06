package com.project.analyzer.telemetry.ac.api.model.car

import com.project.analyzer.math.Vec3

public data class CarFrame(
    val controls: ControlsFrame? = null,
    val engine: EngineFrame? = null,
    val fuel: FuelFrame? = null,
    val assists: AssistsFrame? = null,

    // Kinematics / Pose
    val speedKmh: Float? = null,
    val velocity: Vec3? = null, // World velocity m/s
    val localVelocity: Vec3? = null, // Local velocity m/s (car-relative)
    val accelerationG: Vec3? = null, // G-force
    val worldPosition: Vec3? = null,

    val heading: Float? = null, // radians
    val pitch: Float? = null, // radians
    val roll: Float? = null, // radians

    val localAngularVelocity: Vec3? = null, // rad/s (yaw, pitch, roll rates)

    val cgHeight: Float? = null, // Center of gravity height

    // Ride
    val rideHeightFront: Float? = null, // meters
    val rideHeightRear: Float? = null, // meters

    // Force feedback
    val finalFF: Float? = null, // Final force feedback signal

    // Vibrations (for haptic feedback)
    val kerbVibration: Float? = null,
    val slipVibrations: Float? = null,
    val gVibrations: Float? = null,
    val absVibrations: Float? = null,

    // Lights
    val lightsStage: Int? = null,
    val rainLightsOn: Boolean? = null,
    val flashingLightsOn: Boolean? = null,
    val directionLightsLeft: Boolean? = null,
    val directionLightsRight: Boolean? = null,
)
