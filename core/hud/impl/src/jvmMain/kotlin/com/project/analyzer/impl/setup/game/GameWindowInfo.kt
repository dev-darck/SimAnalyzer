package com.project.analyzer.impl.setup.game

import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.Rectangle

data class GameWindowInfo(
    val hwnd: HWND,
    val title: String,
    val processName: String,
    val bounds: Rectangle,
    val isFullscreen: Boolean,
    val monitor: GraphicsDeviceInfo
)

data class GraphicsDeviceInfo(
    val id: String,
    val bounds: Rectangle,
    val isDefault: Boolean
)

data class GameConfig(
    val titlePatterns: List<String> = emptyList(),
    val processNames: List<String> = emptyList(),  // e.g., "acs.exe", "AssettoCorsa.exe"
    val windowClassNames: List<String> = emptyList()  // e.g., "UnityWndClass"
)
