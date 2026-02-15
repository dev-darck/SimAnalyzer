package com.project.analyzer.game.impl

import java.awt.Rectangle

data class GraphicsDeviceInfo(
    val id: String,
    val bounds: Rectangle,
    val isDefault: Boolean
)
