package com.project.analyzer.app

import com.analyzer.settings.api.AppCloseBehavior
import kotlin.test.Test
import kotlin.test.assertEquals

class AppCloseBehaviorActionTest {

    @Test
    fun `asks when tray is supported and behavior is ask every time`() {
        assertEquals(
            AppCloseAction.Ask,
            resolveAppCloseAction(
                behavior = AppCloseBehavior.AskEveryTime,
                isSystemTraySupported = true,
            ),
        )
    }

    @Test
    fun `minimizes when tray is supported and behavior is minimize`() {
        assertEquals(
            AppCloseAction.MinimizeToTray,
            resolveAppCloseAction(
                behavior = AppCloseBehavior.MinimizeToTray,
                isSystemTraySupported = true,
            ),
        )
    }

    @Test
    fun `falls back to exit when tray is not supported`() {
        assertEquals(
            AppCloseAction.Exit,
            resolveAppCloseAction(
                behavior = AppCloseBehavior.MinimizeToTray,
                isSystemTraySupported = false,
            ),
        )
    }
}
