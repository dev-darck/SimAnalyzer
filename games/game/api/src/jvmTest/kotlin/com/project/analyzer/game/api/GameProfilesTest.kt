package com.project.analyzer.game.api

import com.sun.jna.Pointer
import com.sun.jna.platform.win32.WinDef.HWND
import java.awt.Rectangle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GameProfilesTest {

    @Test
    fun `detectorConfigs keeps title fallback for ACE`() {
        val configs = GameProfiles.detectorConfigs()

        assertTrue(configs.any { "AssettoCorsaEVO.exe" in it.processNames })
        assertTrue(configs.any { "acevo" in it.windowClassNames })
        assertTrue(configs.any { "Evo" in it.titlePatterns })
    }

    @Test
    fun `match resolves ACE by class when process name is unavailable`() {
        val info = GameWindowInfo(
            hwnd = HWND(Pointer.NULL),
            title = "",
            processName = "",
            className = "acevo",
            bounds = Rectangle(0, 0, 2560, 1440),
            isFullscreen = true,
            monitor = GraphicsDeviceInfo("display-0", Rectangle(0, 0, 2560, 1440), true),
        )

        assertEquals(GameId.ACE, GameProfiles.match(info)?.id)
    }
}
