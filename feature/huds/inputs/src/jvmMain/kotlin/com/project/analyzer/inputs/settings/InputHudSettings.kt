package com.project.analyzer.inputs.settings

import com.project.analyzer.annotations.DataStoreSerializer
import kotlinx.serialization.Serializable

@Serializable
@DataStoreSerializer
data class InputHudSettings(
    val showThrottle: Boolean = true,
    val showBrake: Boolean = true,
    val showClutch: Boolean = true,
    val showSteer: Boolean = true,

    val showHeader: Boolean = true,
    val showLegend: Boolean = true,

    val widthDp: Int = 500,
    val graphHeightDp: Int = 120,

    val historySeconds: Int = 3,
)
