package com.project.analyzer.game.api

import java.awt.Rectangle

public data class GraphicsDeviceInfo(
    val id: String,
    val bounds: Rectangle,
    val isDefault: Boolean,
)
