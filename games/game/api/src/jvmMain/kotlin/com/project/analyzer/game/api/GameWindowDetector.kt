package com.project.analyzer.game.api

import com.sun.jna.platform.win32.WinDef.HWND
import kotlinx.coroutines.flow.Flow

public interface GameWindowDetector {
    public fun setOverlayHwnd(hwnd: HWND?)
    public fun observeGameWindow(): Flow<GameWindowInfo?>
}
