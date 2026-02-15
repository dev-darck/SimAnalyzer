package com.project.analyzer.impl.setup.game

import com.project.analyzer.game.impl.GameWindowInfo

import java.awt.Rectangle

data class OverlayState(
    val isVisible: Boolean = false,
    val bounds: Rectangle? = null,
    val gameInfo: GameWindowInfo? = null,
    val isDragging: Boolean = false
)
