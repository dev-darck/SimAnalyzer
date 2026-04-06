package com.analyzer.session.analysis.presentation.components.graph

import com.analyzer.session.analysis.presentation.components.graph.support.formatDeltaStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class SessionAnalysisTelemetryStripFormattingTest {

    @Test
    fun `formatDeltaStatus describes when selected lap is behind reference`() {
        assertEquals("Behind 0.247s", formatDeltaStatus(247))
    }

    @Test
    fun `formatDeltaStatus describes when selected lap is ahead of reference`() {
        assertEquals("Ahead 0.181s", formatDeltaStatus(-181))
    }

    @Test
    fun `formatDeltaStatus handles zero and null`() {
        assertEquals("On pace", formatDeltaStatus(0))
        assertEquals("--", formatDeltaStatus(null))
    }
}
