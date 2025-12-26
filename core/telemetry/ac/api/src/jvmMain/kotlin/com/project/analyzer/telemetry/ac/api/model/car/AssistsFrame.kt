package com.project.analyzer.telemetry.ac.api.model.car

public data class AssistsFrame(
    // TC (Traction Control)
    val tcLevel: Int? = null, // TC setting level
    val tcCut: Int? = null, // TC cut level
    val tcValue: Float? = null, // 0..1 current TC intervention
    val tcInAction: Boolean? = null, // TC currently intervening

    // ABS
    val absLevel: Int? = null, // ABS setting level
    val absValue: Float? = null, // 0..1 current ABS intervention
    val absInAction: Boolean? = null, // ABS currently intervening

    // Engine Map
    val engineMap: Int? = null,

    // DRS (not used in ACC)
    val drsAvailable: Boolean? = null,
    val drsEnabled: Boolean? = null,

    // Pit limiter
    val pitLimiterOn: Boolean? = null,

    // Ideal line
    val idealLineOn: Boolean? = null,

    // Wipers
    val wiperLevel: Int? = null,
)
