package com.project.analyzer.game.api

import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.Rectangle

public data class GameWindowInfo(
    val hwnd: HWND,
    val title: String,
    val processName: String,
    val className: String,
    val bounds: Rectangle,
    val isFullscreen: Boolean,
    val monitor: GraphicsDeviceInfo,
)
