package com.analyzer.session.analysis.presentation.components.layout

import com.analyzer.session.analysis.presentation.components.layout.state.shouldRetainFocusHoverOnExit
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SessionAnalysisStudioInteractionStateHoverPolicyTest {

    @Test
    fun `retains hover on exit while focus mode is enabled`() {
        assertTrue(
            shouldRetainFocusHoverOnExit(
                focusMode = true,
                nextFraction = null,
                nextFrameId = null,
            ),
        )
    }

    @Test
    fun `does not retain hover on exit while focus mode is disabled`() {
        assertFalse(
            shouldRetainFocusHoverOnExit(
                focusMode = false,
                nextFraction = null,
                nextFrameId = null,
            ),
        )
    }

    @Test
    fun `does not retain hover when next hover target is available`() {
        assertFalse(
            shouldRetainFocusHoverOnExit(
                focusMode = true,
                nextFraction = 0.42f,
                nextFrameId = null,
            ),
        )
        assertFalse(
            shouldRetainFocusHoverOnExit(
                focusMode = true,
                nextFraction = null,
                nextFrameId = 42L,
            ),
        )
    }
}
