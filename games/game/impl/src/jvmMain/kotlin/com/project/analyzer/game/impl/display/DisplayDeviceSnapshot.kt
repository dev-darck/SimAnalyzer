package com.project.analyzer.game.impl.display

import java.awt.Rectangle

internal data class DisplayDeviceSnapshot(
    val id: String,
    val userBounds: Rectangle,
    val physicalBounds: Rectangle,
    val scaleX: Double,
    val scaleY: Double,
    val isDefault: Boolean,
)
