package com.project.analyzer.telemetry.api.model.car

public data class AssistsFrame(
    // TC (Traction Control)
    val tcLevel: Int? = null, // TC setting level
    val tcCut: Int? = null, // TC cut level
    val tcValue: Float? = null, // 0..1 current TC intervention
    val tcInAction: Boolean? = null, // TC currently intervening
    val tcActive: Boolean? = null,

    // ABS
    val absLevel: Int? = null, // ABS setting level
    val absValue: Float? = null, // 0..1 current ABS intervention
    val absInAction: Boolean? = null, // ABS currently intervening
    val absActive: Boolean? = null,
    val escActive: Boolean? = null,
    val launchActive: Boolean? = null,

    // Engine Map
    val engineMap: Int? = null,
    val ebbLevel: Int? = null,
    val turboLevel: Float? = null,
    val ersDeploymentMap: Int? = null,
    val ersRechargeMap: Float? = null,
    val diffPowerLevel: Int? = null,
    val diffCoastLevel: Int? = null,
    val diffPowerValue: Float? = null,
    val diffCoastValue: Float? = null,
    val frontBumpDamperLevel: Int? = null,
    val frontReboundDamperLevel: Int? = null,
    val rearBumpDamperLevel: Int? = null,
    val rearReboundDamperLevel: Int? = null,
    val activePerformanceMode: Int? = null,
    val p2pActivations: Int? = null,
    val p2pStatus: Int? = null,

    // DRS (not used in ACC)
    val drsAvailable: Boolean? = null,
    val drsEnabled: Boolean? = null,

    // Pit limiter
    val pitLimiterOn: Boolean? = null,
    val autoPitLimiter: Boolean? = null,

    // Ideal line
    val idealLineOn: Boolean? = null,
    val autoGear: Boolean? = null,
    val autoBlip: Boolean? = null,
    val autoClutch: Boolean? = null,
    val autoClutchOnStart: Boolean? = null,
    val manualIgnitionStarter: Boolean? = null,
    val standingStartAssist: Boolean? = null,
    val autoSteer: Float? = null,
    val stabilityControl: Float? = null,
    val ersHeatChargingOn: Boolean? = null,
    val ersOvertakeModeOn: Boolean? = null,

    // Wipers
    val wiperLevel: Int? = null,
)
