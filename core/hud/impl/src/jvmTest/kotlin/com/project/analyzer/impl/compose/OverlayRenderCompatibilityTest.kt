package com.project.analyzer.impl.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OverlayRenderCompatibilityTest {

    @Test
    fun `uses opengl on windows when nvidia overlay conflict is detected`() {
        assertEquals(
            "OPENGL",
            resolveOverlayRenderApiCompatibility(
                osName = "Windows 11",
                renderApiProperty = null,
                renderApiEnv = null,
                conflict = "NVIDIA in-game overlay",
            ),
        )
    }

    @Test
    fun `does nothing when render api is already configured`() {
        assertNull(
            resolveOverlayRenderApiCompatibility(
                osName = "Windows 11",
                renderApiProperty = "SOFTWARE",
                renderApiEnv = null,
                conflict = "NVIDIA in-game overlay",
            ),
        )
    }

    @Test
    fun `does nothing without overlay conflict`() {
        assertNull(
            resolveOverlayRenderApiCompatibility(
                osName = "Windows 11",
                renderApiProperty = null,
                renderApiEnv = null,
                conflict = null,
            ),
        )
    }
}
