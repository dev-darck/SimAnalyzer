package com.project.analyzer.impl.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayCompatibilityGuardTest {

    @Test
    fun `is enabled by default`() {
        assertTrue(OverlayCompatibilityGuard.isEnabled(propertyValue = null, envValue = null))
    }

    @Test
    fun `property flag disables guard`() {
        assertFalse(OverlayCompatibilityGuard.isEnabled(propertyValue = "false", envValue = null))
    }

    @Test
    fun `detects nvidia share executable`() {
        assertEquals(
            "NVIDIA in-game overlay",
            OverlayCompatibilityGuard.resolveKnownConflict(
                command = "C:\\Program Files\\NVIDIA Corporation\\NVIDIA Share.exe",
                commandLine = null,
            ),
        )
    }

    @Test
    fun `ignores unrelated processes`() {
        assertNull(
            OverlayCompatibilityGuard.resolveKnownConflict(
                command = "C:\\Program Files\\Discord\\Discord.exe",
                commandLine = "\"C:\\Program Files\\Discord\\Discord.exe\" --start-minimized",
            ),
        )
    }
}
